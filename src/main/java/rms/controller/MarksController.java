package rms.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import rms.model.MarksInfo;
import rms.service.MarksService;

@Controller
@RequestMapping(value = "/")
public class MarksController {

	@Autowired
	MarksService marksservice;

	@RequestMapping(value = "/adminviewmarks", method = RequestMethod.GET)
	public ModelAndView adminViewMarks() {
		ModelAndView mv = new ModelAndView("viewmarks");
		List<MarksInfo> markslist = new ArrayList<MarksInfo>();
		markslist = marksservice.getAllMarksByAdmin();
		mv.addObject("markslist", markslist);
		return mv;
	}

}
