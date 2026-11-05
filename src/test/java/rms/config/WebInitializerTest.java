package rms.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;

/** RMS_SECURE_COOKIE: the session cookie's Secure flag (release-tracker item 8). */
class WebInitializerTest {

	@Test
	void trueMakesTheSessionCookieSecure() {
		MockServletContext context = new MockServletContext();
		WebInitializer.applySecureCookie(context, " TRUE ");
		assertThat(context.getSessionCookieConfig().isSecure()).isTrue();
	}

	@Test
	void unsetOrAnythingElseLeavesItToTomcat() {
		for (String setting : new String[] {null, "", "false", "yes", "1"}) {
			MockServletContext context = new MockServletContext();
			WebInitializer.applySecureCookie(context, setting);
			assertThat(context.getSessionCookieConfig().isSecure()).as(String.valueOf(setting)).isFalse();
		}
	}
}
