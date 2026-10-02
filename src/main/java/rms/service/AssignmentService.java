package rms.service;

import java.util.List;

import rms.model.AssignmentInfo;
import rms.model.UserInfo;

public interface AssignmentService {

	public List<AssignmentInfo> getAssignments(String candidateid);

	public boolean isAssigned(String candidateid, String interviewerid);

	public List<UserInfo> getAssignableInterviewers(String candidateid);

	/** Assigns an active Interviewer-role user who isn't assigned yet (the caller checks the candidate). */
	public Outcome assign(String candidateid, String interviewerid, String userid);

	public Outcome unassign(String candidateid, String interviewerid, String userid);

	public List<AssignmentInfo> getMyCandidates(String interviewerid);
}
