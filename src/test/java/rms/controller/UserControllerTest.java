package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.Role;
import rms.model.UserInfo;
import rms.service.UserService;

/** Users and Roles (Phase 1 of the roles plan): add, edit, deactivate, reactivate and reset, with their guards. */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	UserService userservice;

	@InjectMocks
	UserController controller;

	MockMvc mockMvc;

	/** Logged in as U1, a Super Admin. */
	MockHttpSession session;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
		session = new MockHttpSession();
		session.setAttribute("user", user("U1", "test.admin", Role.SUPER_ADMIN, 1));
	}

	// --- list and forms ---

	@Test
	void listShowsEveryUserAndWhoIsLoggedIn() throws Exception {
		List<UserInfo> users = List.of(user("U1", "test.admin", Role.SUPER_ADMIN, 1));
		when(userservice.getAllUsers()).thenReturn(users);

		mockMvc.perform(get("/viewuserlist").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("viewuser"))
				.andExpect(model().attribute("userlist", users))
				.andExpect(model().attribute("currentuserid", "U1"));
	}

	@Test
	void createFormOffersTheFourRoles() throws Exception {
		mockMvc.perform(get("/createuser").session(session))
				.andExpect(view().name("createuser"))
				.andExpect(model().attribute("update", false))
				.andExpect(model().attribute("roles", Role.values()));
	}

	@Test
	void editFormKnowsWhenItIsTheUserThemself() throws Exception {
		when(userservice.findUserById("U1")).thenReturn(user("U1", "test.admin", Role.SUPER_ADMIN, 1));
		when(userservice.findUserById("U2")).thenReturn(user("U2", "test.interviewer", Role.INTERVIEWER, 1));

		mockMvc.perform(get("/updateuser").param("userid", "U1").session(session))
				.andExpect(view().name("createuser"))
				.andExpect(model().attribute("update", true))
				.andExpect(model().attribute("self", true));
		mockMvc.perform(get("/updateuser").param("userid", "U2").session(session))
				.andExpect(model().attribute("self", false))
				.andExpect(model().attribute("passwordreset", false));
	}

	@Test
	void unknownUserIs404() throws Exception {
		mockMvc.perform(get("/updateuser").param("userid", "U9").session(session)).andExpect(status().isNotFound());
		mockMvc.perform(post("/deactivateuser").param("userid", "U9").session(session)).andExpect(status().isNotFound());
		mockMvc.perform(post("/reactivateuser").param("userid", "U9").session(session)).andExpect(status().isNotFound());
		mockMvc.perform(post("/resetpassword").param("userid", "U9").session(session)).andExpect(status().isNotFound());
		mockMvc.perform(post("/saveuser").param("update", "true").param("userid", "U9").session(session))
				.andExpect(status().isNotFound());
	}

	// --- add ---

	@Test
	void addTrimsTheFieldsAndSavesWithTheTemporaryPassword() throws Exception {
		when(userservice.addUser(any(UserInfo.class), eq("test-only-new"))).thenReturn("U6");

		mockMvc.perform(newUser("username", " new.user ", "email", " ", "phone", " 000-0006 ", "role", "hr"))
				.andExpect(redirectedUrl("/viewuserlist"));

		ArgumentCaptor<UserInfo> saved = ArgumentCaptor.forClass(UserInfo.class);
		verify(userservice).addUser(saved.capture(), eq("test-only-new"));
		assertThat(saved.getValue().getUsername()).isEqualTo("new.user");
		assertThat(saved.getValue().getFirstname()).isEqualTo("Nina");
		assertThat(saved.getValue().getEmail()).isNull();
		assertThat(saved.getValue().getPhone()).isEqualTo("000-0006");
		assertThat(saved.getValue().getRole()).isEqualTo("HR");
	}

	@Test
	void addIgnoresAPostedId() throws Exception {
		when(userservice.addUser(any(UserInfo.class), anyString())).thenReturn("U6");

		mockMvc.perform(newUser("userid", "U1", "isactive", "0", "mustchangepassword", "0"))
				.andExpect(redirectedUrl("/viewuserlist"));

		ArgumentCaptor<UserInfo> saved = ArgumentCaptor.forClass(UserInfo.class);
		verify(userservice).addUser(saved.capture(), anyString());
		assertThat(saved.getValue().getUserid()).isNull();
	}

	@Test
	void addRefusesBadInput() throws Exception {
		expectRefused(newUser("username", ""), "Please enter a username.");
		expectRefused(newUser("username", "a b c"),
				"The username must be 3 to 100 letters, digits or . _ @ - characters.");
		expectRefused(newUser("username", "ab"),
				"The username must be 3 to 100 letters, digits or . _ @ - characters.");
		expectRefused(newUser("firstname", " "), "Please enter the first name.");
		expectRefused(newUser("lastname", ""), "Please enter the last name.");
		expectRefused(newUser("email", "not-an-address"),
				"Please enter a valid e-mail address, or leave it empty.");
		expectRefused(newUser("role", ""), "Please choose a role.");
		expectRefused(newUser("role", "ADMIN"), "Please choose a role.");
		expectRefused(newUser("password", "short", "confirmpassword", "short"),
				"The password must have at least 8 characters.");
		expectRefused(newUser("confirmpassword", "test-only-other"), "The two passwords don't match.");
		String tooLong = "x".repeat(73);
		expectRefused(newUser("password", tooLong, "confirmpassword", tooLong),
				"The password is too long: at most 72 characters, and fewer with accented or non-Latin letters.");

		verify(userservice, never()).addUser(any(UserInfo.class), anyString());
	}

	@Test
	void addRefusesATakenUsername() throws Exception {
		when(userservice.addUser(any(UserInfo.class), anyString())).thenReturn(null);

		expectRefused(newUser(), "That username is already taken.");
	}

	// --- edit ---

	@Test
	void editSavesProfileAndRoleButKeepsIdAndUsername() throws Exception {
		when(userservice.findUserById("U2")).thenReturn(user("U2", "test.interviewer", Role.INTERVIEWER, 1));

		mockMvc.perform(post("/saveuser").session(session).param("update", "true").param("userid", "U2")
				.param("username", "renamed").param("firstname", "Ivan").param("lastname", "Ivanov")
				.param("role", "HIRING_MANAGER"))
				.andExpect(redirectedUrl("/viewuserlist"));

		ArgumentCaptor<UserInfo> saved = ArgumentCaptor.forClass(UserInfo.class);
		verify(userservice).updateUser(saved.capture());
		assertThat(saved.getValue().getUserid()).isEqualTo("U2");
		assertThat(saved.getValue().getUsername()).isEqualTo("test.interviewer");
		assertThat(saved.getValue().getLastname()).isEqualTo("Ivanov");
		assertThat(saved.getValue().getRole()).isEqualTo("HIRING_MANAGER");
	}

	@Test
	void superAdminCantChangeTheirOwnRole() throws Exception {
		when(userservice.findUserById("U1")).thenReturn(user("U1", "test.admin", Role.SUPER_ADMIN, 1));

		mockMvc.perform(editOwnProfile().param("role", "HR"))
				.andExpect(view().name("createuser"))
				.andExpect(model().attribute("errorMessage", "You can't change your own role."));
		verify(userservice, never()).updateUser(any(UserInfo.class));

		// Their own name, with the same role, is fine
		mockMvc.perform(editOwnProfile().param("role", "SUPER_ADMIN")).andExpect(redirectedUrl("/viewuserlist"));
	}

	@Test
	void lastActiveSuperAdminCantBeDemoted() throws Exception {
		when(userservice.findUserById("U7")).thenReturn(user("U7", "other.admin", Role.SUPER_ADMIN, 1));
		when(userservice.countActiveSuperAdmins()).thenReturn(1);

		mockMvc.perform(post("/saveuser").session(session).param("update", "true").param("userid", "U7")
				.param("firstname", "Other").param("lastname", "Admin").param("role", "HR"))
				.andExpect(model().attribute("errorMessage", "There must be at least one active Super Admin."));

		verify(userservice, never()).updateUser(any(UserInfo.class));
	}

	// --- deactivate and reactivate ---

	@Test
	void deactivateIsSoft() throws Exception {
		when(userservice.findUserById("U2")).thenReturn(user("U2", "test.interviewer", Role.INTERVIEWER, 1));

		mockMvc.perform(post("/deactivateuser").param("userid", "U2").session(session))
				.andExpect(redirectedUrl("/viewuserlist"));

		verify(userservice).setActive("U2", false);
	}

	@Test
	void superAdminCantDeactivateThemself() throws Exception {
		when(userservice.findUserById("U1")).thenReturn(user("U1", "test.admin", Role.SUPER_ADMIN, 1));

		mockMvc.perform(post("/deactivateuser").param("userid", "U1").session(session))
				.andExpect(view().name("viewuser"))
				.andExpect(model().attribute("errorMessage", "You can't deactivate yourself."));

		verify(userservice, never()).setActive(anyString(), anyBoolean());
	}

	@Test
	void lastActiveSuperAdminCantBeDeactivated() throws Exception {
		when(userservice.findUserById("U7")).thenReturn(user("U7", "other.admin", Role.SUPER_ADMIN, 1));
		when(userservice.countActiveSuperAdmins()).thenReturn(1);

		mockMvc.perform(post("/deactivateuser").param("userid", "U7").session(session))
				.andExpect(model().attribute("errorMessage", "There must be at least one active Super Admin."));

		verify(userservice, never()).setActive(anyString(), anyBoolean());
	}

	@Test
	void reactivateRestoresTheLogin() throws Exception {
		when(userservice.findUserById("U5")).thenReturn(user("U5", "test.former", Role.INTERVIEWER, 0));

		mockMvc.perform(post("/reactivateuser").param("userid", "U5").session(session))
				.andExpect(redirectedUrl("/viewuserlist"));

		verify(userservice).setActive("U5", true);
	}

	// --- reset password ---

	@Test
	void resetSetsATemporaryPassword() throws Exception {
		when(userservice.findUserById("U2")).thenReturn(user("U2", "test.interviewer", Role.INTERVIEWER, 1));

		mockMvc.perform(post("/resetpassword").param("userid", "U2").param("newpassword", "test-only-temp")
				.param("confirmpassword", "test-only-temp").session(session))
				.andExpect(redirectedUrl("/updateuser?userid=U2&passwordreset=true"));

		verify(userservice).resetPassword("U2", "test-only-temp");
	}

	@Test
	void resetRefusals() throws Exception {
		when(userservice.findUserById("U1")).thenReturn(user("U1", "test.admin", Role.SUPER_ADMIN, 1));
		when(userservice.findUserById("U2")).thenReturn(user("U2", "test.interviewer", Role.INTERVIEWER, 1));
		when(userservice.findUserById("U8")).thenReturn(user("U8", null, Role.INTERVIEWER, 1));

		expectResetRefused("U1", "test-only-temp", "Use Change password for your own password.");
		expectResetRefused("U8", "test-only-temp", "This user has no login, so there is no password to reset.");
		expectResetRefused("U2", "short", "The password must have at least 8 characters.");

		verify(userservice, never()).resetPassword(anyString(), anyString());
	}

	// --- helpers ---

	/** An add-user post with valid values; pairs of name and value replace them (a repeated param would add a value). */
	private MockHttpServletRequestBuilder newUser(String... replacements) {
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("username", "new.user");
		params.put("firstname", " Nina ");
		params.put("lastname", "New");
		params.put("role", "INTERVIEWER");
		params.put("password", "test-only-new");
		params.put("confirmpassword", "test-only-new");
		for (int i = 0; i < replacements.length; i += 2) {
			params.put(replacements[i], replacements[i + 1]);
		}
		MockHttpServletRequestBuilder request = post("/saveuser").session(session);
		params.forEach((name, value) -> request.param(name, value));
		return request;
	}

	private MockHttpServletRequestBuilder editOwnProfile() {
		return post("/saveuser").session(session).param("update", "true").param("userid", "U1")
				.param("firstname", "Ada").param("lastname", "Admin");
	}

	private void expectRefused(MockHttpServletRequestBuilder request, String message) throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(view().name("createuser"))
				.andExpect(model().attribute("errorMessage", message));
	}

	private void expectResetRefused(String userid, String password, String message) throws Exception {
		mockMvc.perform(post("/resetpassword").param("userid", userid).param("newpassword", password)
				.param("confirmpassword", password).session(session))
				.andExpect(view().name("createuser"))
				.andExpect(model().attribute("passwordMessage", message));
	}

	private static UserInfo user(String userid, String username, Role role, int isactive) {
		UserInfo user = new UserInfo();
		user.setUserid(userid);
		user.setUsername(username);
		user.setRole(role.name());
		user.setIsactive(isactive);
		return user;
	}
}
