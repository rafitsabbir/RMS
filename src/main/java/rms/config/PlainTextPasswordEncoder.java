package rms.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Matches the legacy plain-text passwords in users.password (G13): exactly (case-sensitive) and in constant
 * time. SecurityConfig uses it only for stored values without an {id} prefix. It never encodes, so nothing new
 * is ever stored in plain text.
 */
final class PlainTextPasswordEncoder implements PasswordEncoder {

	/*
	 * Spring Security checks an unknown username against a bcrypt hash, so that it takes as long as a known one.
	 * A plain-text compare alone takes microseconds, and the answer time would tell which usernames exist. One
	 * bcrypt check per compare evens that out.
	 */
	private static final BCryptPasswordEncoder TIMING_BCRYPT = new BCryptPasswordEncoder();
	private static final String TIMING_HASH = TIMING_BCRYPT.encode("rms-timing-only");

	@Override
	public String encode(CharSequence rawPassword) {
		throw new UnsupportedOperationException("RMS doesn't store plain-text passwords");
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (rawPassword == null || encodedPassword == null) {
			return false;
		}
		TIMING_BCRYPT.matches(rawPassword, TIMING_HASH);
		return MessageDigest.isEqual(rawPassword.toString().getBytes(StandardCharsets.UTF_8),
				encodedPassword.getBytes(StandardCharsets.UTF_8));
	}

}
