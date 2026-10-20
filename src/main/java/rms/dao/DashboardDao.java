package rms.dao;

import java.util.List;
import java.util.Map;

import rms.model.HiringGroup;

/** Counts for the home page dashboard (Phase 4). Read-only; every count is over active rows only. */
public interface DashboardDao {

	/** Active candidates by status code (S, R, H in upper case, trimmed); the key is "" for NULL or blank. */
	public Map<String, Integer> countCandidatesByStatus();

	/**
	 * Pairs of an assigned, active interviewer and a candidate with no final decision (not Selected or Rejected)
	 * that the interviewer has not evaluated yet. One interviewer's, or everyone's when interviewerid is null.
	 */
	public int countPendingEvaluations(String interviewerid);

	/** The pending evaluations of countPendingEvaluations, per interviewer (label "Name (U2)", total), most first. */
	public List<HiringGroup> countPendingEvaluationsByInterviewer();

	/** Open jobs: status OPEN, not deleted. */
	public int countOpenJobs();

	/** Active candidates with no current CV (none uploaded, or deleted, or its file removed). */
	public int countCandidatesWithoutCv();

	/** Candidates with no final decision that have an assigned interviewer who is deactivated or no longer an Interviewer. */
	public int countCandidatesWithInactiveInterviewer();
}
