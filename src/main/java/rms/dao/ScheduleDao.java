package rms.dao;

import java.time.LocalDateTime;
import java.util.List;

import rms.model.ScheduleInfo;

/**
 * Interviews (interview_schedule). Only active rows of active candidates are read. Cancelling sets status to
 * CANCELLED and keeps the row. Times are the MySQL server's wall-clock time (db/migrations/006).
 */
public interface ScheduleDao {

	/** Every interview, by start time (cancelled ones too, so the list shows what happened). */
	public List<ScheduleInfo> getAllSchedule();

	/** One interviewer's interviews, by start time. */
	public List<ScheduleInfo> getScheduleOf(String interviewerid);

	/** The interview, or null when there is none (or its candidate is deleted). */
	public ScheduleInfo findScheduleById(int schedulekey);

	/** Scheduled interviews starting now or later, soonest first, at most limit; of one interviewer, or of all when null. */
	public List<ScheduleInfo> getUpcoming(String interviewerid, int limit);

	/** How many scheduled interviews start now or later; of one interviewer, or of all when null. */
	public int countUpcoming(String interviewerid);

	/** Whether the interviewer has another SCHEDULED interview at exactly this start time (excludekey: 0 for a new one). */
	public boolean hasConflict(String interviewerid, LocalDateTime startat, int excludekey);

	/** Adds a SCHEDULED interview. */
	public void addSchedule(ScheduleInfo scheduleinfo, String userid);

	/** Changes interviewer, time, location and status (SCHEDULED or DONE) unless it is cancelled; false then. */
	public boolean updateSchedule(ScheduleInfo scheduleinfo, String userid);

	/** Cancels a SCHEDULED interview; false when it isn't scheduled (any more). */
	public boolean cancelSchedule(int schedulekey, String userid);
}
