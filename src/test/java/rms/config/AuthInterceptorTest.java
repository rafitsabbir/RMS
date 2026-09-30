package rms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import rms.controller.LoginController;
import rms.controller.MarksController;
import rms.controller.PositionController;
import rms.model.UserInfo;
import rms.service.LoginService;
import rms.service.MarksService;
import rms.service.PositionService;

/** G11: pages need a login, and admin pages need isinterviewer = N. Uses the registration in WebConfig. */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

	@Mock
	LoginService loginservice;

	@Mock
	PositionService positionservice;

	@Mock
	MarksService marksservice;

	@InjectMocks
	LoginController logincontroller;

	@InjectMocks
	PositionController positioncontroller;

	@InjectMocks
	MarksController markscontroller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(logincontroller, positioncontroller, markscontroller)
				.addInterceptors(registeredInterceptors().toArray(new HandlerInterceptor[0]))
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void loggedOutRequestsRedirectToLogin() throws Exception {
		mockMvc.perform(get("/viewpositionlist")).andExpect(redirectedUrl("/login"));
		mockMvc.perform(post("/saveposition").param("positionname", "dev ops")).andExpect(redirectedUrl("/login"));
		// characterizes G17: delete is a GET
		mockMvc.perform(get("/deleteposition/1")).andExpect(redirectedUrl("/login"));
		mockMvc.perform(get("/adminviewmarks")).andExpect(redirectedUrl("/login"));

		verifyNoInteractions(positionservice, marksservice);
	}

	@Test
	void adminSeesAdminPages() throws Exception {
		mockMvc.perform(get("/viewpositionlist").session(sessionFor("N")))
				.andExpect(status().isOk())
				.andExpect(view().name("viewposition"));
		mockMvc.perform(get("/adminviewmarks").session(sessionFor("n")))
				.andExpect(status().isOk())
				.andExpect(view().name("viewmarks"));
	}

	@Test
	void interviewerIsRefusedAdminPages() throws Exception {
		mockMvc.perform(get("/viewpositionlist").session(sessionFor("Y"))).andExpect(status().isForbidden());
		// characterizes G17: delete is a GET
		mockMvc.perform(get("/deleteposition/1").session(sessionFor("Y"))).andExpect(status().isForbidden());
		mockMvc.perform(get("/adminviewmarks").session(sessionFor("Y"))).andExpect(status().isForbidden());

		verifyNoInteractions(positionservice, marksservice);
	}

	@Test
	void userWithoutRoleIsRefusedWithoutError() throws Exception {
		mockMvc.perform(get("/viewpositionlist").session(sessionFor(null))).andExpect(status().isForbidden());

		verifyNoInteractions(positionservice);
	}

	@Test
	void sessionAttributeOfAnotherTypeCountsAsLoggedOut() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", "U1");

		mockMvc.perform(get("/viewpositionlist").session(session)).andExpect(redirectedUrl("/login"));

		verifyNoInteractions(positionservice);
	}

	@Test
	void pathVariantsGetTheSameCheck() throws Exception {
		mockMvc.perform(get("/login;jsessionid=X"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"));
		mockMvc.perform(get("/viewpositionlist;jsessionid=X")).andExpect(redirectedUrl("/login"));
		mockMvc.perform(head("/viewpositionlist")).andExpect(redirectedUrl("/login"));
		mockMvc.perform(head("/viewpositionlist").session(sessionFor("Y"))).andExpect(status().isForbidden());

		verifyNoInteractions(positionservice);
	}

	@Test
	void loginPagesNeedNoSession() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"));
		mockMvc.perform(post("/welcome").param("username", "test.admin").param("password", "wrong"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"));
	}

	@Test
	void loginAndResourcePathsAreExcluded() {
		List<Object> interceptors = registeredInterceptors();
		assertThat(interceptors).hasSize(1);
		MappedInterceptor mapped = (MappedInterceptor) interceptors.get(0);

		assertThat(mapped.getInterceptor()).isInstanceOf(AuthInterceptor.class);
		assertThat(mapped.matches(request("/login"))).isFalse();
		assertThat(mapped.matches(request("/welcome"))).isFalse();
		assertThat(mapped.matches(request("/resources/css/login.css"))).isFalse();
		assertThat(mapped.matches(request("/viewpositionlist"))).isTrue();
		assertThat(mapped.matches(request("/updatelanguage/1"))).isTrue();
		assertThat(mapped.matches(request("/createcandidate"))).isTrue();
	}

	private static List<Object> registeredInterceptors() {
		ReadableRegistry registry = new ReadableRegistry();
		new WebConfig().addInterceptors(registry);
		return registry.getInterceptors();
	}

	private static MockHttpSession sessionFor(String isinterviewer) {
		UserInfo user = new UserInfo();
		user.setUserid("U1");
		user.setIsinterviewer(isinterviewer);
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", user);
		return session;
	}

	/** parseAndCache makes matches() use PathPattern, as Spring 6+ does; the MockMvc tests use Spring 5.3's own matcher. */
	private static MockHttpServletRequest request(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		ServletRequestPathUtils.parseAndCache(request);
		return request;
	}

	/** getInterceptors() is protected; this exposes what WebConfig registered. */
	static class ReadableRegistry extends InterceptorRegistry {

		@Override
		public List<Object> getInterceptors() {
			return super.getInterceptors();
		}

	}

}
