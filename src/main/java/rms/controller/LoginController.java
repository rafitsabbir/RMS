package rms.controller;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import rms.model.DashboardInfo;
import rms.service.DashboardService;

/**
 * The login page and the home page, with its dashboard (Phase 4). Spring Security handles the login post (POST /welcome) and the logout
 * (POST /logout); see rms.config.SecurityConfig.
 */
@Controller
@RequestMapping(value = "/")
public class LoginController {

	private static final Logger log = LoggerFactory.getLogger(LoginController.class);

	@Autowired
	DashboardService dashboardservice;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView root() {
		return new ModelAndView("redirect:/home");
	}

	/** No longer logs out: a GET from another site could otherwise end the session (G32). */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView loginPage(@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "expired", required = false) String expired,
			@RequestParam(value = "unavailable", required = false) String unavailable,
			@RequestParam(value = "ended", required = false) String ended) {
		ModelAndView mv = new ModelAndView("login");
		if (error != null) {
			mv.addObject("errorMessage", "Invalid login!");
		} else if (expired != null) {
			mv.addObject("errorMessage", "Your session expired. Please sign in again.");
		} else if (unavailable != null) {
			mv.addObject("errorMessage", "Sign-in isn't available right now. Please try again later.");
		} else if (ended != null) {
			// AccountCheckFilter: the account was deactivated, or its role changed, while logged in
			mv.addObject("errorMessage", "You were signed out because your account changed. Please sign in again.");
		}
		return mv;
	}

	@RequestMapping(value = "/home", method = RequestMethod.GET)
	public ModelAndView home(HttpSession session) {
		ModelAndView mv = new ModelAndView("main");
		mv.addObject("userinfo", session.getAttribute("user"));
		DashboardInfo dashboard = dashboard(session);
		if (dashboard != null) {
			mv.addObject("dashboard", dashboard);
		}
		return mv;
	}

	/**
	 * The dashboard for the user's role, or null. A failure here (for example migration 006 not run yet) is logged
	 * and the home page still opens, without the figures.
	 */
	private DashboardInfo dashboard(HttpSession session) {
		try {
			return dashboardservice.getDashboard(CurrentUser.role(session), CurrentUser.userid(session));
		} catch (DataAccessException e) {
			log.warn("The dashboard isn't available: {}", e.getMostSpecificCause().getClass().getSimpleName());
			return null;
		}
	}
}
