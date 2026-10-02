package rms.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import rms.model.MarksInfo;
import rms.model.ResultInfo;
import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.DecisionService;
import rms.service.DocumentService;
import rms.service.MarksService;
import rms.service.Outcome;

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

	/** Only the scores and comments bind: the interviewer comes from the session, never from the post. */
	@InitBinder("marksinfo")
	void bindScoresAndCommentsOnly(WebDataBinder binder) {
		List<String> fields = new ArrayList<String>();
		fields.add("candidateid");
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
	 */
	@RequestMapping(value = "/evaluate", method = RequestMethod.GET)
	public ModelAndView evaluate(@RequestParam("candidateid") String candidateid, HttpSession session) {
		String userid = CurrentUser.requireUserid(session);
		CandidateInfo candidate = requireCandidate(candidateid);
		MarksInfo existing = marksservice.findEvaluation(candidateid, userid);
		boolean assigned = assignmentservice.isAssigned(candidateid, userid);
		if (!assigned && existing == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		MarksInfo marksinfo = existing;
		if (marksinfo == null) {
			marksinfo = new MarksInfo();
			marksinfo.setCandidateid(candidate.getCandidateid());
		}
		return form(candidate, marksinfo, assigned, null);
	}

	@RequestMapping(value = "/saveevaluation", method = RequestMethod.POST)
	public ModelAndView save(@ModelAttribute("marksinfo") MarksInfo marksinfo, BindingResult binding,
			HttpSession session, RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		CandidateInfo candidate = requireCandidate(marksinfo.getCandidateid());
		boolean assigned = assignmentservice.isAssigned(candidate.getCandidateid(), userid);
		MarksInfo stored = marksservice.findEvaluation(candidate.getCandidateid(), userid);
		if (!assigned) {
			if (stored == null) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN);
			}
			return form(candidate, stored, false, "You are no longer assigned to this candidate, so your "
					+ "evaluation can't be changed.");
		}
		if (candidate.isLocked()) {
			// Read-only now: show what is stored, not what was posted
			MarksInfo shown = stored;
			if (shown == null) {
				shown = new MarksInfo();
				shown.setCandidateid(candidate.getCandidateid());
			}
			return form(candidate, shown, true, "This candidate has been "
					+ candidate.getStatuslabel().toLowerCase(Locale.ROOT) + ", so the evaluations can't be changed.");
		}
		Outcome outcome = binding.hasErrors()
				? Outcome.refused("Please score every criterion from " + Criterion.MIN_SCORE + " to "
						+ Criterion.MAX_SCORE + ".")
				: marksservice.saveEvaluation(marksinfo, userid);
		if (!outcome.done()) {
			return form(candidate, marksinfo, true, outcome.message());
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

	private ModelAndView form(CandidateInfo candidate, MarksInfo marksinfo, boolean assigned, String errorMessage) {
		ModelAndView mv = new ModelAndView("evaluate");
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
