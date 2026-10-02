package rms.model;

import java.util.Locale;

/**
 * A candidate's decision (candidate.candidatestatus, candidate_decision.status), set by Super Admin or HR, never
 * automatically. No value (NULL or anything else) is "Pending". Selected and Rejected are final: they lock the
 * candidate's evaluations; On hold doesn't. A decision can be changed later; every change is logged.
 */
public enum DecisionStatus {
	S("Selected", true),
	R("Rejected", true),
	H("On hold", false);

	public static final String PENDING = "Pending";

	private final String label;
	private final boolean locking;

	DecisionStatus(String label, boolean locking) {
		this.label = label;
		this.locking = locking;
	}

	public String getLabel() {
		return label;
	}

	/** Selected or Rejected: the candidate's evaluations can't be changed any more. */
	public boolean isLocking() {
		return locking;
	}

	/** For the JSPs, which can't call name(). */
	public String getName() {
		return name();
	}

	/** The status stored as this text, in any case and trimmed (older rows may be lower case), or null. */
	public static DecisionStatus parse(String stored) {
		if (stored != null) {
			String code = stored.trim().toUpperCase(Locale.ROOT);
			for (DecisionStatus status : values()) {
				if (status.name().equals(code)) {
					return status;
				}
			}
		}
		return null;
	}

	/** The label of a stored status, or "Pending". */
	public static String labelOf(String stored) {
		DecisionStatus status = parse(stored);
		return status == null ? PENDING : status.label;
	}

	/** Whether a stored status locks the evaluations. */
	public static boolean locks(String stored) {
		DecisionStatus status = parse(stored);
		return status != null && status.locking;
	}
}
