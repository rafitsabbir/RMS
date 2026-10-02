package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import rms.dao.LoginDao;
import rms.model.LoginInfo;
import rms.model.UserInfo;

/** The user Spring Security checks the password against: users row, admin row and role (G16, G26). */
@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

	@Mock
	LoginDao logindao;

	@InjectMocks
	LoginServiceImpl service;

	@Test
	void roleComesFromIsinterviewer() {
		assertThat(rolesFor("N")).containsExactly(RmsUserDetails.ADMIN);
		assertThat(rolesFor("n")).containsExactly(RmsUserDetails.ADMIN);
		assertThat(rolesFor("Y")).containsExactly(RmsUserDetails.INTERVIEWER);
		assertThat(rolesFor("y")).containsExactly(RmsUserDetails.INTERVIEWER);
		assertThat(rolesFor(null)).isEmpty();
		assertThat(rolesFor("X")).isEmpty();
	}

	@Test
	void userCarriesUseridPasswordAndProfile() {
		UserInfo profile = profile("N");
		when(logindao.findLogins("test.admin")).thenReturn(List.of(new LoginInfo("U1", "test.admin", "test-only-1")));
		when(logindao.getUserInfo("U1")).thenReturn(profile);

		RmsUserDetails user = service.loadUserByUsername("test.admin");

		assertThat(user.getUserid()).isEqualTo("U1");
		assertThat(user.getUsername()).isEqualTo("test.admin");
		assertThat(user.getPassword()).isEqualTo("test-only-1");
		assertThat(user.getUserinfo()).isSameAs(profile);
		// admin.isactive is still ignored (open question #20)
		assertThat(user.isEnabled()).isTrue();
	}

	@Test
	void unknownUsernameIsNotFound() {
		when(logindao.findLogins("nobody")).thenReturn(List.of());

		assertThatThrownBy(() -> service.loadUserByUsername("nobody")).isInstanceOf(UsernameNotFoundException.class);
		verify(logindao, never()).getUserInfo(anyString());
	}

	@Test
	void usernameOnTwoRowsIsNotFound() {
		when(logindao.findLogins("twin")).thenReturn(
				List.of(new LoginInfo("U7", "twin", "a"), new LoginInfo("U8", "twin", "b")));

		assertThatThrownBy(() -> service.loadUserByUsername("twin")).isInstanceOf(UsernameNotFoundException.class);
		verify(logindao, never()).getUserInfo(anyString());
	}

	@Test
	void missingAdminRowGivesUserWithoutProfileOrRole() {
		// SecurityConfig refuses this user after the password check (G26)
		when(logindao.findLogins("orphan")).thenReturn(List.of(new LoginInfo("U9", "orphan", "test-only-9")));
		when(logindao.getUserInfo("U9")).thenReturn(null);

		RmsUserDetails user = service.loadUserByUsername("orphan");

		assertThat(user.getUserinfo()).isNull();
		assertThat(user.getAuthorities()).isEmpty();
	}

	@Test
	void eraseCredentialsDropsThePassword() {
		RmsUserDetails user = new RmsUserDetails(new LoginInfo("U1", "test.admin", "test-only-1"), profile("N"));

		user.eraseCredentials();

		assertThat(user.getPassword()).isNull();
		assertThat(user.toString()).doesNotContain("test-only-1");
	}

	private static List<String> rolesFor(String isinterviewer) {
		RmsUserDetails user = new RmsUserDetails(new LoginInfo("U1", "u", "p"), profile(isinterviewer));
		return AuthorityUtils.authorityListToSet(user.getAuthorities()).stream().toList();
	}

	private static UserInfo profile(String isinterviewer) {
		UserInfo user = new UserInfo();
		user.setUserid("U1");
		user.setIsinterviewer(isinterviewer);
		return user;
	}

}
