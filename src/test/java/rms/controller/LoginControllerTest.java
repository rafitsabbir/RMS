package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.DashboardInfo;
import rms.model.Role;
import rms.model.UserInfo;
import rms.service.DashboardService;

/**
 * Characterization tests: pin current login-page behaviour. The login post, logout and access rules are
 * Spring Security's, tested in rms.config.SecurityConfigTest.
 */
class LoginControllerTest {

	MockMvc mockMvc;

	DashboardService dashboardservice = mock(DashboardService.class);

	@BeforeEach
	void setUp() {
		LoginController controller = new LoginController();
		controller.dashboardservice = dashboardservice;
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void loginPageRendersLoginViewAndKeepsSession() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", new UserInfo());

		mockMvc.perform(get("/login").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(forwardedUrl("/WEB-INF/jsp/login.jsp"))
				.andExpect(model().attributeDoesNotExist("errorMessage"));

		// G32: logging out is POST /logout now, not a visit to the login page
		assertThat(session.isInvalid()).isFalse();
	}

	@Test
	void failedLoginShowsError() throws Exception {
		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(view().name("login"))
				.andExpect(model().attribute("errorMessage", "Invalid login!"));
	}

	@Test
	void expiredSessionShowsMessage() throws Exception {
		mockMvc.perform(get("/login").param("expired", ""))
				.andExpect(view().name("login"))
				.andExpect(model().attribute("errorMessage", "Your session expired. Please sign in again."));
	}

	@Test
	void databaseErrorShowsUnavailable() throws Exception {
		mockMvc.perform(get("/login").param("unavailable", ""))
				.andExpect(view().name("login"))
				.andExpect(model().attribute("errorMessage", "Sign-in isn't available right now. Please try again later."));
	}

	@Test
	void signedOutAfterAnAccountChangeShowsMessage() throws Exception {
		// rms.config.AccountCheckFilter: deactivated, or the role changed, while logged in
		mockMvc.perform(get("/login").param("ended", ""))
				.andExpect(view().name("login"))
				.andExpect(model().attribute("errorMessage",
						"You were signed out because your account changed. Please sign in again."));
	}

	@Test
	void homeShowsMainViewWithSessionUser() throws Exception {
		UserInfo user = new UserInfo();
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", user);

		mockMvc.perform(get("/home").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("main"))
				.andExpect(model().attribute("userinfo", user));
	}

	@Test
	void homeCarriesTheDashboardOfTheUsersRole() throws Exception {
		UserInfo user = new UserInfo();
		user.setUserid("U3");
		user.setRole("HR");
		DashboardInfo dashboard = new DashboardInfo();
		when(dashboardservice.getDashboard(Role.HR, "U3")).thenReturn(dashboard);
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", user);

		mockMvc.perform(get("/home").session(session))
				.andExpect(view().name("main"))
				.andExpect(model().attribute("dashboard", dashboard));
	}

	@Test
	void homeStillOpensWithoutTheDashboardWhenItCantBeRead() throws Exception {
		// For example migration 006 not run yet: the home page works, minus the figures (logged as a WARN)
		UserInfo user = new UserInfo();
		user.setUserid("U3");
		user.setRole("HR");
		when(dashboardservice.getDashboard(Role.HR, "U3"))
				.thenThrow(new BadSqlGrammarException("dashboard", "select 1", new SQLException("no table")));
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", user);

		mockMvc.perform(get("/home").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("main"))
				.andExpect(model().attributeDoesNotExist("dashboard"));
	}

	@Test
	void rootRedirectsToHome() throws Exception {
		mockMvc.perform(get("/")).andExpect(redirectedUrl("/home"));
	}

}
