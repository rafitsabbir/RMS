package rms.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.LoginThrottle;
import rms.config.WebConfig;
import rms.model.UserInfo;
import rms.service.UserService;

/** Change password, for every logged-in user. */
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

	@Mock
	UserService userservice;

	@Spy
	LoginThrottle throttle = new LoginThrottle();

	@InjectMocks
	AccountController controller;

	MockMvc mockMvc;

	MockHttpSession session;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
		UserInfo user = new UserInfo();
		user.setUserid("U2");
		user.setUsername("test.interviewer");
		session = new MockHttpSession();
		session.setAttribute("user", user);
	}

	@Test
	void pageShowsWhetherTheChangeIsForced() throws Exception {
		mockMvc.perform(get("/changepassword").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("changepassword"))
				.andExpect(model().attribute("mustchange", false))
				.andExpect(model().attribute("changed", false));

		((UserInfo) session.getAttribute("user")).setMustchangepassword(1);
		mockMvc.perform(get("/changepassword").session(session)).andExpect(model().attribute("mustchange", true));
		mockMvc.perform(get("/changepassword").param("changed", "").session(session))
				.andExpect(model().attribute("changed", true));
	}

	@Test
	void changeSavesTheNewPassword() throws Exception {
		when(userservice.changePassword("U2", "test-only-2", "test-only-new")).thenReturn(true);

		mockMvc.perform(change("test-only-2", "test-only-new", "test-only-new"))
				.andExpect(redirectedUrl("/changepassword?changed"));
	}

	@Test
	void refusals() throws Exception {
		expectRefused(change("", "test-only-new", "test-only-new"), "Please enter your current password.");
		expectRefused(change("test-only-2", "", ""), "Please enter the new password.");
		expectRefused(change("test-only-2", "short", "short"), "The password must have at least 8 characters.");
		expectRefused(change("test-only-2", "test-only-new", "test-only-other"), "The two passwords don't match.");
		expectRefused(change("test-only-2", "test-only-2", "test-only-2"),
				"The new password must be different from the current one.");

		verify(userservice, never()).changePassword(anyString(), anyString(), anyString());
	}

	@Test
	void wrongCurrentPasswordIsCountedAndLocks() throws Exception {
		for (int i = 0; i < 5; i++) {
			expectRefused(change("wrong", "test-only-new", "test-only-new"), "The current password is wrong.");
		}

		// Locked now: not even checked
		expectRefused(change("test-only-2", "test-only-new", "test-only-new"),
				"Too many wrong passwords. Please try again later.");
		verify(userservice, times(5)).changePassword(anyString(), anyString(), anyString());
	}

	@Test
	void successfulChangeClearsTheCount() throws Exception {
		when(userservice.changePassword("U2", "test-only-2", "test-only-new")).thenReturn(true);
		when(userservice.changePassword("U2", "wrong", "test-only-new")).thenReturn(false);
		for (int i = 0; i < 4; i++) {
			expectRefused(change("wrong", "test-only-new", "test-only-new"), "The current password is wrong.");
		}
		mockMvc.perform(change("test-only-2", "test-only-new", "test-only-new"))
				.andExpect(redirectedUrl("/changepassword?changed"));

		// One more miss later doesn't lock the page
		expectRefused(change("wrong", "test-only-new", "test-only-new"), "The current password is wrong.");
		mockMvc.perform(change("test-only-2", "test-only-new", "test-only-new"))
				.andExpect(redirectedUrl("/changepassword?changed"));
	}

	@Test
	void noSessionUserIsRefused() throws Exception {
		mockMvc.perform(post("/savepassword").param("currentpassword", "x")).andExpect(status().isForbidden());
	}

	private MockHttpServletRequestBuilder change(String current, String password, String confirm) {
		return post("/savepassword").session(session).param("currentpassword", current)
				.param("newpassword", password).param("confirmpassword", confirm);
	}

	private void expectRefused(MockHttpServletRequestBuilder request, String message) throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(view().name("changepassword"))
				.andExpect(model().attribute("errorMessage", message));
	}
}
