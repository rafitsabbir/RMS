package rms.model;

/** Where a candidate came from (candidate.source holds the name; NULL is "not recorded"). */
public enum CandidateSource {
	REFERRAL("Referral"),
	JOB_BOARD("Job board"),
	WEBSITE("Website"),
	AGENCY("Agency"),
	WALK_IN("Walk-in"),
	OTHER("Other");

	private final String label;

	CandidateSource(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/** For the JSPs, which can't call name(). */
	public String getName() {
		return name();
	}

	/** The source with this name, or null for anything else. */
	public static CandidateSource parse(String name) {
		if (name != null) {
			for (CandidateSource source : values()) {
				if (source.name().equals(name)) {
					return source;
				}
			}
		}
		return null;
	}
}
