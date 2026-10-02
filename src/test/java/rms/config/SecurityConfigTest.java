package rms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import rms.controller.CandidateController;
import rms.controller.LanguageController;
import rms.controller.LoginController;
import rms.controller.MarksController;
import rms.controller.PositionController;
import rms.dao.LoginDao;
import rms.model.CandidateInfo;
import rms.model.LoginInfo;
import rms.model.UserInfo;
import rms.service.CandidateService;
import rms.service.LanguageService;
import rms.service.LoginServiceImpl;
import rms.service.MarksService;
import rms.service.PositionService;
import rms.service.RmsUserDetails;

/**
 * The real SecurityConfig in front of the real controllers, with mocked DAO and services (G11, G13, G26, G30,
 * G32). Takes over the access matrix of the former AuthInterceptorTest. Uses WebConfig's view resolver and
 * resource handler, and src/main/webapp as the web root (@SpringJUnitWebConfig's default). Spring's test
 * framework caches the context and resets the @MockitoBean mocks after each test.
 */
@SpringJUnitWebConfig(classes = { SecurityConfig.class, SecurityConfigTest.TestWebConfig.class })
class SecurityConfigTest {

	@Configuration
	@EnableWebMvc
	static class TestWebConfig implements WebMvcConfigurer {

		@Bean
		LoginServiceImpl loginservice() {
			return new LoginServiceImpl();
		}

		@Bean
		LoginController logincontroller() {
			return new LoginController();
		}

		@Bean
		PositionController positioncontroller() {
			return new PositionController();
		}

		@Bean
		LanguageController languagecontroller() {
			return new LanguageController();
		}

		@Bean
		MarksController markscontroller() {
			return new MarksController();
		}

		@Bean
		CandidateController candidatecontroller() {
			return new CandidateController();
		}

		@Bean
		InternalResourceViewResolver viewResolver() {
			return new WebConfig().viewResolver();
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			new WebConfig().addResourceHandlers(registry);
		}

	}

	@Autowired
	WebApplicationContext context;

	@MockitoBean
	LoginDao logindao;

	@MockitoBean
	PositionService positionservice;

	@MockitoBean
	LanguageService languageservice;

	@MockitoBean
	MarksService marksservice;

