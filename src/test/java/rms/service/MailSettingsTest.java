package rms.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** F1: the mail server settings from the environment; off without a host, refused when they can't work. */
class MailSettingsTest {

	@Test
	void withoutAHostNotificationsAreOff() {
		MailSettings settings = MailSettings.from(Map.of());

		assertThat(settings.configured()).isFalse();
		assertThat(settings.enabled()).isFalse();
		assertThat(settings.problem()).isNull();
		assertThat(MailSettings.OFF.enabled()).isFalse();
		assertThat(MailSettings.from(Map.of("RMS_MAIL_HOST", "  ")).configured()).isFalse();
	}

	@Test
	void aHostAndASenderTurnThemOnWithStartTlsOnPort587() {
		MailSettings settings = MailSettings.from(
				Map.of("RMS_MAIL_HOST", " smtp.example.test ", "RMS_MAIL_FROM", "rms@example.test",
						"RMS_APP_URL", "https://rms.example.test/rms/"));

		assertThat(settings.enabled()).isTrue();
		assertThat(settings.host()).isEqualTo("smtp.example.test");
		assertThat(settings.port()).isEqualTo(587);
		assertThat(settings.security()).isEqualTo(MailSettings.STARTTLS);
		assertThat(settings.user()).isNull();
		// The trailing slash goes, so links don't get a double slash
		assertThat(settings.appurl()).isEqualTo("https://rms.example.test/rms");
	}

	@Test
	void settingsThatCantWorkAreRefusedWithAReason() {
		assertThat(problem(Map.of("RMS_MAIL_HOST", "smtp.example.test"))).contains("RMS_MAIL_FROM");
		assertThat(problem(Map.of("RMS_MAIL_HOST", "smtp.example.test", "RMS_MAIL_FROM", "rms@example.test",
				"RMS_MAIL_PORT", "abc"))).contains("RMS_MAIL_PORT");
		assertThat(problem(Map.of("RMS_MAIL_HOST", "smtp.example.test", "RMS_MAIL_FROM", "rms@example.test",
				"RMS_MAIL_SECURITY", "tls"))).contains("RMS_MAIL_SECURITY");
		assertThat(problem(Map.of("RMS_MAIL_HOST", "smtp.example.test", "RMS_MAIL_FROM", "rms@example.test",
				"RMS_APP_URL", "rms.example.test"))).contains("RMS_APP_URL");
		assertThat(MailSettings.from(Map.of("RMS_MAIL_HOST", "smtp.example.test")).enabled()).isFalse();
	}

	@Test
	void sslAndNoneAreAccepted() {
		assertThat(MailSettings.from(Map.of("RMS_MAIL_HOST", "h", "RMS_MAIL_FROM", "a@b.test", "RMS_MAIL_PORT", "465",
				"RMS_MAIL_SECURITY", "SSL")).enabled()).isTrue();
		assertThat(MailSettings.from(Map.of("RMS_MAIL_HOST", "h", "RMS_MAIL_FROM", "a@b.test",
				"RMS_MAIL_SECURITY", "none")).enabled()).isTrue();
	}

	@Test
	void toStringNeverShowsThePasswordOrTheHost() {
		MailSettings settings = MailSettings.from(Map.of("RMS_MAIL_HOST", "smtp.example.test", "RMS_MAIL_FROM",
				"rms@example.test", "RMS_MAIL_USER", "rms", "RMS_MAIL_PASSWORD", "test-only-secret"));

		assertThat(settings.toString()).doesNotContain("test-only-secret", "smtp.example.test")
				.isEqualTo("MailSettings[configured]");
	}

	private static String problem(Map<String, String> env) {
		return MailSettings.from(env).problem();
	}
}
