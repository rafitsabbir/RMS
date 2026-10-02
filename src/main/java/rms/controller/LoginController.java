package rms.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * The login page and the home page. Spring Security handles the login post (POST /welcome) and the logout
 * (POST /logout); see rms.config.SecurityConfig.
 */
@Controller
@RequestMapping(value = "/")
public class LoginController {

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
		return mv;
	}
}
