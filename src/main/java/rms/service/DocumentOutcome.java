package rms.service;

/** The result of an upload or a permanent delete: whether it worked, and the message for the profile page. */
public record DocumentOutcome(boolean done, String message) {

	static DocumentOutcome ok(String message) {
		return new DocumentOutcome(true, message);
	}

	static DocumentOutcome refused(String message) {
		return new DocumentOutcome(false, message);
	}
}
