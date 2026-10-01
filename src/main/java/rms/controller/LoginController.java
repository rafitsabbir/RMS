package rms.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import rms.model.UserInfo;
import rms.service.LoginService;

@Controller
@RequestMapping(value = "/")
public class LoginController {
	private static final Logger log = LoggerFactory.getLogger(LoginController.class);

	@Autowired
	LoginService loginservice;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView root() {
		return new ModelAndView("redirect:/home");
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView loginPage(HttpSession session) {
		ModelAndView mv = new ModelAndView("login");
		session.removeAttribute("user");
		session.invalidate();
		return mv;
	}

	@RequestMapping(value = "/welcome", method = RequestMethod.POST)
	public ModelAndView doLogin(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		ModelAndView mv;

		String userid = null;
		userid = loginservice.checkLogin(username, password);
		UserInfo userinfo = userid == null ? null : loginservice.getUserInfo(userid);
		if (userid != null && userinfo == null) {
			log.warn("User {} has no admin row; login refused", userid);
		}

		if (userinfo != null) {
			// A new session id after login, so an id set before it is worthless
			request.changeSessionId();
			session.setAttribute("user", userinfo);
			// Redirect, so a refresh of the menu page doesn't post the login again
			return new ModelAndView("redirect:/home");
		} else {
			session.removeAttribute("user");
			session.invalidate();
			mv = new ModelAndView("login");
			mv.addObject("errorMessage", "Invalid login!");
			return mv;
		}
	}

	@RequestMapping(value = "/home", method = RequestMethod.GET)
	public ModelAndView home(HttpSession session) {
		ModelAndView mv = new ModelAndView("main");
		mv.addObject("userinfo", session.getAttribute("user"));
		return mv;
	}
}
