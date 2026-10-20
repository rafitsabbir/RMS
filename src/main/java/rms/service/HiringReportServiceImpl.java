package rms.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import rms.model.JobInfo;

/**
 * The hiring report: the active candidates counted per job and per source, how long Selected and Rejected took
 * from the applied date, and the evaluations still to do per interviewer. Computed on every visit from the same
 * candidate list as the Candidates page; nothing is stored.
 */
@Service
public class HiringReportServiceImpl implements HiringReportService {

	static final String NO_JOB = "No job";
	static final String NO_SOURCE = "Not recorded";

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
		HiringGroup nojob = new HiringGroup(NO_JOB);
		HiringGroup nosource = new HiringGroup(NO_SOURCE);

		for (CandidateInfo candidate : candidatedao.getAllCandidate()) {
			report.getOverall().add(candidate);
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
		report.setPendingbyinterviewer(dashboarddao.countPendingEvaluationsByInterviewer());
		return report;
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
