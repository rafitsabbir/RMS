package rms.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import rms.model.ActivityInfo;
import rms.model.CandidateFilter;
import rms.model.CandidateInfo;
import rms.model.Criterion;
import rms.model.DecisionTime;
import rms.model.HiringGroup;
import rms.model.HiringReport;
import rms.model.ResultInfo;
import rms.model.ScheduleInfo;
import rms.service.ActivityService;
import rms.service.CandidateService;
import rms.service.CsvWriter;
import rms.service.HiringReportService;
import rms.service.MarksService;
import rms.service.ScheduleService;

/**
 * Reports (staff): CSV downloads that open in Excel. The candidate, Candidate Status and interview schedule exports
 * hold what those pages show; the activity log export is for Super Admins only (SecurityConfig). Every download is
 * logged with its keys, never its content.
 */
@Controller
@RequestMapping(value = "/")
public class ReportController {
	private static final Logger log = LoggerFactory.getLogger(ReportController.class);

	@Autowired
	CandidateService candidateservice;

	@Autowired
	MarksService marksservice;

	@Autowired
	ScheduleService scheduleservice;

	@Autowired
	ActivityService activityservice;

	@Autowired
	HiringReportService hiringreportservice;

	/** The CSV downloads and the hiring report (counts only, computed on every visit). */
	@RequestMapping(value = "/reports", method = RequestMethod.GET)
	public ModelAndView reports() {
		ModelAndView mv = new ModelAndView("reports");
		mv.addObject("hiring", hiringreportservice.getReport());
		return mv;
	}

	/** The hiring report as one CSV: the decision times, then the rows per job, per source and per interviewer. */
	@RequestMapping(value = "/exporthiring", method = RequestMethod.GET)
	public ResponseEntity<byte[]> hiring(HttpSession session) {
		HiringReport report = hiringreportservice.getReport();
		CsvWriter csv = new CsvWriter().row("Days from applying to the decision", "Candidates", "Average", "Median",
				"Shortest", "Longest");
		time(csv, "Selected", report.getToselected());
		time(csv, "Rejected", report.getTorejected());
		csv.row();
		groups(csv, "Job", report.getByjob());
		csv.row();
		groups(csv, "Source", report.getBysource());
		csv.row();
		csv.row("Interviewer", "Evaluations still to do");
		for (HiringGroup row : report.getPendingbyinterviewer()) {
			csv.row(row.getLabel(), row.getTotal());
		}
		return download(csv, "hiring-report", "HIRING", null, session);
	}

	private static void time(CsvWriter csv, String decision, DecisionTime time) {
		csv.row(decision, time.getCount(), time.getAverage(), time.getMedian(), time.getShortest(), time.getLongest());
	}

	private static void groups(CsvWriter csv, String kind, List<HiringGroup> rows) {
		csv.row(kind, "Candidates", "Pending", "On hold", "Selected", "Rejected", "No CV", "Not evaluated",
				"Average days to Selected");
		for (HiringGroup row : rows) {
			csv.row(row.getLabel(), row.getTotal(), row.getPending(), row.getOnhold(), row.getSelected(),
					row.getRejected(), row.getNocv(), row.getNotevaluated(), row.getAveragedaystoselected());
		}
	}

	/** The Candidates list's filters, so its "Download CSV" exports what the list shows. */
	@InitBinder("filter")
	void bindFilterFieldsOnly(WebDataBinder binder) {
		binder.setAllowedFields("jobkey", "status", "source", "appliedfrom", "appliedto", "nocv", "notevaluated");
	}

