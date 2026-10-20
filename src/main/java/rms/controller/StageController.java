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

import rms.service.CandidateService;
import rms.service.Outcome;
import rms.service.StageService;

/**
 * Moving a candidate to another hiring stage, from the profile's Pipeline card; Super Admin and HR (SecurityConfig).
 * The rules are in StageServiceImpl. Answers with a redirect back to the card and a message.
 */
@Controller
@RequestMapping(value = "/")
public class StageController {

	@Autowired
	StageService stageservice;

	@Autowired
	CandidateService candidateservice;

	/** expected is the stage the form showed, so a move made meanwhile isn't silently undone. */
	@RequestMapping(value = "/savestage", method = RequestMethod.POST)
	public ModelAndView save(@RequestParam("candidateid") String candidateid,
			@RequestParam(value = "expected", required = false) String expected,
			@RequestParam(value = "stage", required = false) String stage,
			@RequestParam(value = "note", required = false) String note, HttpSession session,
			RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		if (candidateservice.findCandidateById(candidateid) == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		Outcome outcome = stageservice.moveStage(candidateid, expected, stage, note, userid);
		redirect.addFlashAttribute(outcome.done() ? "message" : "errorMessage", outcome.message());
		return new ModelAndView("redirect:/viewcandidate?candidateid="
				+ URLEncoder.encode(candidateid, StandardCharsets.UTF_8) + "#pipeline");
	}
}
