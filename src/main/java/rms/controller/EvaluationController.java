package rms.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rms.model.CandidateInfo;
import rms.model.Criterion;
import rms.model.DecisionInfo;
import rms.model.DecisionStatus;
import rms.model.InterviewRound;
import rms.model.MarksInfo;
import rms.model.ResultInfo;
import rms.model.RoundTab;
import rms.model.ScheduleInfo;
import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.DecisionService;
import rms.service.DocumentService;
import rms.service.MarksService;
import rms.service.Outcome;
import rms.service.ScheduleService;

/**
 * Evaluations and decisions (Phase 3; G5-G7, G9). Interviewers: My Evaluations and the evaluation form, for their
 * assigned candidates only. Staff: a candidate's evaluations, with the averages and the decision history; Super Admin
 * and HR record the decision there. Who may open which URL is in SecurityConfig; the per-candidate checks are here.
 */
@Controller
@RequestMapping(value = "/")
public class EvaluationController {

	@Autowired
	MarksService marksservice;

	@Autowired
	AssignmentService assignmentservice;

	@Autowired
	CandidateService candidateservice;

	@Autowired
	DecisionService decisionservice;

	@Autowired
	DocumentService documentservice;

	@Autowired
	ScheduleService scheduleservice;

	/** Only the scores and comments bind: the interviewer comes from the session, never from the post. */
	@InitBinder("marksinfo")
	void bindScoresAndCommentsOnly(WebDataBinder binder) {
		List<String> fields = new ArrayList<String>();
		fields.add("candidateid");
		fields.add("interviewround");
		fields.add("comments");
		for (Criterion criterion : Criterion.values()) {
			fields.add(criterion.getField());
			fields.add(criterion.getCommentfield());
		}
		binder.setAllowedFields(fields.toArray(new String[0]));
	}

	@InitBinder("decisioninfo")
	void bindDecisionFieldsOnly(WebDataBinder binder) {
		binder.setAllowedFields("candidateid", "status", "reason", "decisiondate");
	}

	/** The interviewer's candidates: assigned now, or evaluated and unassigned since (G7). */
	@RequestMapping(value = "/myevaluations", method = RequestMethod.GET)
	public ModelAndView myEvaluations(HttpSession session) {
		ModelAndView mv = new ModelAndView("myevaluations");
		mv.addObject("candidatelist", assignmentservice.getMyCandidates(CurrentUser.requireUserid(session)));
		return mv;
	}

