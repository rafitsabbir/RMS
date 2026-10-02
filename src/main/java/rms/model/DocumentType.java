package rms.model;

/**
 * The kinds of candidate document (candidate_document.doctype holds the name). Only the CV is required; the
 * other single-slot types are optional and count towards the "n/6" completeness. A candidate has at most
 * maxActive active documents of a type: uploading another single-slot document replaces the active one, and a
 * sixth professional certificate is refused.
 */
public enum DocumentType {
	CV("CV", 1),
	SSC("SSC certificate", 1),
	HSC("HSC certificate", 1),
	BSC("BSc / Honours certificate", 1),
	MASTERS("Masters certificate", 1),
	PHD("PhD certificate", 1),
	PROFESSIONAL("Professional certificate", 5);

	/** The single-slot types: the denominator of "CV, n/6". */
	public static final int SLOT_TYPES = 6;

	private final String label;
	private final int maxActive;

	DocumentType(String label, int maxActive) {
		this.label = label;
		this.maxActive = maxActive;
	}

	public String getLabel() {
		return label;
	}

	public int getMaxActive() {
		return maxActive;
	}

	/** One active document at most: uploading again replaces it. */
	public boolean isSingleSlot() {
		return maxActive == 1;
	}

	/** For the JSPs, which can't call name(). */
	public String getName() {
		return name();
	}

	/** The type with this name, or null for anything else (null, blank, unknown or wrong case). */
	public static DocumentType parse(String name) {
		if (name != null) {
			for (DocumentType type : values()) {
				if (type.name().equals(name)) {
					return type;
				}
			}
		}
		return null;
	}
}
