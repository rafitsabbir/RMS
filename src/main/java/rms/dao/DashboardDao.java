package rms.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import rms.model.HiringGroup;
import rms.model.ReminderInfo;

/** Counts for the home page dashboard (Phase 4). Read-only; every count is over active rows only. */
public interface DashboardDao {

	/** Active candidates by stage code (upper case, trimmed); the key is "" for NULL or blank (read as Applied). */
	public Map<String, Integer> countCandidatesByStage();

	/** Active candidates by status code (S, R, H in upper case, trimmed); the key is "" for NULL or blank. */
	public Map<String, Integer> countCandidatesByStatus();

	/**
	 * Pairs of an assigned, active interviewer and a candidate with no final decision (not Selected or Rejected)
	 * that the interviewer has not evaluated yet. One interviewer's, or everyone's when interviewerid is null.
	 */
	public int countPendingEvaluations(String interviewerid);

	/** The pending evaluations of countPendingEvaluations, per interviewer (label "Name (U2)", total), most first. */
	public List<HiringGroup> countPendingEvaluationsByInterviewer();

	/** Active interviewers (F9): userid and "Name (U2)", by name; the Interviewer role or a staff role that also interviews. */
	public Map<String, String> getActiveInterviewers();

	/**
	 * Interviews that aren't cancelled, of active candidates, starting from (inclusive) to (exclusive), per
	 * interviewer and day (F9): interviewerid, then the day and its count.
	 */
	public Map<String, Map<LocalDate, Integer>> countInterviewsByDay(LocalDate from, LocalDate to);

	/**
	 * Overdue evaluations (F10): interviews (not cancelled) that started before the given time, of undecided active
	 * candidates, whose interviewer is still assigned and active but hasn't evaluated the candidate in that round (any
	 * evaluation counts for an interview without a round). One per candidate, interviewer and round, oldest first; of
	 * one interviewer, or of all when null.
	 */
	public List<ReminderInfo> getOverdueEvaluations(String interviewerid, LocalDateTime before);

	/**
	 * Candidates long at one stage (F10): active, not Rejected, not Hired or Withdrawn, who reached their stage before
	 * the given day (the last move into it, else the applied date for Applied; neither: not listed). Oldest first.
	 */
	public List<ReminderInfo> getStuckCandidates(LocalDate before);

	/** Open jobs: status OPEN, not deleted. */
	public int countOpenJobs();

	/** Active candidates with no current CV (none uploaded, or deleted, or its file removed). */
	public int countCandidatesWithoutCv();

	/** Candidates with no final decision that have an assigned interviewer who is deactivated or no longer an Interviewer. */
	public int countCandidatesWithInactiveInterviewer();
}
