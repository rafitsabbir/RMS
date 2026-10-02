package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import rms.config.SecurityConfig;
import rms.dao.UserDao;
import rms.model.UserInfo;

/** Passwords reach the DAO only as {bcrypt} hashes; the current one is checked before a change. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	UserDao userdao;

	UserServiceImpl service;

	/** The production encoder: {bcrypt} for new hashes, and legacy plain-text rows still match. */
	final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

	@BeforeEach
	void setUp() {
		service = new UserServiceImpl();
		service.setUserdao(userdao);
		service.setPasswordEncoder(encoder);
	}

	@Test
	void newUsersPasswordIsStoredAsBcrypt() {
		UserInfo user = new UserInfo();
		when(userdao.addUser(eq(user), anyString())).thenReturn("U6");

		assertThat(service.addUser(user, "test-only-new")).isEqualTo("U6");

		ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
		verify(userdao).addUser(eq(user), stored.capture());
		assertThat(stored.getValue()).startsWith("{bcrypt}$2").hasSize(68);
		assertThat(encoder.matches("test-only-new", stored.getValue())).isTrue();
	}

	@Test
	void resetStoresBcryptAndForcesAChange() {
		service.resetPassword("U2", "test-only-temp");

		ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
		verify(userdao).setPassword(eq("U2"), stored.capture(), eq(true));
		assertThat(encoder.matches("test-only-temp", stored.getValue())).isTrue();
		assertThat(stored.getValue()).doesNotContain("test-only-temp");
	}

	@Test
	void changeChecksTheLegacyPlainTextPassword() {
		when(userdao.findPassword("U1")).thenReturn("test-only-1");

		assertThat(service.changePassword("U1", "test-only-1", "test-only-new")).isTrue();

		ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
		verify(userdao).setPassword(eq("U1"), stored.capture(), eq(false));
		assertThat(stored.getValue()).startsWith("{bcrypt}");
		assertThat(encoder.matches("test-only-new", stored.getValue())).isTrue();
	}

	@Test
	void changeChecksABcryptPassword() {
		when(userdao.findPassword("U3")).thenReturn(encoder.encode("test-only-3"));

		assertThat(service.changePassword("U3", "test-only-3", "test-only-new")).isTrue();
	}

	@Test
	void wrongCurrentPasswordChangesNothing() {
		when(userdao.findPassword("U1")).thenReturn("test-only-1");

		assertThat(service.changePassword("U1", "wrong", "test-only-new")).isFalse();
		assertThat(service.changePassword("U1", "TEST-ONLY-1", "test-only-new")).isFalse();

		verify(userdao, never()).setPassword(anyString(), anyString(), anyBoolean());
	}

	@Test
	void userWithoutLoginRowCantChange() {
		when(userdao.findPassword("U9")).thenReturn(null);

		assertThat(service.changePassword("U9", "anything", "test-only-new")).isFalse();
		verify(userdao, never()).setPassword(anyString(), anyString(), anyBoolean());
	}
}