	@MockitoBean
	CandidateService candidateservice;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		givenLogin("U1", "test.admin", "test-only-1", "N");
		givenLogin("U2", "test.interviewer", "test-only-2", "Y");
	}

	// --- Access: who may open what (was AuthInterceptorTest) ---

	@Test
	void loggedOutRequestsRedirectToLogin() throws Exception {
		mockMvc.perform(get("/viewpositionlist")).andExpect(redirectsToLogin());
		mockMvc.perform(post("/saveposition").with(csrf()).param("positionname", "dev ops")).andExpect(redirectsToLogin());
		mockMvc.perform(post("/deleteposition/1").with(csrf())).andExpect(redirectsToLogin());
		mockMvc.perform(get("/adminviewmarks")).andExpect(redirectsToLogin());
		mockMvc.perform(get("/createcandidate")).andExpect(redirectsToLogin());
		mockMvc.perform(get("/viewcandidatelist")).andExpect(redirectsToLogin());
		mockMvc.perform(post("/savecandidate").with(csrf()).param("candidateid", "C3")).andExpect(redirectsToLogin());
		mockMvc.perform(post("/deletecandidate").with(csrf()).param("candidateid", "C1")).andExpect(redirectsToLogin());

		verifyNoInteractions(positionservice, marksservice, candidateservice);
	}

	@Test
	void loggedOutHomeRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/home")).andExpect(redirectsToLogin());
		mockMvc.perform(get("/")).andExpect(redirectsToLogin());
		mockMvc.perform(head("/home")).andExpect(redirectsToLogin());
	}

	@Test
	void adminSeesAdminPages() throws Exception {
		mockMvc.perform(get("/viewpositionlist").with(user(userWithRole("N"))))
				.andExpect(status().isOk())
				.andExpect(view().name("viewposition"));
		mockMvc.perform(get("/adminviewmarks").with(user(userWithRole("n"))))
				.andExpect(status().isOk())
				.andExpect(view().name("viewmarks"));
		mockMvc.perform(get("/viewcandidatelist").with(user(userWithRole("N"))))
				.andExpect(status().isOk())
				.andExpect(view().name("viewcandidate"));
		mockMvc.perform(get("/createcandidate").with(user(userWithRole("N"))))
				.andExpect(status().isOk())
				.andExpect(view().name("createcandidate"));
		when(candidateservice.findCandidateById("C1")).thenReturn(new CandidateInfo());
		mockMvc.perform(get("/updatecandidate").param("candidateid", "C1").with(user(userWithRole("N"))))
				.andExpect(status().isOk())
				.andExpect(view().name("createcandidate"));
	}

	@Test
	void interviewerIsRefusedAdminPages() throws Exception {
		mockMvc.perform(get("/viewpositionlist").with(user(userWithRole("Y")))).andExpect(status().isForbidden());
		mockMvc.perform(post("/deleteposition/1").with(csrf()).with(user(userWithRole("Y"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/adminviewmarks").with(user(userWithRole("Y")))).andExpect(status().isForbidden());
		mockMvc.perform(head("/viewpositionlist").with(user(userWithRole("Y")))).andExpect(status().isForbidden());
		mockMvc.perform(get("/viewcandidatelist").with(user(userWithRole("Y")))).andExpect(status().isForbidden());
		mockMvc.perform(get("/updatecandidate").param("candidateid", "C1").with(user(userWithRole("Y"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/deletecandidate").with(csrf()).with(user(userWithRole("Y"))).param("candidateid", "C1"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/savecandidate").with(csrf()).with(user(userWithRole("Y"))).param("candidateid", "C3"))
				.andExpect(status().isForbidden());

		verifyNoInteractions(positionservice, marksservice, candidateservice);
	}

	@Test
	void userWithoutRoleIsRefusedAdminPages() throws Exception {
		mockMvc.perform(get("/viewpositionlist").with(user(userWithRole(null)))).andExpect(status().isForbidden());
		mockMvc.perform(get("/viewpositionlist").with(user(userWithRole("X")))).andExpect(status().isForbidden());

		verifyNoInteractions(positionservice);
	}

	@Test
	void homeNeedsOnlyALogin() throws Exception {
		for (String role : new String[] { "Y", "N", null }) {
			mockMvc.perform(get("/home").with(user(userWithRole(role))))
					.andExpect(status().isOk())
					.andExpect(view().name("main"));
		}
		mockMvc.perform(get("/").with(user(userWithRole("Y")))).andExpect(redirectedUrl("/home"));
	}

	@Test
	void loginPageIsPublicAndCarriesACsrfToken() throws Exception {
		// form:form reads this request attribute to add the hidden _csrf field
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(request().attribute("_csrf", notNullValue()));
	}

	@Test
	void urlsWithPathParametersAreRejected() throws Exception {
		// Spring Security's firewall refuses ";" in URLs. Sessions are cookie-only since G41, so the app makes none
		mockMvc.perform(get("/login;jsessionid=X")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/viewpositionlist;jsessionid=X").with(user(userWithRole("N"))))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(positionservice);
	}

	@Test
	void trailingSlashIsNotAPage() throws Exception {
		// Spring 6+ doesn't map "/x/" to "/x": 404 for an admin, a login redirect when logged out
		mockMvc.perform(get("/viewpositionlist/").with(user(userWithRole("N")))).andExpect(status().isNotFound());
		mockMvc.perform(get("/viewpositionlist/")).andExpect(redirectsToLogin());

		verifyNoInteractions(positionservice);
	}

	@Test
	void pagesBehindTheLoginAreNotCached() throws Exception {
		mockMvc.perform(get("/home").with(user(userWithRole("Y"))))
				.andExpect(header().string("Cache-Control", containsString("no-store")));
		mockMvc.perform(get("/viewpositionlist").with(user(userWithRole("N"))))
				.andExpect(header().string("Cache-Control", containsString("no-store")));
	}

	@Test
	void staticResourcesArePublicAndCacheable() throws Exception {
		mockMvc.perform(get("/resources/css/rms.css"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Cache-Control"));
		mockMvc.perform(get("/resources/img/logo.svg")).andExpect(status().isOk());
	}

	// --- Login (POST /welcome) ---

	@Test
	void validLoginRedirectsToHomeWithProfileInANewSession() throws Exception {
		MockHttpSession session = new MockHttpSession(null, "before-login");

		MvcResult result = mockMvc.perform(login("test.admin", "test-only-1").session(session))
				.andExpect(redirectedUrl("/home"))
				.andExpect(authenticated().withRoles("ADMIN"))
				.andReturn();

		// The session id changes at login, and the JSPs find the profile under "user"
		MockHttpSession after = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(after.getId()).isNotEqualTo("before-login");
		assertThat(((UserInfo) after.getAttribute("user")).getUserid()).isEqualTo("U1");
	}

	@Test
	void interviewerLogsInWithInterviewerRole() throws Exception {
		mockMvc.perform(login("test.interviewer", "test-only-2"))
				.andExpect(redirectedUrl("/home"))
				.andExpect(authenticated().withRoles("INTERVIEWER"));
	}

	@Test
	void passwordIsNotKeptAfterLogin() throws Exception {
		MvcResult result = mockMvc.perform(login("test.admin", "test-only-1")).andReturn();

		SecurityContext stored = (SecurityContext) result.getRequest().getSession(false)
				.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		Authentication authentication = stored.getAuthentication();
		assertThat(authentication.getCredentials()).isNull();
		assertThat(((RmsUserDetails) authentication.getPrincipal()).getPassword()).isNull();
	}

	@Test
	void wrongPasswordRedirectsToLoginWithError() throws Exception {
		// G30: a redirect, so a refresh doesn't post the credentials again
		mockMvc.perform(login("test.admin", "wrong"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
	}

	@Test
	void passwordMustMatchExactly() throws Exception {
		// Was compared in SQL, where the column's collation decides (case-insensitive on the MySQL defaults)
		mockMvc.perform(login("test.admin", "TEST-ONLY-1")).andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(login("test.admin", "test-only-1 ")).andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void unknownUserIsRefused() throws Exception {
		mockMvc.perform(login("nobody", "test-only-1"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
	}

	@Test
	void userWithoutAdminRowIsRefused() throws Exception {
		// G26: the users row exists and the password is right, but there's no admin row
		when(logindao.findLogins("orphan")).thenReturn(List.of(new LoginInfo("U9", "orphan", "test-only-9")));
		when(logindao.getUserInfo("U9")).thenReturn(null);

		mockMvc.perform(login("orphan", "test-only-9"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
	}

	@Test
	void ambiguousUsernameIsRefused() throws Exception {
		when(logindao.findLogins("twin")).thenReturn(
				List.of(new LoginInfo("U7", "twin", "test-only-7"), new LoginInfo("U8", "twin", "test-only-8")));

		mockMvc.perform(login("twin", "test-only-7")).andExpect(redirectedUrl("/login?error"));

		verify(logindao, never()).getUserInfo(anyString());
	}

	@Test
	void bcryptPasswordRowLogsIn() throws Exception {
		// Ready for the owner's migration (G13): {bcrypt} rows work beside the plain-text ones
		String hash = "{bcrypt}" + new BCryptPasswordEncoder().encode("test-only-3");
		when(logindao.findLogins("hashed")).thenReturn(List.of(new LoginInfo("U3", "hashed", hash)));
		when(logindao.getUserInfo("U3")).thenReturn(profile("U3", "N"));

		mockMvc.perform(login("hashed", "test-only-3"))
				.andExpect(redirectedUrl("/home"))
				.andExpect(authenticated());
		mockMvc.perform(login("hashed", hash)).andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void failedLoginKeepsNothingInTheSession() throws Exception {
		// Spring's default failure handler would keep the exception, and with it the typed password
		MockHttpSession session = new MockHttpSession();

		mockMvc.perform(login("test.admin", "wrong").session(session)).andExpect(redirectedUrl("/login?error"));

		assertThat(session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)).isNull();
	}

	@Test
	void databaseErrorAtLoginSaysUnavailable() throws Exception {
		when(logindao.findLogins("test.admin")).thenThrow(new DataAccessResourceFailureException("database down"));

		mockMvc.perform(login("test.admin", "test-only-1"))
				.andExpect(redirectedUrl("/login?unavailable"))
				.andExpect(unauthenticated());
	}

	@Test
	void longPasswordIsAWrongPasswordNotAnError() throws Exception {
		// bcrypt reads at most 72 bytes; a longer password must still just fail
		String longPassword = "x".repeat(100);
		String hash = "{bcrypt}" + new BCryptPasswordEncoder().encode("test-only-3");
		when(logindao.findLogins("hashed")).thenReturn(List.of(new LoginInfo("U3", "hashed", hash)));
		when(logindao.getUserInfo("U3")).thenReturn(profile("U3", "N"));

		mockMvc.perform(login("nobody", longPassword)).andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(login("test.admin", longPassword)).andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(login("hashed", longPassword)).andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void securityConfigHasTheOnlyAuthenticationProvider() {
		assertThat(context.getBeansOfType(AuthenticationProvider.class)).hasSize(1);

		// The production scan of the service and DAO packages: a UserDetailsService bean there would make
		// Spring Security consider a default provider of its own, without the admin-row check (G26)
		try (AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext()) {
			app.registerBean(NamedParameterJdbcTemplate.class, () -> mock(NamedParameterJdbcTemplate.class));
			app.scan("rms.service", "rms.dao");
			app.refresh();

			assertThat(app.getBeansOfType(UserDetailsService.class)).isEmpty();
			assertThat(app.getBeansOfType(AuthenticationProvider.class)).isEmpty();
		}
	}

	@Test
	void loginPageKeepsTheSession() throws Exception {
		// G32: a GET from another site can no longer log the user out
		MockHttpSession session = loggedInSession();

		mockMvc.perform(get("/login").session(session)).andExpect(status().isOk());

		assertThat(session.isInvalid()).isFalse();
		mockMvc.perform(get("/home").session(session)).andExpect(status().isOk());
	}

	// --- CSRF (G32) and logout ---

	@Test
	void postWithoutCsrfTokenIsRefused() throws Exception {
		mockMvc.perform(post("/deleteposition/1").with(user(userWithRole("N")))).andExpect(status().isForbidden());
		mockMvc.perform(post("/saveposition").param("positionname", "dev ops").with(user(userWithRole("N"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/deleteposition/1").with(csrf().useInvalidToken()).with(user(userWithRole("N"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/deletecandidate").param("candidateid", "C1").with(user(userWithRole("N"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/savecandidate").param("candidateid", "C3").with(user(userWithRole("N"))))
				.andExpect(status().isForbidden());

		verifyNoInteractions(positionservice, candidateservice);
	}

	@Test
	void loginWithoutValidCsrfTokenGoesBackToLogin() throws Exception {
		// For example a login page left open in a second tab after logging in again elsewhere
		mockMvc.perform(post("/welcome").param("username", "test.admin").param("password", "test-only-1"))
				.andExpect(redirectedUrl("/login?expired"))
				.andExpect(unauthenticated());
		mockMvc.perform(post("/welcome").session(new MockHttpSession()).with(csrf().useInvalidToken())
				.param("username", "test.admin").param("password", "test-only-1"))
				.andExpect(redirectedUrl("/login?expired"))
				.andExpect(unauthenticated());
	}

	@Test
	void loggedInPostWithoutTokenIsRefused() throws Exception {
		MockHttpSession session = loggedInSession();

		mockMvc.perform(post("/deleteposition/1").session(session)).andExpect(status().isForbidden());

		verifyNoInteractions(positionservice);
	}

	@Test
	void formPostedAfterTheSessionExpiredGoesBackToLogin() throws Exception {
		// The login page was left open until the session (and with it the CSRF token) expired
		mockMvc.perform(post("/welcome").param("username", "test.admin").param("password", "test-only-1")
				.with(request -> {
					request.setRequestedSessionId("expired-session");
					request.setRequestedSessionIdValid(false);
					return request;
				}))
				.andExpect(redirectedUrl("/login?expired"))
				.andExpect(unauthenticated());
	}

	@Test
	void logoutIsAPostWithToken() throws Exception {
		MockHttpSession session = loggedInSession();

		// GET /logout is no page (404) and doesn't log out
		mockMvc.perform(get("/logout").session(session)).andExpect(status().isNotFound());
		mockMvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
		mockMvc.perform(get("/home").session(session)).andExpect(status().isOk());

		mockMvc.perform(post("/logout").session(session).with(csrf()))
				.andExpect(redirectedUrl("/login"))
				.andExpect(unauthenticated());
		assertThat(session.isInvalid()).isTrue();
	}

	// --- Passwords (G13) ---

	@Test
	void passwordEncoderComparesPlainTextRowsExactlyAndChecksBcryptRows() {
		PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

		assertThat(encoder.matches("test-only-1", "test-only-1")).isTrue();
		assertThat(encoder.matches("Test-only-1", "test-only-1")).isFalse();
		assertThat(encoder.matches("café", "café")).isTrue();
		// An empty password never matches, not even an empty stored one
		assertThat(encoder.matches("", "")).isFalse();
		assertThat(encoder.matches("x", null)).isFalse();

		// New hashes (not stored by RMS today) are bcrypt
		String hash = encoder.encode("test-only-3");
		assertThat(hash).startsWith("{bcrypt}$2");
		assertThat(encoder.matches("test-only-3", hash)).isTrue();
	}

	// --- helpers ---

	private void givenLogin(String userid, String username, String password, String isinterviewer) {
		when(logindao.findLogins(username)).thenReturn(List.of(new LoginInfo(userid, username, password)));
		when(logindao.getUserInfo(userid)).thenReturn(profile(userid, isinterviewer));
	}

	private MockHttpSession loggedInSession() throws Exception {
		MvcResult result = mockMvc.perform(login("test.admin", "test-only-1")).andExpect(authenticated()).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private static MockHttpServletRequestBuilder login(String username, String password) {
		return post("/welcome").with(csrf()).param("username", username).param("password", password);
	}

	private static UserInfo profile(String userid, String isinterviewer) {
		UserInfo user = new UserInfo();
		user.setUserid(userid);
		user.setIsinterviewer(isinterviewer);
		return user;
	}

	private static RmsUserDetails userWithRole(String isinterviewer) {
		return new RmsUserDetails(new LoginInfo("U1", "someone", "test-only"), profile("U1", isinterviewer));
	}

	/** A relative redirect, as the interceptor sent (Spring Security 7's entry point favours relative URIs). */
	private static ResultMatcher redirectsToLogin() {
		return result -> {
			assertThat(result.getResponse().getStatus()).isEqualTo(302);
			assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/login");
		};
	}

}
