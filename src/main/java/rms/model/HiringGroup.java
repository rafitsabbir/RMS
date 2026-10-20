package rms.model;

/**
 * One row of the hiring report (Reports page): the active candidates of one job, one source or one interviewer,
 * counted by decision, with the ones still missing a CV or an evaluation and the average days to Selected.
 */
public class HiringGroup {

	private String label = null;
	private int total = 0;
	private int pending = 0;
	private int onhold = 0;
	private int selected = 0;
	private int rejected = 0;
	private int nocv = 0;
	private int notevaluated = 0;
	/** Selected candidates with both dates, and their days from applying to the decision. */
	private int timedselected = 0;
	private long daystoselected = 0;

	public HiringGroup() {
	}

	public HiringGroup(String label) {
		this.label = label;
	}

	/** Counts one candidate into the row. */
	public void add(CandidateInfo candidate) {
		total++;
		DecisionStatus status = DecisionStatus.parse(candidate.getCandidatestatus());
		if (status == DecisionStatus.S) {
			selected++;
			Long days = DecisionTime.daysToDecision(candidate);
			if (days != null) {
				timedselected++;
				daystoselected += days;
			}
		} else if (status == DecisionStatus.R) {
			rejected++;
		} else if (status == DecisionStatus.H) {
			onhold++;
		} else {
			pending++;
		}
		if (!candidate.isHascv()) {
			nocv++;
		}
		if (candidate.getEvaluationcount() == 0) {
			notevaluated++;
		}
	}

	/** The average days from applying to Selected, one decimal, or null without a Selected candidate with both dates. */
	public Double getAveragedaystoselected() {
		return timedselected == 0 ? null : Math.round(10.0 * daystoselected / timedselected) / 10.0;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getPending() {
		return pending;
	}

	public int getOnhold() {
		return onhold;
	}

	public int getSelected() {
		return selected;
	}

	public int getRejected() {
		return rejected;
	}

	public int getNocv() {
		return nocv;
	}

	public int getNotevaluated() {
		return notevaluated;
	}
}
