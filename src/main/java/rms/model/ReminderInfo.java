package rms.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A reminder on the home page (F10): an evaluation overdue after an interview, or a candidate long at one stage.
 * since is the interview's day, or the day the candidate reached the stage.
 */
public class ReminderInfo {

	private String candidateid = null;
	private String candidatename = null;
	private String interviewerid = null;
	private String interviewername = null;
	private String interviewround = null;
	private String stage = null;
	private LocalDate since = null;

	/** Days from since to the given day. */
	public long daysTo(LocalDate today) {
		return since == null ? 0 : ChronoUnit.DAYS.between(since, today);
	}

	/** Days from since to today, for the JSPs. */
	public long getDays() {
		return daysTo(LocalDate.now());
	}

	public String getStagelabel() {
		return HiringStage.of(stage).getLabel();
	}

	public String getRoundlabel() {
		return InterviewRound.label(interviewround);
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getCandidatename() {
		return candidatename;
	}

	public void setCandidatename(String candidatename) {
		this.candidatename = candidatename;
	}

	public String getInterviewerid() {
		return interviewerid;
	}

	public void setInterviewerid(String interviewerid) {
		this.interviewerid = interviewerid;
	}

	public String getInterviewername() {
		return interviewername;
	}

	public void setInterviewername(String interviewername) {
		this.interviewername = interviewername;
	}

	public String getInterviewround() {
		return interviewround;
	}

	public void setInterviewround(String interviewround) {
		this.interviewround = interviewround;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public LocalDate getSince() {
		return since;
	}

	public void setSince(LocalDate since) {
		this.since = since;
	}
}
