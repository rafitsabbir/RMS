package rms.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import rms.model.LanguageInfo;
import rms.service.EnglishText;
import rms.service.LanguageService;

@Controller
@RequestMapping(value = "/")
public class LanguageController {

	@Autowired
	LanguageService languageservice;

	@RequestMapping(value = "/createlanguage", method = RequestMethod.GET)
	public ModelAndView createLanguage() {
		ModelAndView mv = new ModelAndView("createlanguage");
		LanguageInfo languageinfo = new LanguageInfo();
		mv.addObject("languageinfo", languageinfo);

		return mv;
	}

	@RequestMapping(value = "/viewlanguagelist", method = RequestMethod.GET)
	public ModelAndView viewLanguage() {
		ModelAndView mv = new ModelAndView("viewlanguage");
		List<LanguageInfo> languagelist = new ArrayList<LanguageInfo>();
		languagelist = languageservice.getAllLanguage();
		mv.addObject("languagelist", languagelist);
		return mv;
	}

	@RequestMapping(value = "/savelanguage", method = RequestMethod.POST)
	public ModelAndView save(
			@ModelAttribute("languageinfo") LanguageInfo languageinfo) {

		String name = languageinfo.getLanguagename();
		if (name == null || name.trim().isEmpty()) {
			return form(languageinfo, "Please enter a language name.");
		}
		if (!EnglishText.isEnglish(name)) {
			return form(languageinfo, "The language name can use English letters only. " + EnglishText.PROBLEM);
		}

		boolean saved;
		if (languageinfo.getLanguagekey() > 0) {
			// The language may have been deleted while the form was open
			if (languageservice.findLanguageById(languageinfo.getLanguagekey()) == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			saved = languageservice.updateLanguage(languageinfo);
		} else {
			saved = languageservice.addLanguage(languageinfo);
		}
		if (!saved) {
			return form(languageinfo, "Language " + name.trim().toUpperCase(Locale.ROOT) + " already exists.");
		}

		return new ModelAndView("redirect:/viewlanguagelist");

	}

	@RequestMapping(value = "/updatelanguage/{languagekey}", method = RequestMethod.GET)
	public ModelAndView update(@PathVariable("languagekey") int languagekey) {
		ModelAndView mv = new ModelAndView("createlanguage");

		LanguageInfo languageinfo = languageservice
				.findLanguageById(languagekey);
		if (languageinfo == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		mv.addObject("languageinfo", languageinfo);

		return mv;

	}
	
	@RequestMapping(value = "/deletelanguage/{languagekey}", method = RequestMethod.POST)
	public ModelAndView delete(@PathVariable("languagekey") int languagekey) {

		languageservice.deleteLanguage(languagekey);

		return new ModelAndView("redirect:/viewlanguagelist");
	}

	/** The create/update form again, with the entered values and an error. */
	private ModelAndView form(LanguageInfo languageinfo, String errorMessage) {
		ModelAndView mv = new ModelAndView("createlanguage");
		mv.addObject("languageinfo", languageinfo);
		mv.addObject("errorMessage", errorMessage);
		return mv;
	}
}
