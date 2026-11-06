package rms.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rms.service.Outcome;
import rms.service.RetentionService;

/** Data retention (F11): Super Admin only (SecurityConfig.SUPER_ADMIN_PAGES). */
@Controller
@RequestMapping(value = "/")
public class RetentionController {

	@Autowired
	RetentionService retentionservice;

	@RequestMapping(value = "/viewretention", method = RequestMethod.GET)
	public ModelAndView view() {
		ModelAndView mv = new ModelAndView("viewretention");
		mv.addObject("months", retentionservice.getMonths());
		mv.addObject("duelist", retentionservice.getDue());
		return mv;
	}

	@RequestMapping(value = "/applyretention", method = RequestMethod.POST)
	public ModelAndView apply(@RequestParam(value = "candidateid", required = false) List<String> candidateids,
			HttpSession session, RedirectAttributes redirect) {
		Outcome outcome = retentionservice.cleanUp(candidateids, CurrentUser.requireUserid(session));
		redirect.addFlashAttribute(outcome.done() ? "message" : "errorMessage", outcome.message());
		return new ModelAndView("redirect:/viewretention");
	}
}
