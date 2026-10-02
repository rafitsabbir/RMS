package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.UserInfo;

/**
 * Characterization tests: pin current login-page behaviour. The login post, logout and access rules are
 * Spring Security's, tested in rms.config.SecurityConfigTest.
 */
class LoginControllerTest {

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new LoginController())
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
	void rootRedirectsToHome() throws Exception {
		mockMvc.perform(get("/")).andExpect(redirectedUrl("/home"));
	}

}
