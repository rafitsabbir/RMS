package rms.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import rms.model.CandidateInfo;
import rms.model.CandidateSource;
import rms.model.DocumentInfo;
import rms.model.DocumentType;
import rms.model.JobInfo;
import rms.model.LanguageInfo;
import rms.model.PositionInfo;
import rms.service.ActivityService;
import rms.service.EnglishText;
import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.DocumentService;
import rms.service.JobService;
import rms.service.LanguageService;
import rms.service.PositionService;

/**
 * Candidate management (G1): add, list, edit and soft delete. RMS generates the candidate ID (C1, C2, ...).
 * Both the generated IDs and the soft delete are owner decisions of 2026-10-02. Phase 2 of the roles plan adds the
 * contact details, source, applied date, the optional job (which sets the position) and the profile page with
 * the documents (DocumentController).
 */
@Controller
@RequestMapping(value = "/")
public class CandidateController {
	private static final Logger log = LoggerFactory.getLogger(CandidateController.class);

	@Autowired
	CandidateService candidateservice;

	@Autowired
	ActivityService activityservice;

	@Autowired
	PositionService positionservice;

	@Autowired
	LanguageService languageservice;

	@Autowired
	JobService jobservice;

	@Autowired
	DocumentService documentservice;

	@Autowired
	AssignmentService assignmentservice;

	static final int MAX_EMAIL = 150;
	static final int MAX_PHONE = 30;
	/** Something@something.something, no spaces: a typing check, not a proof the address works. */
	private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
	private static final Pattern PHONE = Pattern.compile("[0-9+()./ -]{3,30}");

	/** Only the form's fields bind: not candidatestatus, the document counts or the joined names. */
	@InitBinder("candidateinfo")
	void bindFormFieldsOnly(WebDataBinder binder) {
		binder.setAllowedFields("candidateid", "firstname", "lastname", "positionkey", "languagekey", "email", "phone",
				"source", "applieddate", "jobkey");
	}

	@RequestMapping(value = "/createcandidate", method = RequestMethod.GET)
	public ModelAndView createCandidate() {
		return form(new CandidateInfo(), null, null);
	}

