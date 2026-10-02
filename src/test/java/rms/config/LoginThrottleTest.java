package rms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** The login lock (G42): 5 failures in 15 minutes lock a username for one address for 15 minutes. */
class LoginThrottleTest {

	private static final String ADDRESS = "10.0.0.1";

	private Instant now = Instant.parse("2026-10-02T09:00:00Z");
	private final LoginThrottle throttle = new LoginThrottle(new Clock() {
		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}
	});

	@Test
	void fourFailuresDontLock() {
		fail(4);

		assertThat(throttle.isLocked("test.admin", ADDRESS)).isFalse();
	}

	@Test
	void fiveFailuresLockForFifteenMinutes() {
		fail(5);

		assertThat(throttle.isLocked("test.admin", ADDRESS)).isTrue();
		later(Duration.ofMinutes(14).plusSeconds(59));
		assertThat(throttle.isLocked("test.admin", ADDRESS)).isTrue();
		later(Duration.ofSeconds(1));
		assertThat(throttle.isLocked("test.admin", ADDRESS)).isFalse();
	}

	@Test
	void afterTheLockTheCountStartsAgain() {
		fail(5);
		later(LoginThrottle.LOCK);

		fail(4);
		assertThat(throttle.isLocked("test.admin", ADDRESS)).isFalse();
		fail(1);
		assertThat(throttle.isLocked("test.admin", ADDRESS)).isTrue();
	}

	@Test
	void failuresOlderThanTheWindowDontCount() {
		fail(4);
		later(LoginThrottle.WINDOW);

		fail(1);
		assertThat(throttle.isLocked("test.admin", ADDRESS)).isFalse();
	}

	@Test
	void failuresDuringTheLockDontExtendIt() {
		fail(5);
		later(Duration.ofMinutes(10));
		fail(3);

		later(Duration.ofMinutes(5));
		assertThat(throttle.isLocked("test.admin", ADDRESS)).isFalse();
	}

	@Test
	void successClearsTheCount() {
		fail(4);
		throttle.loginSucceeded("test.admin", ADDRESS);
		fail(4);

		assertThat(throttle.isLocked("test.admin", ADDRESS)).isFalse();
	}

	@Test
	void keyIsUsernameAndAddressWithAnyCaseOrSpaces() {
		fail(5);

		assertThat(throttle.isLocked(" TEST.Admin ", ADDRESS)).isTrue();
		assertThat(throttle.isLocked("test.admin", "10.0.0.2")).isFalse();
		assertThat(throttle.isLocked("test.interviewer", ADDRESS)).isFalse();
		assertThat(throttle.isLocked(null, null)).isFalse();
	}

	@Test
	void keyFoldsAccentsAndSharpSLikeTheDefaultCollations() {
		// MySQL's default collations treat these as the same username, so they must share one count
		assertThat(LoginThrottle.key("t" + (char) 0xE9 + "st.admin", ADDRESS))
				.isEqualTo(LoginThrottle.key("TEST.ADMIN", ADDRESS));
		assertThat(LoginThrottle.key("stra" + (char) 0xDF + "e", ADDRESS))
				.isEqualTo(LoginThrottle.key("STRASSE", ADDRESS));
		assertThat(LoginThrottle.key("test.admin", ADDRESS)).isNotEqualTo(LoginThrottle.key("test.admin", "10.0.0.2"));
	}

	private void fail(int times) {
		for (int i = 0; i < times; i++) {
			throttle.loginFailed("test.admin", ADDRESS);
		}
	}

	private void later(Duration duration) {
		now = now.plus(duration);
	}
}
