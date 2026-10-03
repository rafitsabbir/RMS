package rms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The figures on the home page (Phase 4), for one role. Staff (Super Admin, HR, Hiring Manager) get all of them,
 * counted over the whole system; an Interviewer gets only their own pending evaluations and upcoming interviews
 * (the other fields stay at their defaults and the page doesn't show them).
 */
public class DashboardInfo {

	/** Whether the figures are an interviewer's own (the staff-only fields are then not filled in). */
	private boolean personal = false;
	private int selected = 0;
	private int rejected = 0;
	private int onhold = 0;
	/** Active candidates with no decision (NULL, or a status RMS doesn't know). */
	private int pending = 0;
	/** Assigned active interviewers who haven't evaluated yet a candidate with no final decision. */
	private int pendingevaluations = 0;
	private int upcominginterviews = 0;
	/** The next few upcoming interviews, soonest first. */
	private List<ScheduleInfo> nextinterviews = new ArrayList<ScheduleInfo>();
	private int openjobs = 0;
	private int candidateswithoutcv = 0;
	/** Candidates with no final decision that have an assigned interviewer who is no longer active. */
	private int candidateswithinactiveinterviewer = 0;

	public int getTotalcandidates() {
		return selected + rejected + onhold + pending;
	}

	public boolean isPersonal() {
		return personal;
	}

	public void setPersonal(boolean personal) {
		this.personal = personal;
	}

	public int getSelected() {
		return selected;
	}

	public void setSelected(int selected) {
		this.selected = selected;
	}

	public int getRejected() {
		return rejected;
	}

	public void setRejected(int rejected) {
		this.rejected = rejected;
	}

	public int getOnhold() {
		return onhold;
	}

	public void setOnhold(int onhold) {
		this.onhold = onhold;
	}

	public int getPending() {
		return pending;
	}

	public void setPending(int pending) {
		this.pending = pending;
	}

	public int getPendingevaluations() {
		return pendingevaluations;
	}

	public void setPendingevaluations(int pendingevaluations) {
		this.pendingevaluations = pendingevaluations;
	}

	public int getUpcominginterviews() {
		return upcominginterviews;
	}

	public void setUpcominginterviews(int upcominginterviews) {
		this.upcominginterviews = upcominginterviews;
	}

	public List<ScheduleInfo> getNextinterviews() {
		return nextinterviews;
	}

	public void setNextinterviews(List<ScheduleInfo> nextinterviews) {
		this.nextinterviews = nextinterviews;
	}

	public int getOpenjobs() {
		return openjobs;
	}

	public void setOpenjobs(int openjobs) {
		this.openjobs = openjobs;
	}

	public int getCandidateswithoutcv() {
		return candidateswithoutcv;
	}

	public void setCandidateswithoutcv(int candidateswithoutcv) {
		this.candidateswithoutcv = candidateswithoutcv;
	}

	public int getCandidateswithinactiveinterviewer() {
		return candidateswithinactiveinterviewer;
	}

	public void setCandidateswithinactiveinterviewer(int candidateswithinactiveinterviewer) {
		this.candidateswithinactiveinterviewer = candidateswithinactiveinterviewer;
	}
}