	/** Every active candidate, or with the list's filters only those that pass (a date that isn't one is ignored). */
	@RequestMapping(value = "/exportcandidates", method = RequestMethod.GET)
	public ResponseEntity<byte[]> candidates(@ModelAttribute("filter") CandidateFilter filter, BindingResult binding,
			HttpSession session) {
		CsvWriter csv = new CsvWriter().row("Candidate ID", "First name", "Last name", "Position", "Language", "E-mail",
				"Phone", "Source", "Applied date", "Status", "Decision date", "Decided by", "Reason for decision");
		for (CandidateInfo c : candidateservice.getCandidates(filter)) {
			csv.row(c.getCandidateid(), c.getFirstname(), c.getLastname(), c.getPositionname(), c.getLanguagename(),
					c.getEmail(), c.getPhone(), c.getSourcelabel(), c.getApplieddate(), c.getStatuslabel(),
					c.getDecisiondate(), c.getDecidedbyname(), c.getDecisionreason());
		}
		return download(csv, "candidates", "CANDIDATES", filter.isActive() ? "filtered" : null, session);
	}

	@RequestMapping(value = "/exportresults", method = RequestMethod.GET)
	public ResponseEntity<byte[]> results(HttpSession session) {
		Object[] header = new Object[Criterion.values().length + 8];
		int i = 0;
		header[i++] = "Candidate ID";
		header[i++] = "First name";
		header[i++] = "Last name";
		header[i++] = "Position";
		header[i++] = "Language";
		header[i++] = "Evaluations";
		for (Criterion criterion : Criterion.values()) {
			header[i++] = criterion.getLabel() + " (average)";
		}
		header[i++] = "Total (average)";
		header[i++] = "Status";
		CsvWriter csv = new CsvWriter().row(header);
		for (ResultInfo r : marksservice.getResults()) {
			Object[] row = new Object[header.length];
			int j = 0;
			row[j++] = r.getCandidateid();
			row[j++] = r.getFirstname();
			row[j++] = r.getLastname();
			row[j++] = r.getPositionname();
			row[j++] = r.getLanguagename();
			row[j++] = r.getEvaluations();
			for (int k = 0; k < Criterion.values().length; k++) {
				row[j++] = r.getEvaluations() == 0 || k >= r.getRoundedaverages().size() ? null
						: r.getRoundedaverages().get(k);
			}
			row[j++] = r.getTotal();
			row[j++] = r.getStatuslabel();
			csv.row(row);
		}
		return download(csv, "candidate-status", "CANDIDATE_STATUS", null, session);
	}

	@RequestMapping(value = "/exportschedule", method = RequestMethod.GET)
	public ResponseEntity<byte[]> schedule(HttpSession session) {
		CsvWriter csv = new CsvWriter().row("When (server time)", "Candidate ID", "Candidate", "Interviewer ID",
				"Interviewer", "Location", "Status");
		for (ScheduleInfo s : scheduleservice.getAllSchedule()) {
			csv.row(s.getStartlabel(), s.getCandidateid(), s.getCandidatename(), s.getInterviewerid(),
					s.getInterviewername(), s.getLocation(), s.getStatus());
		}
		return download(csv, "interview-schedule", "INTERVIEW_SCHEDULE", null, session);
	}

	@RequestMapping(value = "/exportactivity", method = RequestMethod.GET)
	public ResponseEntity<byte[]> activity(HttpSession session) {
		CsvWriter csv = new CsvWriter().row("When (server time)", "User ID", "User", "Action", "Record type",
				"Record ID", "Detail");
		for (ActivityInfo a : activityservice.getRecent(null)) {
			csv.row(a.getCreatedlabel(), a.getUserid(), a.getUsername(), a.getActionlabel(), a.getEntitytype(),
					a.getEntityid(), a.getDetail());
		}
		return download(csv, "activity-log", "ACTIVITY_LOG", null, session);
	}

	private ResponseEntity<byte[]> download(CsvWriter csv, String name, String report, String detail,
			HttpSession session) {
		String userid = CurrentUser.userid(session);
		log.info("Report {} downloaded by {}", report, userid);
		// The download itself is an activity too; the log export would list its own download from the next one on
		activityservice.record(userid, "REPORT_DOWNLOADED", "REPORT", report, detail);
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(name + "-" + LocalDate.now() + ".csv").build().toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(csv.toBytes());
	}
}
