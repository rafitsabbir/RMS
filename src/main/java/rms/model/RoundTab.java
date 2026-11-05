package rms.model;

/** One round on the evaluation form's tabs (F2): its code ("" for no round), label, and whether it is done or shown. */
public class RoundTab {

	private final String code;
	private final String label;
	private final boolean evaluated;
	private final boolean current;

	public RoundTab(String code, String label, boolean evaluated, boolean current) {
		this.code = code;
		this.label = label;
		this.evaluated = evaluated;
		this.current = current;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public boolean isEvaluated() {
		return evaluated;
	}

	public boolean isCurrent() {
		return current;
	}
}