	/**
	 * The evaluation form (G5), with the candidate's documents. Editable while the interviewer is assigned and the
	 * candidate isn't Selected or Rejected; otherwise their own evaluation read-only, or 403 when they have none.
	 * One evaluation per round (F2): round= picks it ("" for no round); without it, the round of the interviewer's
	 * latest interview with the candidate, else no round.
	 */
	@RequestMapping(value = "/evaluate", method = RequestMethod.GET)
	public ModelAndView evaluate(@RequestParam("candidateid") String candidateid,
			@RequestParam(value = "round", required = false) String round, HttpSession session) {
		String userid = CurrentUser.requireUserid(session);
		CandidateInfo candidate = requireCandidate(candidateid);
		String interviewround = round == null ? latestRound(candidate.getCandidateid(), userid)
				: InterviewRound.code(round);
		MarksInfo existing = marksservice.findEvaluation(candidateid, userid, interviewround);
		boolean assigned = assignmentservice.isAssigned(candidateid, userid);
		if (!assigned && existing == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		MarksInfo marksinfo = existing;
		if (marksinfo == null) {
			marksinfo = new MarksInfo();
			marksinfo.setCandidateid(candidate.getCandidateid());
			marksinfo.setInterviewround(interviewround);
		}
		return form(candidate, marksinfo, assigned, null, userid);
	}

	@RequestMapping(value = "/saveevaluation", method = RequestMethod.POST)
	public ModelAndView save(@ModelAttribute("marksinfo") MarksInfo marksinfo, BindingResult binding,
			HttpSession session, RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		CandidateInfo candidate = requireCandidate(marksinfo.getCandidateid());
		boolean assigned = assignmentservice.isAssigned(candidate.getCandidateid(), userid);
		marksinfo.setInterviewround(InterviewRound.code(marksinfo.getInterviewround()));
		MarksInfo stored = marksservice.findEvaluation(candidate.getCandidateid(), userid, marksinfo.getInterviewround());
		if (!assigned) {
			if (stored == null) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN);
			}
			return form(candidate, stored, false, "You are no longer assigned to this candidate, so your "
					+ "evaluation can't be changed.", userid);
		}
		if (candidate.isLocked()) {
			// Read-only now: show what is stored, not what was posted
			MarksInfo shown = stored;
			if (shown == null) {
				shown = new MarksInfo();
				shown.setCandidateid(candidate.getCandidateid());
				shown.setInterviewround(marksinfo.getInterviewround());
			}
			return form(candidate, shown, true, "This candidate has been "
					+ candidate.getStatuslabel().toLowerCase(Locale.ROOT) + ", so the evaluations can't be changed.",
					userid);
		}
		Outcome outcome = binding.hasErrors()
				? Outcome.refused("Please score every criterion from " + Criterion.MIN_SCORE + " to "
						+ Criterion.MAX_SCORE + ".")
				: marksservice.saveEvaluation(marksinfo, userid);
		if (!outcome.done()) {
			return form(candidate, marksinfo, true, outcome.message(), userid);
		}
		redirect.addFlashAttribute("message", outcome.message());
		return new ModelAndView("redirect:/myevaluations");
	}

	/** A candidate's evaluations, averages and decisions (staff); Super Admin and HR record the decision here. */
	@RequestMapping(value = "/viewevaluations", method = RequestMethod.GET)
	public ModelAndView viewEvaluations(@RequestParam("candidateid") String candidateid) {
		CandidateInfo candidate = requireCandidate(candidateid);
		DecisionInfo decisioninfo = new DecisionInfo();
		decisioninfo.setCandidateid(candidate.getCandidateid());
		decisioninfo.setStatus(DecisionStatus.parse(candidate.getCandidatestatus()) == null ? null
				: DecisionStatus.parse(candidate.getCandidatestatus()).name());
		decisioninfo.setDecisiondate(LocalDate.now());
		return evaluations(candidate, decisioninfo, null);
	}

	@RequestMapping(value = "/savedecision", method = RequestMethod.POST)
	public ModelAndView saveDecision(@ModelAttribute("decisioninfo") DecisionInfo decisioninfo, BindingResult binding,
			HttpSession session, RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		CandidateInfo candidate = requireCandidate(decisioninfo.getCandidateid());
		Outcome outcome = binding.hasFieldErrors("decisiondate")
				? Outcome.refused("Please enter the date of the decision as a date.")
				: decisionservice.saveDecision(decisioninfo, userid,
						marksservice.getEvaluations(candidate.getCandidateid()).size());
		if (!outcome.done()) {
			return evaluations(candidate, decisioninfo, outcome.message());
		}
		redirect.addFlashAttribute("message", outcome.message());
		return new ModelAndView("redirect:/viewevaluations?candidateid="
				+ URLEncoder.encode(candidate.getCandidateid(), StandardCharsets.UTF_8));
	}

	private CandidateInfo requireCandidate(String candidateid) {
		CandidateInfo candidate = candidateid == null ? null : candidateservice.findCandidateById(candidateid);
		if (candidate == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return candidate;
	}

	/** The round of the interviewer's latest interview with the candidate that wasn't cancelled, or null (F2). */
	private String latestRound(String candidateid, String userid) {
		String round = null;
		for (ScheduleInfo schedule : scheduleservice.getScheduleOf(userid)) {
			if (candidateid.equals(schedule.getCandidateid()) && !schedule.isCancelled()) {
				round = schedule.getInterviewround();
			}
		}
		return InterviewRound.code(round);
	}

	/**
	 * The round tabs: every round, plus "No round" while it is shown or the interviewer has an evaluation without a
	 * round (older ones). Unassigned, only the rounds they evaluated.
	 */
	private List<RoundTab> roundTabs(String candidateid, String userid, String current, boolean assigned) {
		Set<String> done = new HashSet<String>();
		for (MarksInfo evaluation : marksservice.getEvaluations(candidateid)) {
			if (userid.equals(evaluation.getInterviewerid())) {
				done.add(evaluation.getInterviewround() == null ? "" : evaluation.getInterviewround());
			}
		}
		String shown = current == null ? "" : current;
		List<RoundTab> tabs = new ArrayList<RoundTab>();
		if (shown.isEmpty() || done.contains("")) {
			tabs.add(new RoundTab("", InterviewRound.NO_ROUND, done.contains(""), shown.isEmpty()));
		}
		for (InterviewRound round : InterviewRound.values()) {
			if (assigned || done.contains(round.name()) || shown.equals(round.name())) {
				tabs.add(new RoundTab(round.name(), round.getLabel(), done.contains(round.name()),
						shown.equals(round.name())));
			}
		}
		return tabs;
	}

	private ModelAndView form(CandidateInfo candidate, MarksInfo marksinfo, boolean assigned, String errorMessage,
			String userid) {
		ModelAndView mv = new ModelAndView("evaluate");
		mv.addObject("roundtabs", roundTabs(candidate.getCandidateid(), userid, marksinfo.getInterviewround(), assigned));
		mv.addObject("candidate", candidate);
		mv.addObject("marksinfo", marksinfo);
		mv.addObject("criteria", Criterion.values());
		mv.addObject("editable", assigned && !candidate.isLocked());
		mv.addObject("assigned", assigned);
		// The documents only while assigned, like the profile and the downloads
		if (assigned) {
			mv.addObject("documents", documentservice.getDocuments(candidate.getCandidateid()));
			mv.addObject("storageready", documentservice.isStorageConfigured());
		}
		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}

	private ModelAndView evaluations(CandidateInfo candidate, DecisionInfo decisioninfo, String errorMessage) {
		ModelAndView mv = new ModelAndView("viewevaluations");
		List<MarksInfo> evaluations = marksservice.getEvaluations(candidate.getCandidateid());
		mv.addObject("candidate", candidate);
		mv.addObject("evaluations", evaluations);
		mv.addObject("summary", ResultInfo.summarize(evaluations));
		// Averages per round (F2), when the evaluations span more than one; "No round" for older ones
		Map<String, List<MarksInfo>> byround = new LinkedHashMap<String, List<MarksInfo>>();
		for (MarksInfo evaluation : evaluations) {
			byround.computeIfAbsent(evaluation.getRoundlabel(), k -> new ArrayList<MarksInfo>()).add(evaluation);
		}
		if (byround.size() > 1) {
			Map<String, ResultInfo> roundsummaries = new LinkedHashMap<String, ResultInfo>();
			for (InterviewRound round : InterviewRound.values()) {
				if (byround.containsKey(round.getLabel())) {
					roundsummaries.put(round.getLabel(), ResultInfo.summarize(byround.get(round.getLabel())));
				}
			}
			if (byround.containsKey(InterviewRound.NO_ROUND)) {
				roundsummaries.put(InterviewRound.NO_ROUND, ResultInfo.summarize(byround.get(InterviewRound.NO_ROUND)));
			}
			mv.addObject("roundsummaries", roundsummaries);
		}
		mv.addObject("criteria", Criterion.values());
		mv.addObject("decisions", decisionservice.getDecisions(candidate.getCandidateid()));
		mv.addObject("decisioninfo", decisioninfo);
		mv.addObject("statuses", DecisionStatus.values());
		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}
}
