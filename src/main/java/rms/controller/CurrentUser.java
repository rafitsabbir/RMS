package rms.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import rms.model.Role;
import rms.model.UserInfo;

/**
 * The logged-in user, from the session profile SecurityConfig puts there at login and AccountCheckFilter refreshes on
 * every request.
 */
final class CurrentUser {

	private CurrentUser() {
	}

	/** The user's ID, or null without a session profile. */
	static String userid(HttpSession session) {
		Object user = session.getAttribute("user");
		return user instanceof UserInfo ? ((UserInfo) user).getUserid() : null;
	}

	/** The user's ID for recording who did something; HTTP 403 without a session profile. */
	static String requireUserid(HttpSession session) {
		String userid = userid(session);
		if (userid == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		return userid;
	}

	/** The user's effective role, or null (no profile, or no role). */
	static Role role(HttpSession session) {
		Object user = session.getAttribute("user");
		return user instanceof UserInfo ? Role.parse(((UserInfo) user).getRole()) : null;
	}

	/** Super Admin, HR or Hiring Manager: may see every candidate. Anyone else only their assigned ones. */
	static boolean isStaff(HttpSession session) {
		Role role = role(session);
		return role == Role.SUPER_ADMIN || role == Role.HR || role == Role.HIRING_MANAGER;
	}

	/** Super Admin or HR: may change candidates, assignments and decisions. */
	static boolean canEdit(HttpSession session) {
		Role role = role(session);
		return role == Role.SUPER_ADMIN || role == Role.HR;
	}
}
