package rms.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

	@Autowired
	LoginService loginservice;	
	UserInfo userinfo;

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

		if (userid != null) {
			mv = new ModelAndView("main");
			userinfo = loginservice.getUserInfo(userid);
			mv.addObject("userinfo", userinfo);
			session.setAttribute("user", userinfo);
			return mv;
		} else {
			session.removeAttribute("user");
			session.invalidate();
			mv = new ModelAndView("login");
			mv.addObject("errorMessage", "Invalid login!");
			return mv;
		}
	}
}
