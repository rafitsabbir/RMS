package rms.service;

import java.nio.charset.StandardCharsets;

/** The rules for a new password, on Change password and Users and Roles alike. */
public final class PasswordRules {

	public static final int MIN_LENGTH = 8;
	/** bcrypt reads at most 72 bytes, and Spring Security refuses to encode a longer password. */
	public static final int MAX_BYTES = 72;

	private PasswordRules() {
	}

	/** The first problem with a new password and its confirmation, as a message for the form, or null. */
	public static String problem(String password, String confirm) {
		if (password == null || password.isEmpty()) {
			return "Please enter the new password.";
		}
		if (password.length() < MIN_LENGTH) {
			return "The password must have at least " + MIN_LENGTH + " characters.";
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
			return "The password is too long: at most " + MAX_BYTES
					+ " characters, and fewer with accented or non-Latin letters.";
		}
		if (!password.equals(confirm)) {
			return "The two passwords don't match.";
		}
		return null;
	}
}
