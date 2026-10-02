package rms.service;

/**
 * The result of an action a service checks (an upload, a permanent delete, an evaluation, a decision, an assignment):
 * whether it worked, and the message for the page.
 */
public record Outcome(boolean done, String message) {

	public static Outcome ok(String message) {
		return new Outcome(true, message);
	}

	public static Outcome refused(String message) {
		return new Outcome(false, message);
	}
}
