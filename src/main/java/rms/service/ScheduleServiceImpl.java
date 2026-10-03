package rms.service;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.AssignmentDao;
import rms.dao.ScheduleDao;
import rms.model.AssignmentInfo;
import rms.model.ScheduleInfo;

/** Interview schedule (Phase 4). Logs carry only keys, never names or locations. */
@Service
public class ScheduleServiceImpl implements ScheduleService {
	private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);

	ScheduleDao scheduledao;
	AssignmentDao assignmentdao;

	ActivityService activityservice;

	
	public void setActivityService(ActivityService activityservice) {
		this.activityservice = activityservice;
	}

	@Autowired
	public void setScheduleDao(ScheduleDao scheduledao) {
		this.scheduledao = scheduledao;
	}

	@Autowired
	public void setAssignmentDao(AssignmentDao assignmentdao) {
		this.assignmentdao = assignmentdao;
	}

	@Override
	public List<ScheduleInfo> getAllSchedule() {
		return scheduledao.getAllSchedule();
	}

	@Override
	public List<ScheduleInfo> getScheduleOf(String interviewerid) {
		return scheduledao.getScheduleOf(interviewerid);
	}

	@Override
	public ScheduleInfo findScheduleById(int schedulekey) {
		return scheduledao.findScheduleById(schedulekey);
	}

	@Override
	public List<AssignmentInfo> getSchedulableInterviewers(String candidateid) {
		return assignmentdao.getAssignments(candidateid).stream().filter(AssignmentInfo::isIntervieweractive).toList();
	}

	@Override
	// synchronized: the conflict check and the write must not interleave with another save. One Tomcat only; on two
	// nodes two HR users could still book the same interviewer at the same minute, which is shown, not prevented
	public synchronized Outcome save(ScheduleInfo scheduleinfo, String userid) {
		boolean update = scheduleinfo.getSchedulekey() > 0;
		String interviewerid = scheduleinfo.getInterviewerid();
		if (interviewerid == null || interviewerid.isBlank()) {
			return Outcome.refused("Please choose an interviewer.");
		}
		if (scheduleinfo.getStartat() == null) {
			return Outcome.refused("Please enter the date and time of the interview.");
		}
		String location = scheduleinfo.getLocation() == null ? "" : scheduleinfo.getLocation().trim();
		if (location.length() > ScheduleInfo.MAX_LOCATION) {
			return Outcome.refused("Please keep the location to " + ScheduleInfo.MAX_LOCATION + " characters.");
		}
		if (!EnglishText.isEnglish(location)) {
			return Outcome.refused("The location can use English only. " + EnglishText.PROBLEM);
		}
		scheduleinfo.setLocation(location.isEmpty() ? null : location);
		// A new interview is always SCHEDULED; a change may mark it DONE. Cancelling has its own action
		if (!update) {
			scheduleinfo.setStatus(ScheduleInfo.SCHEDULED);
		} else if (!ScheduleInfo.SCHEDULED.equals(scheduleinfo.getStatus())
				&& !ScheduleInfo.DONE.equals(scheduleinfo.getStatus())) {
			return Outcome.refused("Please choose whether the interview is scheduled or done.");
		}
		// Whole minutes only: the double-booking check compares the exact time
		scheduleinfo.setStartat(scheduleinfo.getStartat().truncatedTo(ChronoUnit.MINUTES));
		ScheduleInfo stored = update ? scheduledao.findScheduleById(scheduleinfo.getSchedulekey()) : null;
		if (update && stored == null) {
			return Outcome.refused("That interview was cancelled or removed, so it can't be changed.");
		}
		// A new or changed interviewer must be assigned to the candidate now, with an active account (the list the form
		// offers). A change that keeps the stored interviewer needs no check, so an interview can still be marked Done
		// after its interviewer was unassigned or deactivated
		boolean unchanged = update && interviewerid.equals(stored.getInterviewerid());
		boolean allowed = unchanged || assignmentdao.getAssignments(scheduleinfo.getCandidateid()).stream()
				.anyMatch(a -> interviewerid.equals(a.getInterviewerid()) && a.isIntervieweractive());
		if (!allowed) {
			return Outcome.refused("Please choose an active interviewer who is assigned to this candidate.");
		}
		if (ScheduleInfo.SCHEDULED.equals(scheduleinfo.getStatus())
				&& scheduledao.hasConflict(interviewerid, scheduleinfo.getStartat(), scheduleinfo.getSchedulekey())) {
			return Outcome.refused("That interviewer already has an interview at that time.");
		}
		if (update) {
			if (!scheduledao.updateSchedule(scheduleinfo, userid)) {
				return Outcome.refused("That interview was cancelled or removed, so it can't be changed.");
			}
			activityservice.record(userid, "INTERVIEW_CHANGED", "INTERVIEW", String.valueOf(scheduleinfo.getSchedulekey()),
					"candidate " + scheduleinfo.getCandidateid() + ", status " + scheduleinfo.getStatus());
			log.info("Interview {} changed by {}", scheduleinfo.getSchedulekey(), userid);
			return Outcome.ok("Interview updated.");
		}
		scheduledao.addSchedule(scheduleinfo, userid);
		activityservice.record(userid, "INTERVIEW_SCHEDULED", "CANDIDATE", scheduleinfo.getCandidateid(),
				"interviewer " + interviewerid);
		log.info("Interview of candidate {} with interviewer {} scheduled by {}", scheduleinfo.getCandidateid(),
				interviewerid, userid);
		return Outcome.ok("Interview scheduled.");
	}

	@Override
	public Outcome cancel(int schedulekey, String userid) {
		if (!scheduledao.cancelSchedule(schedulekey, userid)) {
			return Outcome.refused("Only a scheduled interview can be cancelled.");
		}
		activityservice.record(userid, "INTERVIEW_CANCELLED", "INTERVIEW", String.valueOf(schedulekey), null);
		log.info("Interview {} cancelled by {}", schedulekey, userid);
		return Outcome.ok("Interview cancelled.");
	}
}
