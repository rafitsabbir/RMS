package rms.config;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import rms.model.UserInfo;

/**
 * Login and role check for every page except those excluded in WebConfig.addInterceptors (G11).
 * Every page behind the login is an admin page today; interviewer pages (G5) must be let through explicitly.
 */
public class AuthInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

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
		if (!"N".equalsIgnoreCase(userinfo.getIsinterviewer())) {
			// servlet path, not the request URI: a rewritten URL carries ;jsessionid=...
			log.warn("User {} is not an admin; {} refused", userinfo.getUserid(), request.getServletPath());
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		return true;
	}

}
