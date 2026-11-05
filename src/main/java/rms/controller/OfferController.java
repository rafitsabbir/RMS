package rms.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rms.model.OfferInfo;
import rms.service.CandidateService;
import rms.service.OfferService;
import rms.service.Outcome;

/** Offers (F8): Super Admin and HR record them on the candidate profile (SecurityConfig.HR_PAGES). */
@Controller
@RequestMapping(value = "/")
public class OfferController {

	@Autowired
	OfferService offerservice;

	@Autowired
	CandidateService candidateservice;

	/** The form's fields only; who made the offer and when it was answered are RMS's. */
	@InitBinder("offerinfo")
	void bindOfferFieldsOnly(WebDataBinder binder) {
		binder.setAllowedFields("offerkey", "candidateid", "offerdate", "salaryband", "startdate", "status", "note");
	}

	@RequestMapping(value = "/saveoffer", method = RequestMethod.POST)
	public ModelAndView save(@ModelAttribute("offerinfo") OfferInfo offerinfo, BindingResult binding,
			HttpSession session, RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		String candidateid = offerinfo.getCandidateid();
		if (candidateid == null || candidateservice.findCandidateById(candidateid) == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		Outcome outcome = binding.hasErrors() ? Outcome.refused("Please enter the dates as dates.")
				: offerservice.saveOffer(offerinfo, userid);
		redirect.addFlashAttribute(outcome.done() ? "message" : "errorMessage", outcome.message());
		return new ModelAndView("redirect:/viewcandidate?candidateid="
				+ URLEncoder.encode(candidateid, StandardCharsets.UTF_8) + "#offer");
	}
}
