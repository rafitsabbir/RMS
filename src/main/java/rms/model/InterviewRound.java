package rms.model;

import java.util.Locale;

/**
 * The round an interview and its evaluation belong to (interview_schedule.interviewround and marks.interviewround,
 * migration 009; F2, 2026-11-05). Optional: rows from before F2, and interviews scheduled without one, have none
 * (NULL), which shows as "No round". An interviewer has one evaluation per candidate and round, so a candidate can be
 * evaluated again by the same interviewer in a later round. A stored text this version doesn't know reads as no round.
 */
public enum InterviewRound {
	FIRST("First interview"),
	TECHNICAL("Technical interview"),
	FINAL("Final interview");

	/** What a missing round shows as. */
	public static final String NO_ROUND = "No round";

	private final String label;

	InterviewRound(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/** For the JSPs, which can't call name(). */
	public String getName() {
		return name();
	}

	/** The round named exactly so (any case, trimmed), or null for NULL, blank and anything else. */
	public static InterviewRound parse(String text) {
		if (text != null) {
			String code = text.trim().toUpperCase(Locale.ROOT);
			for (InterviewRound round : values()) {
				if (round.name().equals(code)) {
					return round;
				}
			}
		}
		return null;
	}

	/** The label of a stored code, or "No round". */
	public static String label(String stored) {
		InterviewRound round = parse(stored);
		return round == null ? NO_ROUND : round.getLabel();
	}

	/** The code to store for a posted value: the round's name, or null for none. */
	public static String code(String posted) {
		InterviewRound round = parse(posted);
		return round == null ? null : round.name();
	}
}
