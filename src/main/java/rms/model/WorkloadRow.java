package rms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One interviewer's workload on the Reports page (F9): interviews per week, from this week on, and the evaluations
 * they still have to do.
 */
public class WorkloadRow {

	private final String interviewerid;
	private final String label;
	private final List<Integer> weeks = new ArrayList<Integer>();
	private int pending = 0;

	public WorkloadRow(String interviewerid, String label, int weekcount) {
		this.interviewerid = interviewerid;
		this.label = label;
		for (int i = 0; i < weekcount; i++) {
			weeks.add(0);
		}
	}

	public String getInterviewerid() {
		return interviewerid;
	}

	/** "Name (U2)", like the other per-interviewer rows. */
	public String getLabel() {
		return label;
	}

	/** Interviews (scheduled or done, not cancelled) per week: this week first. */
	public List<Integer> getWeeks() {
		return weeks;
	}

	public void addInterview(int week) {
		weeks.set(week, weeks.get(week) + 1);
	}

	public int getPending() {
		return pending;
	}

	public void setPending(int pending) {
		this.pending = pending;
	}

	public int getInterviews() {
		return weeks.stream().mapToInt(Integer::intValue).sum();
	}
}
