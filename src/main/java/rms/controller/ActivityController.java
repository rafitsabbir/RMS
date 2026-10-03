package rms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import rms.service.ActivityService;

/**
 * The activity log (Super Admin only, SecurityConfig): the newest entries, optionally one action. The log is read-only
 * here; nothing in RMS edits or deletes entries.
 */
@Controller
@RequestMapping(value = "/")
public class ActivityController {

	@Autowired
	ActivityService activityservice;

	@RequestMapping(value = "/viewactivity", method = RequestMethod.GET)
	public ModelAndView viewActivity(@RequestParam(value = "action", required = false) String action) {
		ModelAndView mv = new ModelAndView("viewactivity");
		mv.addObject("activitylist", activityservice.getRecent(action));
		mv.addObject("actions", activityservice.getActions());
		mv.addObject("selectedaction", action == null ? "" : action.trim());
		return mv;
	}
}
