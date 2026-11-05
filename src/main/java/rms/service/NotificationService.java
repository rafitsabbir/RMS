package rms.service;

/**
 * E-mails to staff about their work (F1, 2026-11-05). Staff only: RMS never e-mails candidates (owner decision). Every
 * method returns at once and never throws: the mail goes out in the background, and a mail problem is logged, never
 * shown to the user or allowed to undo the action.
 */
public interface NotificationService {

	/** No e-mail at all: the default in services until Spring wires the real one, and in tests. */
	NotificationService NONE = new NotificationService() {
	};

	/** The interviewer was assigned to the candidate. */
	default void interviewerAssigned(String candidateid, String interviewerid) {
	}

	/** A new interview: its interviewer gets the details and the calendar file. */
	default void interviewScheduled(int schedulekey) {
	}

	/**
	 * A changed interview: its interviewer gets the new details and the calendar file; a previous interviewer, when it
	 * changed, is told they no longer have it.
	 */
	default void interviewChanged(int schedulekey, String previousinterviewerid) {
	}

	/** A cancelled interview: its interviewer gets the cancellation and the calendar file that removes it. */
	default void interviewCancelled(int schedulekey) {
	}

	/** An interviewer saved an evaluation (in a round, or null): HR is told (no scores or comments in the mail). */
	default void evaluationSaved(String candidateid, String interviewerid, String interviewround) {
	}
}