	/** One endpoint for add and edit; the edit form posts update=true and can't change the ID. */
	@RequestMapping(value = "/savecandidate", method = RequestMethod.POST)
	public ModelAndView save(
			@ModelAttribute("candidateinfo") CandidateInfo candidateinfo, BindingResult binding,
			@RequestParam(value = "update", defaultValue = "false") boolean update,
			HttpSession session) {

		// A new candidate's ID is generated on save; an ID posted with it is ignored
		candidateinfo.setCandidateid(update ? trim(candidateinfo.getCandidateid()) : null);
		candidateinfo.setFirstname(trim(candidateinfo.getFirstname()));
		candidateinfo.setLastname(trim(candidateinfo.getLastname()));
		candidateinfo.setEmail(trim(candidateinfo.getEmail()));
		candidateinfo.setPhone(trim(candidateinfo.getPhone()));
		candidateinfo.setSource(trim(candidateinfo.getSource()));

		CandidateInfo stored = null;
		if (update) {
			stored = candidateservice.findCandidateById(candidateinfo.getCandidateid());
			if (stored == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
		}

		String errorMessage = check(candidateinfo, stored, binding);
		if (errorMessage != null) {
			return form(candidateinfo, stored, errorMessage);
		}

		String userid = CurrentUser.userid(session);
		if (update) {
			candidateservice.updateCandidate(candidateinfo);
			activityservice.record(userid, "CANDIDATE_CHANGED", "CANDIDATE", candidateinfo.getCandidateid(), null);
		} else {
			String candidateid = candidateservice.addCandidate(candidateinfo);
			activityservice.record(userid, "CANDIDATE_ADDED", "CANDIDATE", candidateid, null);
		}

		return new ModelAndView("redirect:/viewcandidatelist");
	}

	@RequestMapping(value = "/viewcandidatelist", method = RequestMethod.GET)
	public ModelAndView viewCandidate() {
		ModelAndView mv = new ModelAndView("viewcandidate");
		mv.addObject("candidatelist", candidateservice.getAllCandidate());
		return mv;
	}

	/** A request parameter, not a path variable: IDs from before the generator may be any text (G22). */
	@RequestMapping(value = "/updatecandidate", method = RequestMethod.GET)
	public ModelAndView update(@RequestParam("candidateid") String candidateid) {
		CandidateInfo candidateinfo = candidateservice.findCandidateById(candidateid);
		if (candidateinfo == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return form(candidateinfo, candidateinfo, null);
	}

	/** Soft delete; the ID is a form field, not in the path, because older IDs may be any text. */
	@RequestMapping(value = "/deletecandidate", method = RequestMethod.POST)
	public ModelAndView delete(@RequestParam("candidateid") String candidateid, HttpSession session) {

		candidateservice.deleteCandidate(candidateid);
		activityservice.record(CurrentUser.userid(session), "CANDIDATE_DELETED", "CANDIDATE", candidateid, null);

		return new ModelAndView("redirect:/viewcandidatelist");
	}

	/**
	 * The profile: details, job and documents (Phase 2). Interviewers, evaluations and decisions come in Phase 3.
	 * doctype preselects the upload form's kind (the Replace links); toolarge is set by SecurityConfig when an
	 * upload was over the size limit.
	 */
	@RequestMapping(value = "/viewcandidate", method = RequestMethod.GET)
	public ModelAndView profile(@RequestParam("candidateid") String candidateid,
			@RequestParam(value = "doctype", required = false) String doctype,
			@RequestParam(value = "toolarge", required = false) String toolarge, HttpSession session) {
		CandidateInfo candidateinfo = candidateservice.findCandidateById(candidateid);
		if (candidateinfo == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		// Interviewers (Phase 3): only candidates they are assigned to
		boolean staff = CurrentUser.isStaff(session);
		if (!staff && !assignmentservice.isAssigned(candidateinfo.getCandidateid(), CurrentUser.userid(session))) {
			log.warn("User {} isn't assigned to candidate {}; profile refused", CurrentUser.userid(session),
					candidateinfo.getCandidateid());
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		ModelAndView mv = new ModelAndView("candidateprofile");
		mv.addObject("candidate", candidateinfo);
		if (staff) {
			// Interviewers and the decision are for staff; an interviewer sees details and documents
			mv.addObject("assignments", assignmentservice.getAssignments(candidateinfo.getCandidateid()));
			if (CurrentUser.canEdit(session)) {
				mv.addObject("assignable", assignmentservice.getAssignableInterviewers(candidateinfo.getCandidateid()));
			}
		}

		// Grouped by kind, in the kinds' order; a kind this version doesn't know isn't shown
		Map<DocumentType, List<DocumentInfo>> documents = new EnumMap<DocumentType, List<DocumentInfo>>(
				DocumentType.class);
		for (DocumentType type : DocumentType.values()) {
			documents.put(type, new ArrayList<DocumentInfo>());
		}
		for (DocumentInfo document : documentservice.getDocuments(candidateinfo.getCandidateid())) {
			DocumentType type = DocumentType.parse(document.getDoctype());
			if (type != null) {
				documents.get(type).add(document);
			}
		}
		mv.addObject("documents", documents);
		mv.addObject("doctypes", DocumentType.values());
		mv.addObject("storageready", documentservice.isStorageConfigured());
		DocumentType selected = DocumentType.parse(doctype);
		mv.addObject("selecteddoctype", selected == null ? DocumentType.CV.name() : selected.name());
		if (toolarge != null) {
			mv.addObject("documentError", "The file is larger than 5 MB.");
		}
		return mv;
	}

	/** The first problem with the entered values, or null. stored is the saved candidate when editing. */
	private String check(CandidateInfo candidateinfo, CandidateInfo stored, BindingResult binding) {
		if (candidateinfo.getFirstname().isEmpty()) {
			return "Please enter the first name.";
		}
		if (candidateinfo.getLastname().isEmpty()) {
			return "Please enter the last name.";
		}
		if (!EnglishText.isEnglish(candidateinfo.getFirstname()) || !EnglishText.isEnglish(candidateinfo.getLastname())) {
			return "Names can use English letters only. " + EnglishText.PROBLEM;
		}
		String email = candidateinfo.getEmail();
		if (!email.isEmpty() && (email.length() > MAX_EMAIL || !EMAIL.matcher(email).matches())) {
			return "Please enter a valid e-mail address, or leave it empty.";
		}
		String phone = candidateinfo.getPhone();
		if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) {
			return "Please enter a valid phone number (digits, spaces and + ( ) - . /), or leave it empty.";
		}
		String jobProblem = checkJob(candidateinfo, stored, binding);
		if (jobProblem != null) {
			return jobProblem;
		}
		// An active position, or the one the candidate already has (it may have been deleted since). With a job,
		// checkJob has set the job's position
		int positionkey = candidateinfo.getPositionkey();
		if (candidateinfo.getJobkey() <= 0 && (positionkey <= 0 || (positionservice.findPositionById(positionkey) == null
				&& (stored == null || stored.getPositionkey() != positionkey)))) {
			return "Please choose a position.";
		}
		int languagekey = candidateinfo.getLanguagekey();
		if (languagekey <= 0 || (languageservice.findLanguageById(languagekey) == null
				&& (stored == null || stored.getLanguagekey() != languagekey))) {
			return "Please choose a language.";
		}
		// A known source, or the one the candidate already has
		String source = candidateinfo.getSource();
		if (!source.isEmpty() && CandidateSource.parse(source) == null
				&& (stored == null || !source.equals(stored.getSource()))) {
			return "Please choose where the candidate came from, or leave it empty.";
		}
		if (binding.hasFieldErrors("applieddate")) {
			return "Please enter the applied date as a date, or leave it empty.";
		}
		LocalDate applied = candidateinfo.getApplieddate();
		if (applied != null && applied.isAfter(LocalDate.now())) {
			return "The applied date can't be in the future.";
		}
		return null;
	}

	/**
	 * A job is optional. When there is one it must be open, unless the candidate already has it (it may have been
	 * closed or deleted since), and it sets the candidate's position (decision M): the job's, or for a deleted
	 * job the position the candidate already has.
	 */
	private String checkJob(CandidateInfo candidateinfo, CandidateInfo stored, BindingResult binding) {
		if (binding.hasFieldErrors("jobkey")) {
			return "Please choose a job, or none.";
		}
		int jobkey = candidateinfo.getJobkey();
		if (jobkey <= 0) {
			candidateinfo.setJobkey(0);
			return null;
		}
		boolean kept = stored != null && stored.getJobkey() == jobkey;
		JobInfo job = jobservice.findJobById(jobkey);
		if (job == null) {
			if (!kept) {
				return "Please choose an open job, or none.";
			}
			candidateinfo.setPositionkey(stored.getPositionkey());
			return null;
		}
		if (!job.isOpen() && !kept) {
			return "That job is closed. Please choose an open job, or none.";
		}
		candidateinfo.setPositionkey(job.getPositionkey());
		return null;
	}

	/**
	 * The add/edit form, with the active positions and languages and the open jobs to choose from. When editing,
	 * a deleted position or language, or a closed or deleted job, the candidate still has is offered too, so
	 * saving doesn't silently change it.
	 */
	private ModelAndView form(CandidateInfo candidateinfo, CandidateInfo stored, String errorMessage) {
		ModelAndView mv = new ModelAndView("createcandidate");
		mv.addObject("candidateinfo", candidateinfo);
		mv.addObject("update", stored != null);

		List<PositionInfo> positionlist = new ArrayList<PositionInfo>(positionservice.getAllPosition());
		List<LanguageInfo> languagelist = new ArrayList<LanguageInfo>(languageservice.getAllLanguage());
		if (stored != null) {
			int positionkey = stored.getPositionkey();
			if (positionkey > 0 && positionlist.stream().noneMatch(p -> p.getPositionkey() == positionkey)) {
				PositionInfo deleted = new PositionInfo();
				deleted.setPositionkey(positionkey);
				deleted.setPositionname(deletedLabel(stored.getPositionname(), "Position", positionkey));
				positionlist.add(deleted);
			}
			int languagekey = stored.getLanguagekey();
			if (languagekey > 0 && languagelist.stream().noneMatch(l -> l.getLanguagekey() == languagekey)) {
				LanguageInfo deleted = new LanguageInfo();
				deleted.setLanguagekey(languagekey);
				deleted.setLanguagename(deletedLabel(stored.getLanguagename(), "Language", languagekey));
				languagelist.add(deleted);
			}
		}
		mv.addObject("positionlist", positionlist);
		mv.addObject("languagelist", languagelist);

		List<JobInfo> joblist = new ArrayList<JobInfo>(jobservice.getOpenJob());
		if (stored != null && stored.getJobkey() > 0) {
			int jobkey = stored.getJobkey();
			if (joblist.stream().noneMatch(j -> j.getJobkey() == jobkey)) {
				JobInfo kept = jobservice.findJobById(jobkey);
				if (kept == null) {
					// Deleted: shown as "Job n (deleted)" (JobInfo.getLabel)
					kept = new JobInfo();
					kept.setJobkey(jobkey);
					kept.setStatus(null);
				}
				joblist.add(kept);
			}
		}
		mv.addObject("joblist", joblist);
		mv.addObject("sourcelist", CandidateSource.values());

		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}

	private static String deletedLabel(String name, String kind, int key) {
		return (name == null ? kind + " " + key : name) + " (deleted)";
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
