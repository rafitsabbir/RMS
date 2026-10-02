package rms.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.Outcome;

/**
 * Assigning interviewers to a candidate (Phase 3), from the candidate profile; Super Admin and HR (SecurityConfig).
 * Both answer with a redirect back to the profile and a message.
 */
@Controller
@RequestMapping(value = "/")
public class AssignmentController {

	@Autowired
	AssignmentService assignmentservice;

	@Autowired
	CandidateService candidateservice;

	@RequestMapping(value = "/assigninterviewer", method = RequestMethod.POST)
	public ModelAndView assign(@RequestParam("candidateid") String candidateid,
			@RequestParam(value = "interviewerid", required = false) String interviewerid, HttpSession session,
			RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		requireCandidate(candidateid);
		return back(candidateid, assignmentservice.assign(candidateid, interviewerid, userid), redirect);
	}

	/** The interviewer's evaluation, if any, stays and still counts; they can no longer change it. */
	@RequestMapping(value = "/unassigninterviewer", method = RequestMethod.POST)
	public ModelAndView unassign(@RequestParam("candidateid") String candidateid,
			@RequestParam(value = "interviewerid", required = false) String interviewerid, HttpSession session,
			RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		requireCandidate(candidateid);
		return back(candidateid, assignmentservice.unassign(candidateid, interviewerid, userid), redirect);
	}

	private void requireCandidate(String candidateid) {
		if (candidateservice.findCandidateById(candidateid) == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	/** Back to the profile's Interviewers card; the ID may be any text (G22), so it is encoded. */
	private static ModelAndView back(String candidateid, Outcome outcome, RedirectAttributes redirect) {
		redirect.addFlashAttribute(outcome.done() ? "message" : "errorMessage", outcome.message());
		return new ModelAndView("redirect:/viewcandidate?candidateid="
				+ URLEncoder.encode(candidateid, StandardCharsets.UTF_8) + "#interviewers");
	}
}
