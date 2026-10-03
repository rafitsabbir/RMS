package rms.service;

import java.util.List;

import rms.model.AssignmentInfo;
import rms.model.ScheduleInfo;

public interface ScheduleService {

	public List<ScheduleInfo> getAllSchedule();

	public List<ScheduleInfo> getScheduleOf(String interviewerid);

	public ScheduleInfo findScheduleById(int schedulekey);

	/** The candidate's active assignments whose interviewer is active: who an interview can be scheduled with. */
	public List<AssignmentInfo> getSchedulableInterviewers(String candidateid);

	/**
	 * Adds (schedulekey 0: always SCHEDULED) or changes (SCHEDULED or DONE) an interview, after checking the
	 * interviewer, time, location and status. The caller checks that the candidate exists, and for a change sets
	 * the candidate from the stored row.
	 */
	public Outcome save(ScheduleInfo scheduleinfo, String userid);

	public Outcome cancel(int schedulekey, String userid);
}
