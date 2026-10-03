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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import rms.model.JobInfo;
import rms.model.PositionInfo;
import rms.service.ActivityService;
import rms.service.JobService;
import rms.service.PositionService;

/**
 * Jobs (Phase 2 of the roles plan): an opening built on a position, with vacancies, a closing date and a status
 * HR sets (OPEN or CLOSED; nothing closes a job automatically). Soft delete. Copied from PositionController.
 */
@Controller
@RequestMapping(value = "/")
public class JobController {

	static final int MAX_VACANCIES = 999;

	@Autowired
	JobService jobservice;

	@Autowired
	ActivityService activityservice;

	@Autowired
	PositionService positionservice;

	/** Only the form's fields bind. */
	@InitBinder("jobinfo")
	void bindFormFieldsOnly(WebDataBinder binder) {
		binder.setAllowedFields("jobkey", "positionkey", "vacancies", "closingdate", "status");
	}

	@RequestMapping(value = "/viewjoblist", method = RequestMethod.GET)
	public ModelAndView viewJob() {
		ModelAndView mv = new ModelAndView("viewjob");
		mv.addObject("joblist", jobservice.getAllJob());
		return mv;
	}

	@RequestMapping(value = "/createjob", method = RequestMethod.GET)
	public ModelAndView createJob() {
		return form(new JobInfo(), null, null);
	}

	@RequestMapping(value = "/updatejob/{jobkey}", method = RequestMethod.GET)
	public ModelAndView update(@PathVariable("jobkey") int jobkey) {
		JobInfo jobinfo = jobservice.findJobById(jobkey);
		if (jobinfo == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return form(jobinfo, jobinfo, null);
	}

	@RequestMapping(value = "/savejob", method = RequestMethod.POST)
	public ModelAndView save(@ModelAttribute("jobinfo") JobInfo jobinfo, BindingResult binding, HttpSession session) {
		JobInfo stored = null;
		if (jobinfo.getJobkey() > 0) {
			// The job may have been deleted while the form was open
			stored = jobservice.findJobById(jobinfo.getJobkey());
			if (stored == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
		}

		String errorMessage = check(jobinfo, stored, binding);
		if (errorMessage != null) {
			return form(jobinfo, stored, errorMessage);
		}

		if (stored != null) {
			jobservice.updateJob(jobinfo);
			activityservice.record(CurrentUser.userid(session), "JOB_CHANGED", "JOB", String.valueOf(jobinfo.getJobkey()), null);
		} else {
			jobservice.addJob(jobinfo);
			activityservice.record(CurrentUser.userid(session), "JOB_ADDED", "JOB", null, "position " + jobinfo.getPositionkey());
		}
		return new ModelAndView("redirect:/viewjoblist");
	}

	/** Soft delete: candidates linked to the job keep the link. */
	@RequestMapping(value = "/deletejob/{jobkey}", method = RequestMethod.POST)
	public ModelAndView delete(@PathVariable("jobkey") int jobkey, HttpSession session) {
		jobservice.deleteJob(jobkey);
		activityservice.record(CurrentUser.userid(session), "JOB_DELETED", "JOB", String.valueOf(jobkey), null);
		return new ModelAndView("redirect:/viewjoblist");
	}

	/** The first problem with the entered values, or null. stored is the saved job when editing. */
	private String check(JobInfo jobinfo, JobInfo stored, BindingResult binding) {
		// An active position, or the one the job already has (it may have been deleted since)
		int positionkey = jobinfo.getPositionkey();
		if (binding.hasFieldErrors("positionkey") || positionkey <= 0
				|| (positionservice.findPositionById(positionkey) == null
						&& (stored == null || stored.getPositionkey() != positionkey))) {
			return "Please choose a position.";
		}
		if (binding.hasFieldErrors("vacancies") || jobinfo.getVacancies() < 1
				|| jobinfo.getVacancies() > MAX_VACANCIES) {
			return "Please enter the number of vacancies, from 1 to " + MAX_VACANCIES + ".";
		}
		if (binding.hasFieldErrors("closingdate")) {
			return "Please enter the closing date as a date, or leave it empty.";
		}
		if (!JobInfo.OPEN.equals(jobinfo.getStatus()) && !JobInfo.CLOSED.equals(jobinfo.getStatus())) {
			return "Please choose whether the job is open or closed.";
		}
		return null;
	}

	/** The add/edit form, with the active positions; when editing, a deleted position the job still has too. */
	private ModelAndView form(JobInfo jobinfo, JobInfo stored, String errorMessage) {
		ModelAndView mv = new ModelAndView("createjob");
		mv.addObject("jobinfo", jobinfo);

		List<PositionInfo> positionlist = new ArrayList<PositionInfo>(positionservice.getAllPosition());
		if (stored != null) {
			int positionkey = stored.getPositionkey();
			if (positionkey > 0 && positionlist.stream().noneMatch(p -> p.getPositionkey() == positionkey)) {
				PositionInfo deleted = new PositionInfo();
				deleted.setPositionkey(positionkey);
				deleted.setPositionname((stored.getPositionname() == null ? "Position " + positionkey
						: stored.getPositionname()) + " (deleted)");
				positionlist.add(deleted);
			}
		}
		mv.addObject("positionlist", positionlist);

		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}
}
