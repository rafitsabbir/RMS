package rms.config;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import rms.model.Role;
import rms.model.UserInfo;
import rms.service.LoginService;
import rms.service.RmsUserDetails;

/**
 * Re-reads the logged-in user's admin row on every request, so changes made on the Users screen apply at once:
 * <ul>
 * <li>deactivated, admin row gone, or role changed: the session ends and the browser goes to /login?ended</li>
 * <li>a password reset (users.mustchangepassword = 1): every page except Change password redirects there</li>
 * <li>otherwise the session's profile ("user", read by the JSPs) is refreshed, so profile edits show at once</li>
 * </ul>
 * Runs after the login filters and before the access rules (SecurityConfig.appFilterChain).
 */
class AccountCheckFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(AccountCheckFilter.class);

	/** What a user who must change their password may still open (logout is handled before this filter). */
	private static final Set<String> PASSWORD_CHANGE_PATHS = Set.of("/changepassword", "/savepassword", "/login");

	private final LoginService loginservice;

	AccountCheckFilter(LoginService loginservice) {
		this.loginservice = loginservice;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext()
				.getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof RmsUserDetails)) {
			chain.doFilter(request, response);
			return;
		}

		RmsUserDetails user = (RmsUserDetails) authentication.getPrincipal();
		UserInfo now = loginservice.currentProfile(user.getUserid());
		if (now == null || now.getIsactive() != 1 || Role.parse(now.getRole()) != user.getRole()) {
			log.info("User {} signed out: the account was deactivated or its role changed", user.getUserid());
			new SecurityContextLogoutHandler().logout(request, response, authentication);
			response.sendRedirect(request.getContextPath() + "/login?ended");
			return;
		}

		request.getSession().setAttribute("user", now);
		if (now.getMustchangepassword() == 1 && !PASSWORD_CHANGE_PATHS.contains(SecurityConfig.pathOf(request))) {
			response.sendRedirect(request.getContextPath() + "/changepassword");
			return;
		}
		chain.doFilter(request, response);
	}

}
