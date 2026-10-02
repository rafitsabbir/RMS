package rms.dao;

import java.util.List;

import rms.model.AssignmentInfo;
import rms.model.UserInfo;

/** Interviewers assigned to candidates (candidate_interviewer). Unassigning sets isactive=0; rows are kept. */
public interface AssignmentDao {

	/** The candidate's active assignments, oldest first, with the interviewer's name and flags. */
	public List<AssignmentInfo> getAssignments(String candidateid);

	/** Whether the interviewer is assigned to the candidate now. */
	public boolean isAssigned(String candidateid, String interviewerid);

	/** Active users with the Interviewer role not assigned to the candidate yet, by name (userid and names only). */
	public List<UserInfo> getAssignableInterviewers(String candidateid);

	/** Assigns the interviewer; false when they are assigned already. */
	public boolean assign(String candidateid, String interviewerid, String assignedby);

	/** Ends the assignment; false when there was none. The interviewer's evaluation stays. */
	public boolean unassign(String candidateid, String interviewerid, String userid);

	/** My Evaluations: the active candidates the interviewer is assigned to or has evaluated, by candidate ID. */
	public List<AssignmentInfo> getMyCandidates(String interviewerid);
}
