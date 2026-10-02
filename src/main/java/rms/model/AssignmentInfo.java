package rms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An interviewer's assignment to a candidate (candidate_interviewer, Phase 3), with names and flags read through
 * joins and subqueries for display. On My Evaluations a row is one of the interviewer's candidates: assigned now,
 * or evaluated earlier and unassigned since.
 */
public class AssignmentInfo {

	private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private int assignmentkey = 0;
	private String candidateid = null;
	private String candidatefirstname = null;
	private String candidatelastname = null;
	private String positionname = null;
	private String candidatestatus = null;
	private String interviewerid = null;
	private String interviewername = null;
	/** False when the interviewer's account is deactivated: shown as "interviewer inactive" so HR can reassign. */
	private boolean intervieweractive = true;
	/** Whether the assignment is active (always true on the profile; false on My Evaluations once unassigned). */
	private boolean assigned = true;
	private boolean evaluated = false;
	private LocalDateTime assignedat = null;

	public String getStatuslabel() {
		return DecisionStatus.labelOf(candidatestatus);
	}

	/** Selected or Rejected: the evaluation can't be changed. */
	public boolean isLocked() {
		return DecisionStatus.locks(candidatestatus);
	}

	public String getAssignedlabel() {
		return assignedat == null ? "" : assignedat.format(LABEL);
	}

	/** Keys and flags only. */
	@Override
	public String toString() {
		return "AssignmentInfo [assignmentkey=" + assignmentkey + ", candidateid=" + candidateid + ", interviewerid="
				+ interviewerid + ", assigned=" + assigned + ", evaluated=" + evaluated + "]";
	}

	public int getAssignmentkey() {
		return assignmentkey;
	}

	public void setAssignmentkey(int assignmentkey) {
		this.assignmentkey = assignmentkey;
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getCandidatefirstname() {
		return candidatefirstname;
	}

	public void setCandidatefirstname(String candidatefirstname) {
		this.candidatefirstname = candidatefirstname;
	}

	public String getCandidatelastname() {
		return candidatelastname;
	}

	public void setCandidatelastname(String candidatelastname) {
		this.candidatelastname = candidatelastname;
	}

	public String getPositionname() {
		return positionname;
	}

	public void setPositionname(String positionname) {
		this.positionname = positionname;
	}

	public String getCandidatestatus() {
		return candidatestatus;
	}

	public void setCandidatestatus(String candidatestatus) {
		this.candidatestatus = candidatestatus;
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

	public boolean isIntervieweractive() {
		return intervieweractive;
	}

	public void setIntervieweractive(boolean intervieweractive) {
		this.intervieweractive = intervieweractive;
	}

	public boolean isAssigned() {
		return assigned;
	}

	public void setAssigned(boolean assigned) {
		this.assigned = assigned;
	}

	public boolean isEvaluated() {
		return evaluated;
	}

	public void setEvaluated(boolean evaluated) {
		this.evaluated = evaluated;
	}

	public LocalDateTime getAssignedat() {
		return assignedat;
	}

	public void setAssignedat(LocalDateTime assignedat) {
		this.assignedat = assignedat;
	}

}
