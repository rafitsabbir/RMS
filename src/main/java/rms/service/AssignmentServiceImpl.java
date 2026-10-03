package rms.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.AssignmentDao;
import rms.model.AssignmentInfo;
import rms.model.UserInfo;

/** Interviewer assignments (Phase 3). Logs carry only candidate and user keys. */
@Service
public class AssignmentServiceImpl implements AssignmentService {
	private static final Logger log = LoggerFactory.getLogger(AssignmentServiceImpl.class);

	AssignmentDao assignmentdao;

	ActivityService activityservice;

	
	public void setActivityService(ActivityService activityservice) {
		this.activityservice = activityservice;
	}

	@Autowired
	public void setAssignmentDao(AssignmentDao assignmentdao) {
		this.assignmentdao = assignmentdao;
	}

	@Override
	public List<AssignmentInfo> getAssignments(String candidateid) {
		return assignmentdao.getAssignments(candidateid);
	}

	@Override
	public boolean isAssigned(String candidateid, String interviewerid) {
		return interviewerid != null && assignmentdao.isAssigned(candidateid, interviewerid);
	}

	@Override
	public List<UserInfo> getAssignableInterviewers(String candidateid) {
		return assignmentdao.getAssignableInterviewers(candidateid);
	}

	@Override
	public Outcome assign(String candidateid, String interviewerid, String userid) {
		// Only active Interviewer-role users (decision F) not assigned yet: the same list the form offers
		boolean assignable = interviewerid != null && assignmentdao.getAssignableInterviewers(candidateid).stream()
				.anyMatch(user -> interviewerid.equals(user.getUserid()));
		if (!assignable) {
			return Outcome.refused("Please choose an active interviewer who isn't assigned yet.");
		}
		if (!assignmentdao.assign(candidateid, interviewerid, userid)) {
			return Outcome.refused("That interviewer is assigned already.");
		}
		activityservice.record(userid, "ASSIGNED", "CANDIDATE", candidateid, "interviewer " + interviewerid);
		log.info("Interviewer {} assigned to candidate {} by {}", interviewerid, candidateid, userid);
		return Outcome.ok("Interviewer assigned.");
	}

	@Override
	public Outcome unassign(String candidateid, String interviewerid, String userid) {
		if (interviewerid == null || !assignmentdao.unassign(candidateid, interviewerid, userid)) {
			return Outcome.refused("That interviewer isn't assigned any more.");
		}
		activityservice.record(userid, "UNASSIGNED", "CANDIDATE", candidateid, "interviewer " + interviewerid);
		log.info("Interviewer {} unassigned from candidate {} by {}", interviewerid, candidateid, userid);
		return Outcome.ok("Interviewer unassigned. Their evaluation, if any, still counts.");
	}

	@Override
	public List<AssignmentInfo> getMyCandidates(String interviewerid) {
		return assignmentdao.getMyCandidates(interviewerid);
	}
}
