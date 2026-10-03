package rms.service;

/**
 * RMS accepts English text only (owner decision, 2026-10-03): printable ASCII, plus line breaks and tabs in
 * multi-line fields. Other scripts were stored as unreadable codes (G34), so they are refused instead.
 */
public final class EnglishText {

	public static final String PROBLEM = "Please use English letters, digits and common punctuation only.";

	private EnglishText() {
	}

	/** True for null or empty text, and for text of printable ASCII characters only (no line breaks). */
	public static boolean isEnglish(String text) {
		return check(text, false);
	}

	/** Like {@link #isEnglish}, but line breaks and tabs are fine (comments and reasons). */
	public static boolean isEnglishMultiline(String text) {
		return check(text, true);
	}

	private static boolean check(String text, boolean multiline) {
		if (text == null) {
			return true;
		}
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			boolean printable = c >= 0x20 && c <= 0x7E;
			boolean whitespace = c == '\n' || c == '\r' || c == '\t';
			if (!printable && !(multiline && whitespace)) {
				return false;
			}
		}
		return true;
	}
}
