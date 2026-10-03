package rms.controller;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import rms.model.Role;
import rms.model.UserInfo;
import rms.service.ActivityService;
import rms.service.EnglishText;
import rms.service.PasswordRules;
import rms.service.UserService;

/**
 * Users and Roles (Super Admin only, SecurityConfig): add a user with a temporary password, edit the profile and
 * role, deactivate and reactivate (soft: the user's rows and history stay), and reset a password. A Super Admin
 * can't deactivate, demote or reset themselves here, and the last active Super Admin can't be deactivated or
 * demoted. Changes apply to a logged-in user at their next request (rms.config.AccountCheckFilter).
 */
@Controller
@RequestMapping(value = "/")
public class UserController {
	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	/** New usernames: letters, digits and . _ @ - (existing ones are left as they are). */
	private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._@-]{3,100}");
	private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

	@Autowired
	UserService userservice;

	@Autowired
	ActivityService activityservice;

	/** Only these fields bind from the form; the active flag, the forced-change flag and the like never do. */
	@InitBinder("userinfo")
	void bindProfileFieldsOnly(WebDataBinder binder) {
		binder.setAllowedFields("userid", "username", "firstname", "lastname", "email", "phone", "designation",
				"role");
	}

	@RequestMapping(value = "/viewuserlist", method = RequestMethod.GET)
	public ModelAndView viewUsers(HttpSession session) {
		return list(session, null);
	}

	@RequestMapping(value = "/createuser", method = RequestMethod.GET)
	public ModelAndView createUser() {
		return form(new UserInfo(), false, null);
	}

	/** A request parameter, not a path variable: older user IDs may be any text (G22). */
	@RequestMapping(value = "/updateuser", method = RequestMethod.GET)
	public ModelAndView updateUser(HttpSession session, @RequestParam("userid") String userid,
			@RequestParam(value = "passwordreset", required = false) String passwordreset) {
		UserInfo stored = found(userservice.findUserById(userid));
		ModelAndView mv = form(stored, true, null);
		mv.addObject("self", stored.getUserid().equals(currentUserid(session)));
		mv.addObject("passwordreset", passwordreset != null);
		return mv;
	}

	/** One endpoint for add and edit; the edit form posts update=true. The username and ID can't change. */
	@RequestMapping(value = "/saveuser", method = RequestMethod.POST)
	public ModelAndView save(HttpSession session,
			@ModelAttribute("userinfo") UserInfo userinfo,
			@RequestParam(value = "update", defaultValue = "false") boolean update,
			@RequestParam(value = "password", defaultValue = "") String password,
			@RequestParam(value = "confirmpassword", defaultValue = "") String confirmpassword) {
		userinfo.setFirstname(trim(userinfo.getFirstname()));
		userinfo.setLastname(trim(userinfo.getLastname()));
		userinfo.setEmail(blankToNull(userinfo.getEmail()));
		userinfo.setPhone(blankToNull(userinfo.getPhone()));
		userinfo.setDesignation(blankToNull(userinfo.getDesignation()));
		Role role = Role.parse(userinfo.getRole());
		userinfo.setRole(role == null ? null : role.name());

		if (update) {
			UserInfo stored = found(userservice.findUserById(trim(userinfo.getUserid())));
			userinfo.setUserid(stored.getUserid());
			userinfo.setUsername(stored.getUsername());
			userinfo.setIsactive(stored.getIsactive());
			String errorMessage = checkProfile(userinfo);
			if (errorMessage == null) {
				errorMessage = checkRoleChange(session, stored, role);
			}
			if (errorMessage != null) {
				ModelAndView mv = form(userinfo, true, errorMessage);
				mv.addObject("self", stored.getUserid().equals(currentUserid(session)));
				return mv;
			}
			userservice.updateUser(userinfo);
			activityservice.record(currentUserid(session), "USER_CHANGED", "USER", stored.getUserid(), "role " + userinfo.getRole());
			log.info("User {} edited by {}", stored.getUserid(), currentUserid(session));
			return new ModelAndView("redirect:/viewuserlist");
		}

		userinfo.setUserid(null);
		userinfo.setUsername(trim(userinfo.getUsername()));
		String errorMessage = checkUsername(userinfo.getUsername());
		if (errorMessage == null) {
			errorMessage = checkProfile(userinfo);
		}
		if (errorMessage == null) {
			errorMessage = PasswordRules.problem(password, confirmpassword);
		}
		if (errorMessage != null) {
			return form(userinfo, false, errorMessage);
		}
		String userid = userservice.addUser(userinfo, password);
		if (userid == null) {
			return form(userinfo, false, "That username is already taken.");
		}
		activityservice.record(currentUserid(session), "USER_ADDED", "USER", userid, "role " + userinfo.getRole());
		log.info("User {} created by {}", userid, currentUserid(session));
		return new ModelAndView("redirect:/viewuserlist");
	}

	/** Soft: blocks the login only. The user's evaluations, schedules, assignments and history stay. */
	@RequestMapping(value = "/deactivateuser", method = RequestMethod.POST)
	public ModelAndView deactivate(HttpSession session, @RequestParam("userid") String userid) {
		UserInfo stored = found(userservice.findUserById(userid));
		if (stored.getUserid().equals(currentUserid(session))) {
			return list(session, "You can't deactivate yourself.");
		}
		if (stored.getIsactive() == 1 && isSuperAdmin(stored) && userservice.countActiveSuperAdmins() <= 1) {
			return list(session, "There must be at least one active Super Admin.");
		}
		userservice.setActive(stored.getUserid(), false);
		activityservice.record(currentUserid(session), "USER_DEACTIVATED", "USER", stored.getUserid(), null);
		log.info("User {} deactivated by {}", stored.getUserid(), currentUserid(session));
		return new ModelAndView("redirect:/viewuserlist");
	}

	@RequestMapping(value = "/reactivateuser", method = RequestMethod.POST)
	public ModelAndView reactivate(HttpSession session, @RequestParam("userid") String userid) {
		UserInfo stored = found(userservice.findUserById(userid));
		userservice.setActive(stored.getUserid(), true);
		activityservice.record(currentUserid(session), "USER_REACTIVATED", "USER", stored.getUserid(), null);
		log.info("User {} reactivated by {}", stored.getUserid(), currentUserid(session));
		return new ModelAndView("redirect:/viewuserlist");
	}

	/** Sets a temporary password; the user must choose their own at the next page they open. */
	@RequestMapping(value = "/resetpassword", method = RequestMethod.POST)
	public ModelAndView resetPassword(HttpSession session, @RequestParam("userid") String userid,
			@RequestParam(value = "newpassword", defaultValue = "") String newpassword,
			@RequestParam(value = "confirmpassword", defaultValue = "") String confirmpassword) {
		UserInfo stored = found(userservice.findUserById(userid));
		boolean self = stored.getUserid().equals(currentUserid(session));

		String passwordMessage;
		if (self) {
			passwordMessage = "Use Change password for your own password.";
		} else if (stored.getUsername() == null) {
			passwordMessage = "This user has no login, so there is no password to reset.";
		} else {
			passwordMessage = PasswordRules.problem(newpassword, confirmpassword);
		}
		if (passwordMessage != null) {
			ModelAndView mv = form(stored, true, null);
			mv.addObject("self", self);
			mv.addObject("passwordMessage", passwordMessage);
			return mv;
		}

		userservice.resetPassword(stored.getUserid(), newpassword);
		activityservice.record(currentUserid(session), "PASSWORD_RESET", "USER", stored.getUserid(), null);
		log.info("Password of user {} reset by {}", stored.getUserid(), currentUserid(session));
		// The redirect encodes the model as query parameters
		ModelAndView mv = new ModelAndView("redirect:/updateuser");
		mv.addObject("userid", stored.getUserid());
		mv.addObject("passwordreset", "true");
		return mv;
	}

	private String checkUsername(String username) {
		if (username.isEmpty()) {
			return "Please enter a username.";
		}
		if (!USERNAME.matcher(username).matches()) {
			return "The username must be 3 to 100 letters, digits or . _ @ - characters.";
		}
		return null;
	}

	/** The first problem with the profile fields, or null. The lengths are the columns' (db/schema.sql). */
	private String checkProfile(UserInfo userinfo) {
		if (userinfo.getFirstname().isEmpty()) {
			return "Please enter the first name.";
		}
		if (userinfo.getLastname().isEmpty()) {
			return "Please enter the last name.";
		}
		if (userinfo.getFirstname().length() > 100 || userinfo.getLastname().length() > 100) {
			return "The names can have at most 100 characters.";
		}
		if (!EnglishText.isEnglish(userinfo.getFirstname()) || !EnglishText.isEnglish(userinfo.getLastname())
				|| !EnglishText.isEnglish(userinfo.getDesignation())) {
			return "Names and designation can use English letters only. " + EnglishText.PROBLEM;
		}
		String email = userinfo.getEmail();
		if (email != null && (email.length() > 150 || !EMAIL.matcher(email).matches())) {
			return "Please enter a valid e-mail address, or leave it empty.";
		}
		if (userinfo.getPhone() != null && userinfo.getPhone().length() > 30) {
			return "The phone number can have at most 30 characters.";
		}
		if (userinfo.getDesignation() != null && userinfo.getDesignation().length() > 100) {
			return "The designation can have at most 100 characters.";
		}
		if (userinfo.getRole() == null) {
			return "Please choose a role.";
		}
		return null;
	}

	/** Refuses changing one's own role, and demoting the last active Super Admin. */
	private String checkRoleChange(HttpSession session, UserInfo stored, Role role) {
		if (Role.parse(stored.getRole()) == role) {
			return null;
		}
		if (stored.getUserid().equals(currentUserid(session))) {
			return "You can't change your own role.";
		}
		if (stored.getIsactive() == 1 && isSuperAdmin(stored) && userservice.countActiveSuperAdmins() <= 1) {
			return "There must be at least one active Super Admin.";
		}
		return null;
	}

	private ModelAndView list(HttpSession session, String errorMessage) {
		ModelAndView mv = new ModelAndView("viewuser");
		mv.addObject("userlist", userservice.getAllUsers());
		mv.addObject("currentuserid", currentUserid(session));
		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}

	private ModelAndView form(UserInfo userinfo, boolean update, String errorMessage) {
		ModelAndView mv = new ModelAndView("createuser");
		mv.addObject("userinfo", userinfo);
		mv.addObject("update", update);
		mv.addObject("roles", Role.values());
		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}

	private static UserInfo found(UserInfo userinfo) {
		if (userinfo == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return userinfo;
	}

	private static boolean isSuperAdmin(UserInfo userinfo) {
		return Role.parse(userinfo.getRole()) == Role.SUPER_ADMIN;
	}

	/** The logged-in user, from the session profile SecurityConfig and AccountCheckFilter keep there. */
	private static String currentUserid(HttpSession session) {
		Object user = session.getAttribute("user");
		return user instanceof UserInfo ? ((UserInfo) user).getUserid() : null;
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private static String blankToNull(String value) {
		String trimmed = trim(value);
		return trimmed.isEmpty() ? null : trimmed;
	}
}
