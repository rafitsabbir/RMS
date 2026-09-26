package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.UserInfo;
import rms.service.LoginService;

/** Characterization tests: pin current login behaviour before any upgrade. */
@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

	@Mock
	LoginService loginservice;

	@InjectMocks
	LoginController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void loginPageRendersLoginViewAndInvalidatesSession() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", new UserInfo());

		mockMvc.perform(get("/login").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(forwardedUrl("/WEB-INF/jsp/login.jsp"));

		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void validLoginShowsMainViewAndStoresUserInSession() throws Exception {
		UserInfo user = new UserInfo();
		user.setUserid("U1");
		when(loginservice.checkLogin("test.admin", "test-only-1")).thenReturn("U1");
		when(loginservice.getUserInfo("U1")).thenReturn(user);

		mockMvc.perform(post("/welcome").param("username", "test.admin").param("password", "test-only-1"))
				.andExpect(status().isOk())
				.andExpect(view().name("main"))
				.andExpect(model().attribute("userinfo", user))
				.andExpect(request().sessionAttribute("user", user));
	}

	@Test
	void invalidLoginShowsLoginViewWithErrorAndInvalidatesSession() throws Exception {
		MockHttpSession session = new MockHttpSession();
		when(loginservice.checkLogin("test.admin", "wrong")).thenReturn(null);

		mockMvc.perform(post("/welcome").session(session).param("username", "test.admin").param("password", "wrong"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(model().attribute("errorMessage", "Invalid login!"));

		assertThat(session.isInvalid()).isTrue();
		verify(loginservice, never()).getUserInfo(anyString());
	}

}
