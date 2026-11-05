package rms.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.CandidateDao;
import rms.dao.DashboardDao;
import rms.dao.JobDao;
import rms.model.CandidateInfo;
import rms.model.CandidateSource;
import rms.model.DecisionStatus;
import rms.model.HiringGroup;
import rms.model.HiringReport;
import rms.model.HiringStage;
import rms.model.JobInfo;
import rms.model.WorkloadRow;

/**
 * The hiring report: the active candidates counted per stage, per job and per source, how long Selected and Rejected took
 * from the applied date, and the evaluations still to do per interviewer. Computed on every visit from the same
 * candidate list as the Candidates page; nothing is stored.
 */
@Service
public class HiringReportServiceImpl implements HiringReportService {

	static final String NO_JOB = "No job";
	static final String NO_SOURCE = "Not recorded";
	/** Weeks in the workload table, this week first. */
	static final int WORKLOAD_WEEKS = 4;
	private static final DateTimeFormatter WEEK = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

	CandidateDao candidatedao;
	JobDao jobdao;
	DashboardDao dashboarddao;

	@Autowired
	public void setCandidateDao(CandidateDao candidatedao) {
		this.candidatedao = candidatedao;
	}

	@Autowired
	public void setJobDao(JobDao jobdao) {
		this.jobdao = jobdao;
	}

	@Autowired
	public void setDashboardDao(DashboardDao dashboarddao) {
		this.dashboarddao = dashboarddao;
	}

	@Override
	public HiringReport getReport() {
		HiringReport report = new HiringReport();
		// Jobs in their list order, then "No job"; a job that is no longer listed shows as deleted (JobInfo.getLabel)
		Map<Integer, HiringGroup> byjob = new LinkedHashMap<Integer, HiringGroup>();
		for (JobInfo job : jobdao.getAllJob()) {
			byjob.put(job.getJobkey(), new HiringGroup(job.getLabel()));
		}
		// Sources in their order, then texts this version doesn't know, then "Not recorded"
		Map<String, HiringGroup> bysource = new LinkedHashMap<String, HiringGroup>();
		for (CandidateSource source : CandidateSource.values()) {
			bysource.put(source.name(), new HiringGroup(source.getLabel()));
		}
		Map<HiringStage, HiringGroup> bystage = new LinkedHashMap<HiringStage, HiringGroup>();
		for (HiringStage stage : HiringStage.values()) {
			bystage.put(stage, new HiringGroup(stage.getLabel()));
		}
		HiringGroup nojob = new HiringGroup(NO_JOB);
		HiringGroup nosource = new HiringGroup(NO_SOURCE);

		for (CandidateInfo candidate : candidatedao.getAllCandidate()) {
			report.getOverall().add(candidate);
			bystage.get(candidate.getHiringstage()).add(candidate);
			if (candidate.getJobkey() > 0) {
				byjob.computeIfAbsent(candidate.getJobkey(), key -> {
					JobInfo deleted = new JobInfo();
					deleted.setJobkey(key);
					deleted.setStatus(null);
					return new HiringGroup(deleted.getLabel());
				}).add(candidate);
			} else {
				nojob.add(candidate);
			}
			String source = candidate.getSource();
			if (source == null || source.isBlank()) {
				nosource.add(candidate);
			} else {
				bysource.computeIfAbsent(source, HiringGroup::new).add(candidate);
			}
			DecisionStatus status = DecisionStatus.parse(candidate.getCandidatestatus());
			if (status == DecisionStatus.S) {
				report.getToselected().add(candidate);
			} else if (status == DecisionStatus.R) {
				report.getTorejected().add(candidate);
			}
		}
		report.setByjob(withCandidates(byjob.values(), nojob));
		report.setBysource(withCandidates(bysource.values(), nosource));
		report.setBystage(new ArrayList<HiringGroup>(bystage.values()));
		report.setPendingbyinterviewer(dashboarddao.countPendingEvaluationsByInterviewer());
		workload(report, today());
		return report;
	}

	/** The server's date; a method so tests can fix it. */
	LocalDate today() {
		return LocalDate.now();
	}

	/**
	 * Interviewer workload (F9): every active interviewer, with their interviews (scheduled or done, not cancelled)
	 * per week, Monday to Sunday, from this week on, and their evaluations still to do. An interviewer who is no
	 * longer active but still has evaluations to do is listed too.
	 */
	void workload(HiringReport report, LocalDate today) {
		LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		List<String> labels = new ArrayList<String>();
		for (int week = 0; week < WORKLOAD_WEEKS; week++) {
			LocalDate start = monday.plusWeeks(week);
			labels.add((week == 0 ? "This week, " : "") + start.format(WEEK));
		}
		Map<String, WorkloadRow> rows = new LinkedHashMap<String, WorkloadRow>();
		for (Map.Entry<String, String> interviewer : dashboarddao.getActiveInterviewers().entrySet()) {
			rows.put(interviewer.getKey(), new WorkloadRow(interviewer.getKey(), interviewer.getValue(), WORKLOAD_WEEKS));
		}
		for (HiringGroup pending : report.getPendingbyinterviewer()) {
			String id = idOf(pending.getLabel());
			rows.computeIfAbsent(id, key -> new WorkloadRow(key, pending.getLabel(), WORKLOAD_WEEKS))
					.setPending(pending.getTotal());
		}
		for (Map.Entry<String, Map<LocalDate, Integer>> byday : dashboarddao
				.countInterviewsByDay(monday, monday.plusWeeks(WORKLOAD_WEEKS)).entrySet()) {
			WorkloadRow row = rows.get(byday.getKey());
			if (row == null) {
				// Not an active interviewer any more: their interviews are on the schedule, not their workload
				continue;
			}
			for (Map.Entry<LocalDate, Integer> day : byday.getValue().entrySet()) {
				int week = (int) (ChronoUnit.DAYS.between(monday, day.getKey()) / 7);
				for (int i = 0; i < day.getValue(); i++) {
					row.addInterview(week);
				}
			}
		}
		report.setWeeklabels(labels);
		report.setWorkload(new ArrayList<WorkloadRow>(rows.values()));
	}

	/** The user ID in a "Name (U2)" label. */
	private static String idOf(String label) {
		int open = label.lastIndexOf('(');
		return open < 0 || !label.endsWith(")") ? label : label.substring(open + 1, label.length() - 1);
	}

	/** The rows that have candidates, and the last row if it has any: an empty job or source isn't listed. */
	private static List<HiringGroup> withCandidates(Iterable<HiringGroup> rows, HiringGroup last) {
		List<HiringGroup> list = new ArrayList<HiringGroup>();
		for (HiringGroup row : rows) {
			if (row.getTotal() > 0) {
				list.add(row);
			}
		}
		if (last.getTotal() > 0) {
			list.add(last);
		}
		return list;
	}
}
