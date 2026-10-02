package rms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import rms.model.Criterion;
import rms.service.MarksService;

/**
 * Candidate Status (staff): since Phase 3 one row per active candidate with the number of evaluations, the average of
 * each criterion and the total of the averages, and the decision. The detail is EvaluationController.viewEvaluations.
 */
@Controller
@RequestMapping(value = "/")
public class MarksController {

	@Autowired
	MarksService marksservice;

	@RequestMapping(value = "/adminviewmarks", method = RequestMethod.GET)
	public ModelAndView adminViewMarks() {
		ModelAndView mv = new ModelAndView("viewmarks");
		mv.addObject("resultlist", marksservice.getResults());
		mv.addObject("criteria", Criterion.values());
		return mv;
	}

}
