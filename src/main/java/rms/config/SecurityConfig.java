package rms.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.NullRequestCache;

import rms.service.LoginService;
import rms.service.RmsUserDetails;

/**
 * Spring Security 7, in place of the interim AuthInterceptor (G11, G13, G32):
 * <ul>
 * <li>form login posts to /welcome (login.jsp) and always lands on /home; a failed one goes to /login?error</li>
 * <li>/login is public, / and /home need a login, every other page needs ROLE_ADMIN (isinterviewer = N)</li>
 * <li>every POST needs the CSRF token, which &lt;form:form&gt; adds; logout is POST /logout</li>
 * <li>passwords: {bcrypt} rows, or the legacy plain-text rows compared exactly; nothing is written back</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	/** Static resources: public, and without the no-store headers, so browsers keep caching them. */
	@Bean
	@Order(1)
	public SecurityFilterChain resourceFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/resources/**")
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.headers(headers -> headers.cacheControl(cache -> cache.disable()));
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
						// A JSP forward belongs to a request that was already checked
						.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR)
						.permitAll()
						.requestMatchers("/login", "/welcome").permitAll()
						.requestMatchers("/", "/home").authenticated()
						// Everything else is an admin page. Interviewer pages (G5) need a rule above this one
						.anyRequest().hasRole("ADMIN"))
				.formLogin(form -> form
						.loginPage("/login")
						.loginProcessingUrl("/welcome")
						.usernameParameter("username")
						.passwordParameter("password")
						.successHandler(new LoginSuccessHandler())
						.failureHandler(SecurityConfig::loginFailed))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login"))
				// After a login always /home, as before, so the page that led to the login isn't kept
				.requestCache(cache -> cache.requestCache(new NullRequestCache()))
				.exceptionHandling(exceptions -> exceptions.accessDeniedHandler(SecurityConfig::refuse));
		return http.build();
	}

	/**
	 * {bcrypt}... rows are checked with bcrypt; a row without an {id} prefix is a legacy plain-text password
	 * (G13). Nothing is re-hashed or written back: migrating the stored passwords is the owner's call.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		Map<String, PasswordEncoder> encoders = new HashMap<String, PasswordEncoder>();
		encoders.put("bcrypt", new BCryptPasswordEncoder());
		DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt", encoders);
		encoder.setDefaultPasswordEncoderForMatches(new PlainTextPasswordEncoder());
		return encoder;
	}

	/** The only authentication provider; as a bean, Spring Security adds no default one beside it. */
	@Bean
	public DaoAuthenticationProvider authenticationProvider(LoginService loginservice,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(loginservice::loadUserByUsername);
		provider.setPasswordEncoder(passwordEncoder);
		// After the password check, as before: a users row without an admin row can't log in (G26)
		provider.setPostAuthenticationChecks(user -> {
			RmsUserDetails details = (RmsUserDetails) user;
			if (details.getUserinfo() == null) {
				log.warn("User {} has no admin row; login refused", details.getUserid());
				throw new DisabledException("No admin row");
			}
		});
		return provider;
	}

	/**
	 * Back to the login page, as a redirect so a refresh doesn't post the login again (G30). The exception isn't
	 * kept in the session (SimpleUrlAuthenticationFailureHandler would keep it, with the typed password).
	 */
	private static void loginFailed(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failure) throws IOException {
		// The database failing isn't a wrong password; Spring Security has logged it at ERROR
		boolean unavailable = failure instanceof InternalAuthenticationServiceException;
		response.sendRedirect(request.getContextPath() + (unavailable ? "/login?unavailable" : "/login?error"));
	}

	/** HTTP 403, logged. A stale login form, or a form posted after its session expired, goes back to the login page. */
	private static void refuse(HttpServletRequest request, HttpServletResponse response, AccessDeniedException denied)
			throws IOException {
		if (denied instanceof CsrfException) {
			boolean sessionExpired = request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
			// The CSRF token lived in the expired session, or the login form came from before a later login
			if (sessionExpired || "/welcome".equals(pathOf(request))) {
				response.sendRedirect(request.getContextPath() + "/login?expired");
				return;
			}
			log.warn("Missing or invalid CSRF token; {} {} refused", request.getMethod(), pathOf(request));
		} else {
			log.warn("User {} is not an admin; {} refused", currentUserid(), pathOf(request));
		}
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	/** The path inside the app, without the query string, which can carry form data. */
	private static String pathOf(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		return request.getServletPath() + (pathInfo == null ? "" : pathInfo);
	}

	private static String currentUserid() {
		Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext()
				.getAuthentication();
		if (authentication == null) {
			return null;
		}
		if (authentication.getPrincipal() instanceof RmsUserDetails) {
			return ((RmsUserDetails) authentication.getPrincipal()).getUserid();
		}
		return authentication.getName();
	}

	/** Puts the profile in the session for the JSPs (sessionScope.user), then redirects to /home. */
	static class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

		LoginSuccessHandler() {
			super("/home");
			setAlwaysUseDefaultTargetUrl(true);
		}

		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
				Authentication authentication) throws IOException, ServletException {
			RmsUserDetails user = (RmsUserDetails) authentication.getPrincipal();
			request.getSession().setAttribute("user", user.getUserinfo());
			super.onAuthenticationSuccess(request, response, authentication);
		}

	}

}
