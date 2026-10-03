package rms.controller;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rms.model.AssignmentInfo;
import rms.model.CandidateInfo;
import rms.model.ScheduleInfo;
import rms.service.CandidateService;
import rms.service.Outcome;
import rms.service.ScheduleService;

/**
 * Interview schedule (Phase 4). Super Admin and HR schedule, change and cancel (SecurityConfig); Super Admin, HR and
 * Hiring Manager read the whole list; an Interviewer reads only their own (/myschedule takes the user from the
 * session, never from the request). A new interview takes two steps without scripts: choose the candidate (GET),
 * then fill in the interview for them, with the interviewers assigned to them. The rules (assigned and active
 * interviewer, no double booking) are in ScheduleServiceImpl.
 */
@Controller
@RequestMapping(value = "/")
public class ScheduleController {

	@Autowired
	ScheduleService scheduleservice;

	@Autowired
	CandidateService candidateservice;

	/** Only the form's fields bind; the candidate of an existing interview comes from the stored row. */
	@InitBinder("scheduleinfo")
	void bindFormFieldsOnly(WebDataBinder binder) {
		binder.setAllowedFields("schedulekey", "candidateid", "interviewerid", "startat", "location", "status");
	}

	@RequestMapping(value = "/viewschedulelist", method = RequestMethod.GET)
	public ModelAndView viewSchedule() {
		ModelAndView mv = new ModelAndView("viewschedule");
		mv.addObject("schedulelist", scheduleservice.getAllSchedule());
		return mv;
	}

	/** The interviewer's own interviews: the user is the session's, so nobody can ask for another's. */
	@RequestMapping(value = "/myschedule", method = RequestMethod.GET)
	public ModelAndView mySchedule(HttpSession session) {
		ModelAndView mv = new ModelAndView("myschedule");
		mv.addObject("schedulelist", scheduleservice.getScheduleOf(CurrentUser.requireUserid(session)));
		return mv;
	}

	/** Without a candidate ID: the list of candidates to choose from. With one: the interview form for them. */
	@RequestMapping(value = "/createschedule", method = RequestMethod.GET)
	public ModelAndView createSchedule(@RequestParam(value = "candidateid", required = false) String candidateid) {
		if (candidateid == null || candidateid.isBlank()) {
			ModelAndView mv = new ModelAndView("createschedule");
			mv.addObject("candidatelist", candidateservice.getAllCandidate());
			return mv;
		}
		CandidateInfo candidate = requireCandidate(candidateid);
		ScheduleInfo scheduleinfo = new ScheduleInfo();
		scheduleinfo.setCandidateid(candidate.getCandidateid());
		return form(scheduleinfo, candidate, null);
	}

	@RequestMapping(value = "/updateschedule/{schedulekey}", method = RequestMethod.GET)
	public ModelAndView update(@PathVariable("schedulekey") int schedulekey, RedirectAttributes redirect) {
		ScheduleInfo stored = scheduleservice.findScheduleById(schedulekey);
		if (stored == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		if (stored.isCancelled()) {
			redirect.addFlashAttribute("errorMessage", "A cancelled interview can't be changed.");
			return new ModelAndView("redirect:/viewschedulelist");
		}
		return form(stored, requireCandidate(stored.getCandidateid()), null);
	}

	@RequestMapping(value = "/saveschedule", method = RequestMethod.POST)
	public ModelAndView save(@ModelAttribute("scheduleinfo") ScheduleInfo scheduleinfo, BindingResult binding,
			HttpSession session, RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		if (scheduleinfo.getSchedulekey() > 0) {
			// The interview may have been deleted while the form was open; its candidate can't be changed
			ScheduleInfo stored = scheduleservice.findScheduleById(scheduleinfo.getSchedulekey());
			if (stored == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			scheduleinfo.setCandidateid(stored.getCandidateid());
		}
		CandidateInfo candidate = requireCandidate(scheduleinfo.getCandidateid());

		if (binding.hasFieldErrors("startat") || binding.hasFieldErrors("schedulekey")) {
			return form(scheduleinfo, candidate, "Please enter the date and time as a date and time.");
		}
		Outcome outcome = scheduleservice.save(scheduleinfo, userid);
		if (!outcome.done()) {
			return form(scheduleinfo, candidate, outcome.message());
		}
		redirect.addFlashAttribute("message", outcome.message());
		return new ModelAndView("redirect:/viewschedulelist");
	}

	/** Cancelling keeps the row: the list shows the interview as cancelled. */
	@RequestMapping(value = "/cancelschedule/{schedulekey}", method = RequestMethod.POST)
	public ModelAndView cancel(@PathVariable("schedulekey") int schedulekey, HttpSession session,
			RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		if (scheduleservice.findScheduleById(schedulekey) == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		Outcome outcome = scheduleservice.cancel(schedulekey, userid);
		redirect.addFlashAttribute(outcome.done() ? "message" : "errorMessage", outcome.message());
		return new ModelAndView("redirect:/viewschedulelist");
	}

	/** The candidate, or HTTP 404 for a missing, blank or deleted one. */
	private CandidateInfo requireCandidate(String candidateid) {
		CandidateInfo candidate = candidateid == null || candidateid.isBlank() ? null
				: candidateservice.findCandidateById(candidateid);
		if (candidate == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return candidate;
	}

	/** The interview form, with the interviewers assigned to the candidate whose account is active. */
	private ModelAndView form(ScheduleInfo scheduleinfo, CandidateInfo candidate, String errorMessage) {
		ModelAndView mv = new ModelAndView("createschedule");
		mv.addObject("scheduleinfo", scheduleinfo);
		mv.addObject("candidate", candidate);
		List<AssignmentInfo> interviewers = new ArrayList<AssignmentInfo>(
				scheduleservice.getSchedulableInterviewers(candidate.getCandidateid()));
		// An interview keeps its interviewer even if they were unassigned or deactivated since, so it can still be
		// marked Done or given a place; only a different interviewer must be an assigned, active one
		String current = scheduleinfo.getInterviewerid();
		if (scheduleinfo.getSchedulekey() > 0 && current != null
				&& interviewers.stream().noneMatch(a -> current.equals(a.getInterviewerid()))) {
			AssignmentInfo former = new AssignmentInfo();
			former.setInterviewerid(current);
			former.setInterviewername((scheduleinfo.getInterviewername() == null ? current : scheduleinfo.getInterviewername())
					+ " (not assigned now)");
			interviewers.add(former);
		}
		mv.addObject("interviewerlist", interviewers);
		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}
}
