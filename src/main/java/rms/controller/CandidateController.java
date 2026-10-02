package rms.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import rms.model.CandidateInfo;
import rms.model.LanguageInfo;
import rms.model.PositionInfo;
import rms.service.CandidateService;
import rms.service.LanguageService;
import rms.service.PositionService;

/**
 * Candidate management (G1): add, list and edit. RMS generates the candidate ID (C1, C2, ...; owner decision
 * 2026-10-02). There is no delete, because the candidate table has no known isactive column (open question #26).
 */
@Controller
@RequestMapping(value = "/")
public class CandidateController {

	@Autowired
	CandidateService candidateservice;

	@Autowired
	PositionService positionservice;

	@Autowired
	LanguageService languageservice;

	@RequestMapping(value = "/createcandidate", method = RequestMethod.GET)
	public ModelAndView createCandidate() {
		return form(new CandidateInfo(), null, null);
	}

	/** One endpoint for add and edit; the edit form posts update=true and can't change the ID. */
	@RequestMapping(value = "/savecandidate", method = RequestMethod.POST)
	public ModelAndView save(
			@ModelAttribute("candidateinfo") CandidateInfo candidateinfo,
			@RequestParam(value = "update", defaultValue = "false") boolean update) {

		// A new candidate's ID is generated on save; an ID posted with it is ignored
		candidateinfo.setCandidateid(update ? trim(candidateinfo.getCandidateid()) : null);
		candidateinfo.setFirstname(trim(candidateinfo.getFirstname()));
		candidateinfo.setLastname(trim(candidateinfo.getLastname()));

		CandidateInfo stored = null;
		if (update) {
			stored = candidateservice.findCandidateById(candidateinfo.getCandidateid());
			if (stored == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
		}

		String errorMessage = check(candidateinfo, stored);
		if (errorMessage != null) {
			return form(candidateinfo, stored, errorMessage);
		}

		if (update) {
			candidateservice.updateCandidate(candidateinfo);
		} else {
			candidateservice.addCandidate(candidateinfo);
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

	/** The first problem with the entered values, or null. stored is the saved candidate when editing. */
	private String check(CandidateInfo candidateinfo, CandidateInfo stored) {
		if (candidateinfo.getFirstname().isEmpty()) {
			return "Please enter the first name.";
		}
		if (candidateinfo.getLastname().isEmpty()) {
			return "Please enter the last name.";
		}
		// An active position, or the one the candidate already has (it may have been deleted since)
		int positionkey = candidateinfo.getPositionkey();
		if (positionkey <= 0 || (positionservice.findPositionById(positionkey) == null
				&& (stored == null || stored.getPositionkey() != positionkey))) {
			return "Please choose a position.";
		}
		int languagekey = candidateinfo.getLanguagekey();
		if (languagekey <= 0 || (languageservice.findLanguageById(languagekey) == null
				&& (stored == null || stored.getLanguagekey() != languagekey))) {
			return "Please choose a language.";
		}
		return null;
	}

	/**
	 * The add/edit form, with the active positions and languages to choose from. When editing, a deleted
	 * position or language the candidate still has is offered too, so saving doesn't silently change it.
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
