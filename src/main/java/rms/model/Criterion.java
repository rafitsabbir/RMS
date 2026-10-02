package rms.model;

/**
 * The 10 evaluation criteria, in the order of the marks columns and the pages. Each is scored 1 to 10, so the
 * total is at most 100. field is the marks column and the MarksInfo property; commentfield its comment's.
 */
public enum Criterion {
	WORKEXP("workexp", "Work Experience"),
	TECHKNOWLEDGE("techknowledge", "Technical Knowledge"),
	LEADERSHIP("leadership", "Leadership Skill"),
	DECISION("decision", "Decision Making"),
	PROBSOLVING("probsolving", "Problem Solving Skill"),
	STRESS("stress", "Stress Tolerance"),
	EDUCATION("education", "Educational Background"),
	COMSKILL("comskill", "Communication Skill"),
	ATTITUDE("attitude", "Attitude"),
	PERSONALITY("personality", "Personality");

	public static final int MIN_SCORE = 1;
	public static final int MAX_SCORE = 10;

	private final String field;
	private final String label;

	Criterion(String field, String label) {
		this.field = field;
		this.label = label;
	}

	public String getField() {
		return field;
	}

	public String getCommentfield() {
		return field + "comment";
	}

	public String getLabel() {
		return label;
	}
}
