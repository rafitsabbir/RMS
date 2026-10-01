package rms.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import rms.model.UserInfo;

/**
 * Login and role check (G11), registered twice in WebConfig.addInterceptors: the menu page needs a login,
 * every other page behind the login also needs the admin role. Interviewer pages (G5) must be added to the
 * login-only registration.
 */
public class AuthInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

	private final boolean adminOnly;

	public AuthInterceptor(boolean adminOnly) {
		this.adminOnly = adminOnly;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		HttpSession session = request.getSession(false);
		Object user = session == null ? null : session.getAttribute("user");

		if (!(user instanceof UserInfo)) {
			response.sendRedirect(request.getContextPath() + "/login");
			return false;
		}

		UserInfo userinfo = (UserInfo) user;
		if (adminOnly && !"N".equalsIgnoreCase(userinfo.getIsinterviewer())) {
			// servlet path, not the request URI: a rewritten URL carries ;jsessionid=...
			log.warn("User {} is not an admin; {} refused", userinfo.getUserid(), request.getServletPath());
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		return true;
	}

}
