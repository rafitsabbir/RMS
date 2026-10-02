package rms.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import rms.config.LoginThrottle;
import rms.model.UserInfo;
import rms.service.PasswordRules;
import rms.service.UserService;

/**
 * Change password, for every logged-in user. After a reset (users.mustchangepassword = 1) it is the only page
 * the user can open (rms.config.AccountCheckFilter). Wrong current passwords are counted the way failed logins
 * are (5 in 15 minutes lock the page for 15 minutes), so a session left open can't be used to guess the password.
 */
@Controller
@RequestMapping(value = "/")
public class AccountController {
	private static final Logger log = LoggerFactory.getLogger(AccountController.class);

	@Autowired
	UserService userservice;

	/** Its own counts, by userid: the login's counts are by username and can't reach these. */
	LoginThrottle throttle = new LoginThrottle();

	@RequestMapping(value = "/changepassword", method = RequestMethod.GET)
	public ModelAndView changePassword(HttpSession session,
			@RequestParam(value = "changed", required = false) String changed) {
		ModelAndView mv = form(session, null);
		mv.addObject("changed", changed != null);
		return mv;
	}

	@RequestMapping(value = "/savepassword", method = RequestMethod.POST)
	public ModelAndView savePassword(HttpSession session, HttpServletRequest request,
			@RequestParam(value = "currentpassword", defaultValue = "") String currentpassword,
			@RequestParam(value = "newpassword", defaultValue = "") String newpassword,
			@RequestParam(value = "confirmpassword", defaultValue = "") String confirmpassword) {
		UserInfo user = (UserInfo) session.getAttribute("user");
		if (user == null) {
			// AccountCheckFilter sets it for every logged-in request
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		String userid = user.getUserid();
		String address = request.getRemoteAddr();

		if (throttle.isLocked(userid, address)) {
			return form(session, "Too many wrong passwords. Please try again later.");
		}
		if (currentpassword.isEmpty()) {
			return form(session, "Please enter your current password.");
		}
		String problem = PasswordRules.problem(newpassword, confirmpassword);
		if (problem != null) {
			return form(session, problem);
		}
		if (newpassword.equals(currentpassword)) {
			return form(session, "The new password must be different from the current one.");
		}
		if (!userservice.changePassword(userid, currentpassword, newpassword)) {
			throttle.loginFailed(userid, address);
			return form(session, "The current password is wrong.");
		}

		throttle.loginSucceeded(userid, address);
		log.info("User {} changed their password", userid);
		return new ModelAndView("redirect:/changepassword?changed");
	}

	private ModelAndView form(HttpSession session, String errorMessage) {
		UserInfo user = (UserInfo) session.getAttribute("user");
		ModelAndView mv = new ModelAndView("changepassword");
		mv.addObject("mustchange", user != null && user.getMustchangepassword() == 1);
		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}
}
