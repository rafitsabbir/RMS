package rms.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.NullRequestCache;

import rms.model.Role;
import rms.service.LoginService;
import rms.service.RmsUserDetails;

/**
 * Spring Security 7 (G11, G13, G32, G42):
 * <ul>
 * <li>form login posts to /welcome (login.jsp) and always lands on /home; a failed one goes to /login?error</li>
 * <li>access by role (rms.model.Role), deny by default: a page no rule below names is refused (HTTP 403)</li>
 * <li>every POST needs the CSRF token, which &lt;form:form&gt; adds; logout is POST /logout</li>
 * <li>passwords: {bcrypt} rows, or the legacy plain-text rows compared exactly; RMS writes only {bcrypt}</li>
 * <li>an inactive user (admin.isactive = 0) can't log in, and is signed out on the next request
 * (AccountCheckFilter)</li>
 * <li>5 failed logins for a username from one address in 15 minutes lock it there for 15 minutes
 * (LoginThrottle)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	private static final String SUPER_ADMIN = Role.SUPER_ADMIN.name();
	private static final String HR = Role.HR.name();
	private static final String HIRING_MANAGER = Role.HIRING_MANAGER.name();
	private static final String INTERVIEWER = Role.INTERVIEWER.name();

	/** Users and Roles: Super Admin only. */
	static final String[] USER_ADMIN_PAGES = { "/viewuserlist", "/createuser", "/updateuser", "/saveuser",
			"/deactivateuser", "/reactivateuser", "/resetpassword" };
	/** The permanent delete of a candidate's document files: Super Admin only. */
	static final String[] SUPER_ADMIN_PAGES = { "/purgedocuments" };
	/** Read-only staff pages: Super Admin, HR and Hiring Manager. */
	static final String[] STAFF_READ_PAGES = { "/adminviewmarks", "/viewcandidatelist", "/viewevaluations",
			"/viewjoblist" };
	/**
	 * A candidate's profile and documents: staff, and interviewers for the candidates they are assigned to (checked in
	 * CandidateController and DocumentController, Phase 3).
	 */
	static final String[] CANDIDATE_READ_PAGES = { "/viewcandidate", "/downloaddocument/*" };
	/** An interviewer's own evaluations: Interviewer only (the assignment is checked in EvaluationController). */
	static final String[] INTERVIEWER_PAGES = { "/myevaluations", "/evaluate", "/saveevaluation" };
	/** Candidate, document, assignment and decision changes, jobs and master data: Super Admin and HR. */
	static final String[] HR_PAGES = { "/createcandidate", "/updatecandidate", "/savecandidate",
			"/deletecandidate", "/assigninterviewer", "/unassigninterviewer", "/savedecision", "/uploaddocument",
			"/deletedocument/*", "/createjob", "/updatejob/*", "/savejob",
			"/deletejob/*", "/viewpositionlist", "/createposition", "/updateposition/*", "/saveposition",
			"/deleteposition/*", "/viewlanguagelist", "/createlanguage", "/updatelanguage/*", "/savelanguage",
			"/deletelanguage/*" };

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
	public SecurityFilterChain appFilterChain(HttpSecurity http, LoginThrottle throttle, LoginService loginservice)
			throws Exception {
		http.authorizeHttpRequests(auth -> auth
						// A JSP forward belongs to a request that was already checked
						.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR)
						.permitAll()
						.requestMatchers("/login", "/welcome").permitAll()
						.requestMatchers("/", "/home", "/changepassword", "/savepassword").authenticated()
						.requestMatchers(USER_ADMIN_PAGES).hasRole(SUPER_ADMIN)
						.requestMatchers(SUPER_ADMIN_PAGES).hasRole(SUPER_ADMIN)
						.requestMatchers(STAFF_READ_PAGES).hasAnyRole(SUPER_ADMIN, HR, HIRING_MANAGER)
						.requestMatchers(CANDIDATE_READ_PAGES).hasAnyRole(SUPER_ADMIN, HR, HIRING_MANAGER, INTERVIEWER)
						.requestMatchers(INTERVIEWER_PAGES).hasRole(INTERVIEWER)
						.requestMatchers(HR_PAGES).hasAnyRole(SUPER_ADMIN, HR)
						// Deny by default: a new page needs its own rule above, and cases in SecurityConfigTest
						.anyRequest().denyAll())
				.formLogin(form -> form
						.loginPage("/login")
						.loginProcessingUrl("/welcome")
						.usernameParameter("username")
						.passwordParameter("password")
						.successHandler(new LoginSuccessHandler(throttle))
						.failureHandler(loginFailed(throttle)))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login"))
				// After a login always /home, as before, so the page that led to the login isn't kept
				.requestCache(cache -> cache.requestCache(new NullRequestCache()))
				.exceptionHandling(exceptions -> exceptions.accessDeniedHandler(SecurityConfig::refuse))
				// After the CSRF check, before the password check: a locked login isn't even tried
				.addFilterBefore(lockedLoginFilter(throttle), UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(new AccountCheckFilter(loginservice), AuthorizationFilter.class);
		return http.build();
	}

	@Bean
	public LoginThrottle loginThrottle() {
		return new LoginThrottle();
	}

	/**
	 * {bcrypt}... rows are checked with bcrypt; a row without an {id} prefix is a legacy plain-text password
	 * (G13). New and changed passwords are encoded as {bcrypt} (rms.service.UserServiceImpl). Existing rows
	 * change only through the owner's migration (db/migrations/003-hash-passwords).
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
		// The default checks the account before the password, so an inactive account would answer faster than a
		// wrong password and show that it exists. Every account check runs after the password check instead
		provider.setPreAuthenticationChecks(user -> {
		});
		provider.setPostAuthenticationChecks(user -> {
			RmsUserDetails details = (RmsUserDetails) user;
			// A users row without an admin row can't log in (G26)
			if (details.getUserinfo() == null) {
				log.warn("User {} has no admin row; login refused", details.getUserid());
				throw new DisabledException("No admin row");
			}
			if (!details.isEnabled()) {
				log.info("User {} is inactive; login refused", details.getUserid());
				throw new DisabledException("Inactive");
			}
		});
		return provider;
	}

	/** Refuses a login post while its username is locked for the address, with the usual error. */
	private static Filter lockedLoginFilter(LoginThrottle throttle) {
		return (servletRequest, servletResponse, chain) -> {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			if ("POST".equals(request.getMethod()) && "/welcome".equals(pathOf(request))
					&& throttle.isLocked(request.getParameter("username"), request.getRemoteAddr())) {
				response.sendRedirect(request.getContextPath() + "/login?error");
				return;
			}
			chain.doFilter(servletRequest, servletResponse);
		};
	}

	/**
	 * Back to the login page, as a redirect so a refresh doesn't post the login again (G30). The exception isn't
	 * kept in the session (SimpleUrlAuthenticationFailureHandler would keep it, with the typed password).
	 * Every failure except a database error counts towards the lock.
	 */
	private static AuthenticationFailureHandler loginFailed(LoginThrottle throttle) {
		return (request, response, failure) -> {
			// The database failing isn't a wrong password; Spring Security has logged it at ERROR
			boolean unavailable = failure instanceof InternalAuthenticationServiceException;
			if (!unavailable) {
				throttle.loginFailed(request.getParameter("username"), request.getRemoteAddr());
			}
			response.sendRedirect(request.getContextPath() + (unavailable ? "/login?unavailable" : "/login?error"));
		};
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
			// An upload over the size limit: the container didn't read the form, so its CSRF token is missing too
			String candidateid = tooLargeUpload(request);
			if (candidateid != null) {
				response.sendRedirect(request.getContextPath() + "/viewcandidate?candidateid="
						+ URLEncoder.encode(candidateid, StandardCharsets.UTF_8) + "&toolarge");
				return;
			}
			log.warn("Missing or invalid CSRF token; {} {} refused", request.getMethod(), pathOf(request));
		} else {
			log.warn("User {} may not open {}; refused", currentUserid(), pathOf(request));
		}
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	/**
	 * The candidate ID of a document upload the container refused as too large (WebInitializer's limits), or
	 * null. The ID comes from the form's URL (candidateprofile.jsp), the only part of the request still read.
	 * Only a redirect to the profile follows, which changes nothing.
	 */
	static String tooLargeUpload(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (!"POST".equals(request.getMethod()) || !"/uploaddocument".equals(pathOf(request)) || contentType == null
				|| !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
			return null;
		}
		try {
			request.getParts();
			return null;
		} catch (IllegalStateException e) {
			// Tomcat: the parts couldn't be read because a size limit was exceeded
			for (Throwable cause = e; cause != null; cause = cause.getCause()) {
				String message = cause.getMessage();
				if (message != null && message.toLowerCase(Locale.ROOT).contains("exceed")) {
					return request.getParameter("candidateid");
				}
			}
			return null;
		} catch (IOException | ServletException e) {
			return null;
		}
	}

	/** The path inside the app, without the query string, which can carry form data. */
	static String pathOf(HttpServletRequest request) {
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

	/**
	 * Puts the profile in the session for the JSPs (sessionScope.user), clears the failure count, then redirects
	 * to /home.
	 */
	static class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

		private final LoginThrottle throttle;

		LoginSuccessHandler(LoginThrottle throttle) {
			super("/home");
			setAlwaysUseDefaultTargetUrl(true);
			this.throttle = throttle;
		}

		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
				Authentication authentication) throws IOException, ServletException {
			RmsUserDetails user = (RmsUserDetails) authentication.getPrincipal();
			throttle.loginSucceeded(request.getParameter("username"), request.getRemoteAddr());
			request.getSession().setAttribute("user", user.getUserinfo());
			super.onAuthenticationSuccess(request, response, authentication);
		}

	}

}
