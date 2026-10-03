package rms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
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
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import rms.controller.AccountController;
import rms.controller.ActivityController;
import rms.controller.AssignmentController;
import rms.controller.CandidateController;
import rms.controller.DocumentController;
import rms.controller.EvaluationController;
import rms.controller.JobController;
import rms.controller.LanguageController;
import rms.controller.LoginController;
import rms.controller.MarksController;
import rms.controller.PositionController;
import rms.controller.ReportController;
import rms.controller.ScheduleController;
import rms.controller.UserController;
import rms.dao.LoginDao;
import rms.model.CandidateInfo;
import rms.model.DashboardInfo;
import rms.model.DocumentInfo;
import rms.model.LoginInfo;
import rms.model.Role;
import rms.model.UserInfo;
import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.DashboardService;
import rms.service.DecisionService;
import rms.service.DocumentService;
import rms.service.JobService;
import rms.service.LanguageService;
import rms.service.LoginServiceImpl;
import rms.service.MarksService;
import rms.service.PositionService;
import rms.service.ActivityService;
import rms.service.ScheduleService;
import rms.service.RmsUserDetails;
import rms.service.UserService;

/**
 * The real SecurityConfig in front of the real controllers, with mocked DAO and services (G11, G13, G26, G30,
 * G32, G42, roles). Uses WebConfig's view resolver and resource handler, and src/main/webapp as the web root
 * (@SpringJUnitWebConfig's default). Spring's test framework caches the context and resets the @MockitoBean
 * mocks after each test; the login lock counts are cleared before each.
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
		JobController jobcontroller() {
			return new JobController();
		}

		@Bean
		DocumentController documentcontroller() {
			return new DocumentController();
		}

		@Bean
		EvaluationController evaluationcontroller() {
			return new EvaluationController();
		}

		@Bean
		AssignmentController assignmentcontroller() {
			return new AssignmentController();
		}

		@Bean
		ScheduleController schedulecontroller() {
			return new ScheduleController();
		}

		@Bean
		ReportController reportcontroller() {
			return new ReportController();
		}

		@Bean
		ActivityController activitycontroller() {
			return new ActivityController();
		}

		@Bean
		UserController usercontroller() {
			return new UserController();
		}

		@Bean
		AccountController accountcontroller() {
			return new AccountController();
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

	/** The seed's users (db/test-seed.sql): one per role, plus U6 without a role. */
	private static final String SUPER_ADMIN_ID = "U1";
	private static final String INTERVIEWER_ID = "U2";
	private static final String HR_ID = "U3";
	private static final String HIRING_MANAGER_ID = "U4";
	private static final String NO_ROLE_ID = "U6";

	@Autowired
	WebApplicationContext context;

	@Autowired
	LoginThrottle throttle;

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

	@MockitoBean
	JobService jobservice;

	@MockitoBean
	DocumentService documentservice;

	@MockitoBean
	AssignmentService assignmentservice;

	@MockitoBean
	DecisionService decisionservice;

	@MockitoBean
	ScheduleService scheduleservice;

	@MockitoBean
	ActivityService activityservice;

	@MockitoBean
	DashboardService dashboardservice;

	@MockitoBean
	UserService userservice;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		throttle.clear();
		givenLogin(SUPER_ADMIN_ID, "test.admin", "test-only-1", Role.SUPER_ADMIN);
		givenLogin(INTERVIEWER_ID, "test.interviewer", "test-only-2", Role.INTERVIEWER);
		givenLogin(HR_ID, "test.hr", "{bcrypt}" + new BCryptPasswordEncoder().encode("test-only-3"), Role.HR);
		givenLogin(HIRING_MANAGER_ID, "test.manager", "test-only-4", Role.HIRING_MANAGER);
		givenLogin(NO_ROLE_ID, "test.norole", "test-only-6", null);
	}

	// --- Access: who may open what ---

	/** One request per page, with the roles allowed to open it; everyone else gets HTTP 403. */
	private record Page(AbstractMockHttpServletRequestBuilder<?> request, Set<Role> allowed) {
	}

	private static final Set<Role> ALL = EnumSet.allOf(Role.class);
	private static final Set<Role> STAFF = EnumSet.of(Role.SUPER_ADMIN, Role.HR, Role.HIRING_MANAGER);
	private static final Set<Role> HR_AND_UP = EnumSet.of(Role.SUPER_ADMIN, Role.HR);
	private static final Set<Role> SUPER_ADMIN_ONLY = EnumSet.of(Role.SUPER_ADMIN);
	private static final Set<Role> NOBODY = EnumSet.noneOf(Role.class);
	/** Every role, but not a user without one (a separate set from ALL, which userWithoutRole... compares by identity). */
	private static final Set<Role> EVERY_ROLE = EnumSet.allOf(Role.class);
	private static final Set<Role> INTERVIEWER_ONLY = EnumSet.of(Role.INTERVIEWER);

	private static List<Page> pages() {
		return List.of(
				new Page(get("/home"), ALL),
				new Page(get("/changepassword"), ALL),
				new Page(post("/savepassword").with(csrf()), ALL),
				new Page(get("/adminviewmarks"), STAFF),
				new Page(get("/viewcandidatelist"), STAFF),
				new Page(get("/createcandidate"), HR_AND_UP),
				new Page(get("/updatecandidate").param("candidateid", "C1"), HR_AND_UP),
				new Page(post("/savecandidate").with(csrf()), HR_AND_UP),
				new Page(post("/deletecandidate").with(csrf()).param("candidateid", "C1"), HR_AND_UP),
				new Page(get("/viewcandidate").param("candidateid", "C1"), EVERY_ROLE),
				new Page(get("/viewjoblist"), STAFF),
				new Page(get("/createjob"), HR_AND_UP),
				new Page(get("/updatejob/1"), HR_AND_UP),
				new Page(post("/savejob").with(csrf()), HR_AND_UP),
				new Page(post("/deletejob/1").with(csrf()), HR_AND_UP),
				new Page(get("/viewschedulelist"), STAFF),
				new Page(get("/createschedule"), HR_AND_UP),
				new Page(get("/updateschedule/1"), HR_AND_UP),
				new Page(post("/saveschedule").with(csrf()), HR_AND_UP),
				new Page(post("/cancelschedule/1").with(csrf()), HR_AND_UP),
				new Page(get("/myschedule"), INTERVIEWER_ONLY),
				new Page(get("/reports"), STAFF),
				new Page(get("/exportcandidates"), STAFF),
				new Page(get("/exportresults"), STAFF),
				new Page(get("/exportschedule"), STAFF),
				new Page(get("/viewactivity"), SUPER_ADMIN_ONLY),
				new Page(get("/exportactivity"), SUPER_ADMIN_ONLY),
				new Page(multipart("/uploaddocument").file(new MockMultipartFile("file", "cv.pdf", "application/pdf",
						"%PDF-1".getBytes())).with(csrf()).param("candidateid", "C1").param("doctype", "CV"), HR_AND_UP),
				new Page(get("/downloaddocument/1"), EVERY_ROLE),
				new Page(get("/viewevaluations").param("candidateid", "C1"), STAFF),
				new Page(post("/assigninterviewer").with(csrf()).param("candidateid", "C1"), HR_AND_UP),
				new Page(post("/unassigninterviewer").with(csrf()).param("candidateid", "C1"), HR_AND_UP),
				new Page(post("/savedecision").with(csrf()), HR_AND_UP),
				new Page(get("/myevaluations"), INTERVIEWER_ONLY),
				new Page(get("/evaluate").param("candidateid", "C1"), INTERVIEWER_ONLY),
				new Page(post("/saveevaluation").with(csrf()), INTERVIEWER_ONLY),
				new Page(post("/deletedocument/1").with(csrf()), HR_AND_UP),
				new Page(post("/purgedocuments").with(csrf()).param("candidateid", "C1").param("reason", "x"),
						SUPER_ADMIN_ONLY),
				new Page(get("/viewpositionlist"), HR_AND_UP),
				new Page(get("/createposition"), HR_AND_UP),
				new Page(get("/updateposition/1"), HR_AND_UP),
				new Page(post("/saveposition").with(csrf()), HR_AND_UP),
				new Page(post("/deleteposition/1").with(csrf()), HR_AND_UP),
				new Page(get("/viewlanguagelist"), HR_AND_UP),
				new Page(get("/createlanguage"), HR_AND_UP),
				new Page(get("/updatelanguage/1"), HR_AND_UP),
				new Page(post("/savelanguage").with(csrf()), HR_AND_UP),
				new Page(post("/deletelanguage/1").with(csrf()), HR_AND_UP),
				new Page(get("/viewuserlist"), SUPER_ADMIN_ONLY),
				new Page(get("/createuser"), SUPER_ADMIN_ONLY),
				new Page(get("/updateuser").param("userid", "U9"), SUPER_ADMIN_ONLY),
				new Page(post("/saveuser").with(csrf()), SUPER_ADMIN_ONLY),
				new Page(post("/deactivateuser").with(csrf()).param("userid", "U9"), SUPER_ADMIN_ONLY),
				new Page(post("/reactivateuser").with(csrf()).param("userid", "U9"), SUPER_ADMIN_ONLY),
				new Page(post("/resetpassword").with(csrf()).param("userid", "U9"), SUPER_ADMIN_ONLY),
				// Deny by default: a page no rule names, even one that doesn't exist
				new Page(get("/nosuchpage"), NOBODY),
				new Page(get("/viewpositionlist/"), NOBODY));
	}

	@Test
	void eachRoleOpensExactlyItsPages() throws Exception {
		for (Role role : Role.values()) {
			for (Page page : pages()) {
				int status = mockMvc.perform(page.request().with(as(role))).andReturn().getResponse().getStatus();
				String where = role + " " + page.request().buildRequest(context.getServletContext()).getRequestURI();
				if (page.allowed().contains(role)) {
					// Past the security filters: the controller answers (200, a redirect, or 404 for a mocked-out row)
					assertThat(status).as(where).isNotEqualTo(403);
				} else {
					assertThat(status).as(where).isEqualTo(403);
				}
			}
		}
	}

	@Test
	void userWithoutRoleOpensOnlyHomeAndChangePassword() throws Exception {
		for (Page page : pages()) {
			int status = mockMvc.perform(page.request().with(asUser(NO_ROLE_ID, null))).andReturn().getResponse()
					.getStatus();
			String where = page.request().buildRequest(context.getServletContext()).getRequestURI();
			if (page.allowed() == ALL) {
				assertThat(status).as(where).isNotEqualTo(403);
			} else {
				assertThat(status).as(where).isEqualTo(403);
			}
		}
	}

	@Test
	void loggedOutRequestsRedirectToLogin() throws Exception {
		for (Page page : pages()) {
			mockMvc.perform(page.request()).andExpect(redirectsToLogin());
		}
		mockMvc.perform(get("/")).andExpect(redirectsToLogin());
		mockMvc.perform(head("/home")).andExpect(redirectsToLogin());

		verifyNoInteractions(positionservice, marksservice, candidateservice, userservice, jobservice, documentservice,
				assignmentservice, decisionservice, scheduleservice, dashboardservice);
	}

	@Test
	void refusedRequestsDontReachTheServices() throws Exception {
		mockMvc.perform(get("/viewpositionlist").with(as(Role.INTERVIEWER))).andExpect(status().isForbidden());
		mockMvc.perform(post("/deletecandidate").with(csrf()).with(as(Role.HIRING_MANAGER)).param("candidateid", "C1"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/deactivateuser").with(csrf()).with(as(Role.HR)).param("userid", "U1"))
				.andExpect(status().isForbidden());
		mockMvc.perform(head("/viewpositionlist").with(as(Role.INTERVIEWER))).andExpect(status().isForbidden());

		verifyNoInteractions(positionservice, candidateservice, userservice);
	}

	@Test
	void staffPagesRender() throws Exception {
		mockMvc.perform(get("/viewpositionlist").with(as(Role.SUPER_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(view().name("viewposition"));
		mockMvc.perform(get("/adminviewmarks").with(as(Role.HIRING_MANAGER)))
				.andExpect(status().isOk())
				.andExpect(view().name("viewmarks"));
		mockMvc.perform(get("/viewcandidatelist").with(as(Role.HR)))
				.andExpect(status().isOk())
				.andExpect(view().name("viewcandidate"));
		when(candidateservice.findCandidateById("C1")).thenReturn(new CandidateInfo());
		mockMvc.perform(get("/updatecandidate").param("candidateid", "C1").with(as(Role.HR)))
				.andExpect(status().isOk())
				.andExpect(view().name("createcandidate"));
		mockMvc.perform(get("/viewuserlist").with(as(Role.SUPER_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(view().name("viewuser"));
		mockMvc.perform(get("/changepassword").with(as(Role.INTERVIEWER)))
				.andExpect(status().isOk())
				.andExpect(view().name("changepassword"));
	}

	@Test
	void homeNeedsOnlyALogin() throws Exception {
		for (Role role : Role.values()) {
			mockMvc.perform(get("/home").with(as(role)))
					.andExpect(status().isOk())
					.andExpect(view().name("main"));
		}
		mockMvc.perform(get("/home").with(asUser(NO_ROLE_ID, null))).andExpect(status().isOk());
		mockMvc.perform(get("/").with(as(Role.INTERVIEWER))).andExpect(redirectedUrl("/home"));
	}

	@Test
	void schedulePagesRenderForTheirRoles() throws Exception {
		mockMvc.perform(get("/viewschedulelist").with(as(Role.HIRING_MANAGER)))
				.andExpect(status().isOk())
				.andExpect(view().name("viewschedule"));
		mockMvc.perform(get("/createschedule").with(as(Role.HR)))
				.andExpect(status().isOk())
				.andExpect(view().name("createschedule"));
		// The interviewer's schedule is the session user's: there is no way to ask for another's
		mockMvc.perform(get("/myschedule").param("interviewerid", "U9").with(as(Role.INTERVIEWER)))
				.andExpect(status().isOk())
				.andExpect(view().name("myschedule"));
		verify(scheduleservice).getScheduleOf(INTERVIEWER_ID);
		verify(scheduleservice, never()).getScheduleOf("U9");
	}

	@Test
	void homeGetsTheDashboardOfTheUsersRole() throws Exception {
		DashboardInfo dashboard = new DashboardInfo();
		when(dashboardservice.getDashboard(Role.HR, HR_ID)).thenReturn(dashboard);

		mockMvc.perform(get("/home").with(as(Role.HR)))
				.andExpect(status().isOk())
				.andExpect(view().name("main"))
				.andExpect(model().attribute("dashboard", dashboard));
		// A user without a role gets none
		mockMvc.perform(get("/home").with(asUser(NO_ROLE_ID, null)))
				.andExpect(status().isOk())
				.andExpect(model().attributeDoesNotExist("dashboard"));
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
		mockMvc.perform(get("/viewpositionlist;jsessionid=X").with(as(Role.SUPER_ADMIN)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(positionservice);
	}

	@Test
	void trailingSlashIsRefused() throws Exception {
		// Spring 6+ doesn't map "/x/" to "/x", and no access rule names it: 403 (deny by default), or a login redirect
		mockMvc.perform(get("/viewpositionlist/").with(as(Role.SUPER_ADMIN))).andExpect(status().isForbidden());
		mockMvc.perform(get("/viewpositionlist/")).andExpect(redirectsToLogin());

		verifyNoInteractions(positionservice);
	}

	@Test
	void pagesBehindTheLoginAreNotCached() throws Exception {
		mockMvc.perform(get("/home").with(as(Role.INTERVIEWER)))
				.andExpect(header().string("Cache-Control", containsString("no-store")));
		mockMvc.perform(get("/viewpositionlist").with(as(Role.SUPER_ADMIN)))
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
				.andExpect(authenticated().withRoles("SUPER_ADMIN"))
				.andReturn();

		// The session id changes at login, and the JSPs find the profile under "user"
		MockHttpSession after = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(after.getId()).isNotEqualTo("before-login");
		assertThat(((UserInfo) after.getAttribute("user")).getUserid()).isEqualTo("U1");
	}

	@Test
	void eachRoleLogsInWithItsAuthority() throws Exception {
		mockMvc.perform(login("test.interviewer", "test-only-2")).andExpect(authenticated().withRoles("INTERVIEWER"));
		// A {bcrypt} row
		mockMvc.perform(login("test.hr", "test-only-3")).andExpect(authenticated().withRoles("HR"));
		mockMvc.perform(login("test.manager", "test-only-4")).andExpect(authenticated().withRoles("HIRING_MANAGER"));
		// No role: logged in (Home and Change password), with no ROLE_ authority. Spring Security 7 adds FACTOR_PASSWORD
		mockMvc.perform(login("test.norole", "test-only-6")).andExpect(authenticated().withAuthentication(
				authentication -> assertThat(authentication.getAuthorities())
						.noneMatch(authority -> authority.getAuthority().startsWith("ROLE_"))));
	}

	@Test
	void inactiveUserCantLogIn() throws Exception {
		UserInfo inactive = profile(HR_ID, Role.HR);
		inactive.setIsactive(0);
		when(logindao.getUserInfo(HR_ID)).thenReturn(inactive);

		// The same answer as a wrong password, so it doesn't show that the account exists
		mockMvc.perform(login("test.hr", "test-only-3"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
		mockMvc.perform(login("test.hr", "wrong"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
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
		// {bcrypt} rows work beside the plain-text ones (G13); new and reset passwords are stored this way
		String hash = "{bcrypt}" + new BCryptPasswordEncoder().encode("test-only-3");
		when(logindao.findLogins("hashed")).thenReturn(List.of(new LoginInfo("U7", "hashed", hash)));
		when(logindao.getUserInfo("U7")).thenReturn(profile("U7", Role.SUPER_ADMIN));

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
		when(logindao.findLogins("hashed")).thenReturn(List.of(new LoginInfo("U7", "hashed", hash)));
		when(logindao.getUserInfo("U7")).thenReturn(profile("U7", Role.SUPER_ADMIN));

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
			app.registerBean(PasswordEncoder.class, () -> new SecurityConfig().passwordEncoder());
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

	// --- Documents (Phase 2) ---

	/** A multipart POST to /uploaddocument whose parts the container refused, as Tomcat does over its limits. */
	static class TooLargeUpload extends MockHttpServletRequest {

		private final String message;

		TooLargeUpload(jakarta.servlet.ServletContext context, String message) {
			super(context, "POST", "/uploaddocument");
			setServletPath("/uploaddocument");
			setContentType("multipart/form-data; boundary=x");
			// Only the query string is still readable
			setQueryString("candidateid=C1%2B2");
			addParameter("candidateid", "C1+2");
			this.message = message;
		}

		@Override
		public java.util.Collection<jakarta.servlet.http.Part> getParts() {
			throw new IllegalStateException(new Exception(message));
		}
	}

	@Test
	void uploadOverTheSizeLimitReturnsToTheProfile() throws Exception {
		// No CSRF token: the container didn't read the body. The ID is encoded again for the redirect
		mockMvc.perform(post("/uploaddocument").with(as(Role.HR)).with(request -> new TooLargeUpload(
				request.getServletContext(), "the request was rejected because its size (9000000) exceeds the "
						+ "configured maximum (6291456)")))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1%2B2&toolarge"));

		verifyNoInteractions(documentservice);
	}

	@Test
	void tooLargeUploadIsOnlyTheSizeFailureOfAnUploadPost() throws Exception {
		jakarta.servlet.ServletContext servletContext = context.getServletContext();
		assertThat(SecurityConfig.tooLargeUpload(new TooLargeUpload(servletContext,
				"The field file exceeds its maximum permitted size of 5242880 bytes."))).isEqualTo("C1+2");
		// Another parse failure, a working upload, another path, another method
		assertThat(SecurityConfig.tooLargeUpload(new TooLargeUpload(servletContext, "Stream ended unexpectedly")))
				.isNull();
		MockHttpServletRequest readable = new MockHttpServletRequest(servletContext, "POST", "/uploaddocument");
		readable.setServletPath("/uploaddocument");
		readable.setContentType("multipart/form-data; boundary=x");
		readable.addParameter("candidateid", "C1");
		assertThat(SecurityConfig.tooLargeUpload(readable)).isNull();
		TooLargeUpload otherPath = new TooLargeUpload(servletContext, "size exceeds the configured maximum");
		otherPath.setServletPath("/savecandidate");
		assertThat(SecurityConfig.tooLargeUpload(otherPath)).isNull();
		TooLargeUpload get = new TooLargeUpload(servletContext, "size exceeds the configured maximum");
		get.setMethod("GET");
		assertThat(SecurityConfig.tooLargeUpload(get)).isNull();
	}

	@Test
	void uploadWithoutCsrfTokenIsStillRefused() throws Exception {
		mockMvc.perform(multipart("/uploaddocument").file(new MockMultipartFile("file", "cv.pdf", "application/pdf",
				"%PDF-1".getBytes())).with(as(Role.HR)).param("candidateid", "C1").param("doctype", "CV"))
				.andExpect(status().isForbidden());

		verifyNoInteractions(documentservice);
	}

	@Test
	void hiringManagerAndInterviewerCantChangeDocuments() throws Exception {
		mockMvc.perform(multipart("/uploaddocument").file(new MockMultipartFile("file", "cv.pdf", "application/pdf",
				"%PDF-1".getBytes())).with(csrf()).with(as(Role.HIRING_MANAGER)).param("candidateid", "C1"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/deletedocument/1").with(csrf()).with(as(Role.HIRING_MANAGER)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/purgedocuments").with(csrf()).with(as(Role.HR)).param("candidateid", "C1")
				.param("reason", "x")).andExpect(status().isForbidden());

		verifyNoInteractions(documentservice, jobservice);
	}

	@Test
	void interviewersReachOnlyTheirAssignedCandidates() throws Exception {
		// Phase 3: the URL rule lets interviewers in; the controllers check the assignment
		CandidateInfo carla = new CandidateInfo();
		carla.setCandidateid("C1");
		when(candidateservice.findCandidateById("C1")).thenReturn(carla);
		DocumentInfo cv = new DocumentInfo();
		cv.setDocumentkey(1);
		cv.setCandidateid("C1");
		when(documentservice.findDocument(1)).thenReturn(cv);

		when(assignmentservice.isAssigned("C1", INTERVIEWER_ID)).thenReturn(false);
		mockMvc.perform(get("/viewcandidate").param("candidateid", "C1").with(as(Role.INTERVIEWER)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/downloaddocument/1").with(as(Role.INTERVIEWER))).andExpect(status().isForbidden());

		when(assignmentservice.isAssigned("C1", INTERVIEWER_ID)).thenReturn(true);
		mockMvc.perform(get("/viewcandidate").param("candidateid", "C1").with(as(Role.INTERVIEWER)))
				.andExpect(status().isOk());
		verify(documentservice, never()).findFile(any());

		// The staff-only evaluation and decision pages stay closed to them
		mockMvc.perform(get("/viewevaluations").param("candidateid", "C1").with(as(Role.INTERVIEWER)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/savedecision").with(csrf()).with(as(Role.INTERVIEWER)).param("candidateid", "C1"))
				.andExpect(status().isForbidden());
		// And staff can't evaluate
		mockMvc.perform(get("/evaluate").param("candidateid", "C1").with(as(Role.HR))).andExpect(status().isForbidden());
	}

	// --- Login lock (G42) ---

	@Test
	void fiveFailuresLockTheUsernameForThatAddress() throws Exception {
		for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
			mockMvc.perform(login("test.admin", "wrong")).andExpect(redirectedUrl("/login?error"));
		}

		// Now even the right password is refused there, with the usual message and no password check
		mockMvc.perform(login("test.admin", "test-only-1"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
		// Any case of the username counts as the same one (MySQL's default collation finds the row either way)
		when(logindao.findLogins("TEST.ADMIN")).thenReturn(List.of(new LoginInfo("U1", "test.admin", "test-only-1")));
		mockMvc.perform(login("TEST.ADMIN", "test-only-1")).andExpect(unauthenticated());
		// So does an accented spelling, which an accent-insensitive collation also finds
		String accented = "t" + (char) 0xE9 + "st.admin";
		when(logindao.findLogins(accented)).thenReturn(List.of(new LoginInfo("U1", "test.admin", "test-only-1")));
		mockMvc.perform(login(accented, "test-only-1")).andExpect(unauthenticated());
		// From another address, and for another username, logins still work
		mockMvc.perform(login("test.admin", "test-only-1").with(from("10.0.0.2"))).andExpect(authenticated());
		mockMvc.perform(login("test.interviewer", "test-only-2")).andExpect(authenticated());
	}

	@Test
	void successfulLoginClearsTheCount() throws Exception {
		for (int i = 0; i < LoginThrottle.MAX_FAILURES - 1; i++) {
			mockMvc.perform(login("test.admin", "wrong"));
		}
		mockMvc.perform(login("test.admin", "test-only-1")).andExpect(authenticated());
		for (int i = 0; i < LoginThrottle.MAX_FAILURES - 1; i++) {
			mockMvc.perform(login("test.admin", "wrong"));
		}

		mockMvc.perform(login("test.admin", "test-only-1")).andExpect(authenticated());
	}

	@Test
	void databaseErrorsDontCountTowardsTheLock() throws Exception {
		when(logindao.findLogins("test.admin")).thenThrow(new DataAccessResourceFailureException("database down"));
		for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
			mockMvc.perform(login("test.admin", "test-only-1")).andExpect(redirectedUrl("/login?unavailable"));
		}

		assertThat(throttle.isLocked("test.admin", "127.0.0.1")).isFalse();
	}

	// --- Account changes while logged in (AccountCheckFilter) ---

	@Test
	void deactivatedUserIsSignedOutAtTheNextRequest() throws Exception {
		MockHttpSession session = loggedInSession();
		UserInfo deactivated = profile(SUPER_ADMIN_ID, Role.SUPER_ADMIN);
		deactivated.setIsactive(0);
		when(logindao.getUserInfo(SUPER_ADMIN_ID)).thenReturn(deactivated);

		mockMvc.perform(get("/viewpositionlist").session(session)).andExpect(redirectedUrl("/login?ended"));

		assertThat(session.isInvalid()).isTrue();
		verifyNoInteractions(positionservice);
	}

	@Test
	void roleChangeSignsTheUserOut() throws Exception {
		MockHttpSession session = loggedInSession();
		when(logindao.getUserInfo(SUPER_ADMIN_ID)).thenReturn(profile(SUPER_ADMIN_ID, Role.HR));

		mockMvc.perform(get("/home").session(session)).andExpect(redirectedUrl("/login?ended"));

		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void removedAdminRowSignsTheUserOut() throws Exception {
		MockHttpSession session = loggedInSession();
		when(logindao.getUserInfo(SUPER_ADMIN_ID)).thenReturn(null);

		mockMvc.perform(get("/home").session(session)).andExpect(redirectedUrl("/login?ended"));
	}

	@Test
	void profileEditsShowAtTheNextRequest() throws Exception {
		MockHttpSession session = loggedInSession();
		UserInfo renamed = profile(SUPER_ADMIN_ID, Role.SUPER_ADMIN);
		renamed.setFirstname("Renamed");
		when(logindao.getUserInfo(SUPER_ADMIN_ID)).thenReturn(renamed);

		mockMvc.perform(get("/home").session(session)).andExpect(status().isOk());

		assertThat(((UserInfo) session.getAttribute("user")).getFirstname()).isEqualTo("Renamed");
	}

	@Test
	void resetPasswordAllowsOnlyChangePasswordAndLogout() throws Exception {
		UserInfo mustchange = profile(SUPER_ADMIN_ID, Role.SUPER_ADMIN);
		mustchange.setMustchangepassword(1);
		when(logindao.getUserInfo(SUPER_ADMIN_ID)).thenReturn(mustchange);
		MockHttpSession session = loggedInSession();

		mockMvc.perform(get("/home").session(session)).andExpect(redirectedUrl("/changepassword"));
		mockMvc.perform(get("/viewuserlist").session(session)).andExpect(redirectedUrl("/changepassword"));
		mockMvc.perform(post("/saveposition").session(session).with(csrf())).andExpect(redirectedUrl("/changepassword"));
		mockMvc.perform(get("/changepassword").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("changepassword"));
		mockMvc.perform(post("/savepassword").session(session).with(csrf())).andExpect(status().isOk());
		mockMvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login"));

		verifyNoInteractions(positionservice);
	}

	// --- CSRF (G32) and logout ---

	@Test
	void postWithoutCsrfTokenIsRefused() throws Exception {
		mockMvc.perform(post("/deleteposition/1").with(as(Role.SUPER_ADMIN))).andExpect(status().isForbidden());
		mockMvc.perform(post("/saveposition").param("positionname", "dev ops").with(as(Role.SUPER_ADMIN)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/deleteposition/1").with(csrf().useInvalidToken()).with(as(Role.SUPER_ADMIN)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/deletecandidate").param("candidateid", "C1").with(as(Role.SUPER_ADMIN)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/saveuser").param("username", "new.user").with(as(Role.SUPER_ADMIN)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/resetpassword").param("userid", "U2").with(as(Role.SUPER_ADMIN)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/savepassword").with(as(Role.INTERVIEWER))).andExpect(status().isForbidden());

		verifyNoInteractions(positionservice, candidateservice, userservice);
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

		// GET /logout is no page (403: no access rule names it) and doesn't log out
		mockMvc.perform(get("/logout").session(session)).andExpect(status().isForbidden());
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

		// New and reset passwords are stored as {bcrypt}: 68 characters
		String hash = encoder.encode("test-only-3");
		assertThat(hash).startsWith("{bcrypt}$2");
		assertThat(hash).hasSize(68);
		assertThat(encoder.matches("test-only-3", hash)).isTrue();
	}

	// --- helpers ---

	private void givenLogin(String userid, String username, String password, Role role) {
		when(logindao.findLogins(username)).thenReturn(List.of(new LoginInfo(userid, username, password)));
		when(logindao.getUserInfo(userid)).thenReturn(profile(userid, role));
	}

	private MockHttpSession loggedInSession() throws Exception {
		MvcResult result = mockMvc.perform(login("test.admin", "test-only-1")).andExpect(authenticated()).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private static MockHttpServletRequestBuilder login(String username, String password) {
		return post("/welcome").with(csrf()).param("username", username).param("password", password);
	}

	private static RequestPostProcessor from(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}

	/** An active profile as LoginDaoImpl maps it: role is the effective role's name. */
	private static UserInfo profile(String userid, Role role) {
		UserInfo user = new UserInfo();
		user.setUserid(userid);
		user.setIsactive(1);
		user.setRole(role == null ? null : role.name());
		return user;
	}

	/** Logged in as the seed user with this role (whose admin row, stubbed in setUp, the filter re-reads). */
	private static RequestPostProcessor as(Role role) {
		String userid = switch (role) {
			case SUPER_ADMIN -> SUPER_ADMIN_ID;
			case HR -> HR_ID;
			case HIRING_MANAGER -> HIRING_MANAGER_ID;
			case INTERVIEWER -> INTERVIEWER_ID;
		};
		return asUser(userid, role);
	}

	private static RequestPostProcessor asUser(String userid, Role role) {
		return user(new RmsUserDetails(new LoginInfo(userid, "someone", "test-only"), profile(userid, role)));
	}

	/** A relative redirect, as the interceptor sent (Spring Security 7's entry point favours relative URIs). */
	private static ResultMatcher redirectsToLogin() {
		return result -> {
			assertThat(result.getResponse().getStatus()).as(result.getRequest().getRequestURI()).isEqualTo(302);
			assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/login");
		};
	}

}
