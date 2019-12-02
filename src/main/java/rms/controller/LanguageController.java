package rms.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import rms.model.LanguageInfo;
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

		if (languageinfo != null && languageinfo.getLanguagekey() > 0) {
			languageservice.updateLanguage(languageinfo);
		} else {
			languageservice.addLanguage(languageinfo);
		}

		return new ModelAndView("redirect:/viewlanguagelist");

	}

	@RequestMapping(value = "/updatelanguage/{languagekey}", method = RequestMethod.GET)
	public ModelAndView update(@PathVariable("languagekey") int languagekey) {
		ModelAndView mv = new ModelAndView("createlanguage");

		LanguageInfo languageinfo = languageservice
				.findLanguageById(languagekey);
		mv.addObject("languageinfo", languageinfo);

		return mv;

	}
	
	@RequestMapping(value = "/deletelanguage/{languagekey}", method = RequestMethod.GET)
	public ModelAndView delete(@PathVariable("languagekey") int languagekey) {

		languageservice.deleteLanguage(languagekey);

		return new ModelAndView("redirect:/viewlanguagelist");
	}

}
