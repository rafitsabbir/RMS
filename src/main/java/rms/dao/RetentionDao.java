package rms.dao;

import java.time.LocalDate;
import java.util.List;

import rms.model.RetentionInfo;

/** The retention clean-up (F11, migration 012). */
public interface RetentionDao {
	/**
	 * Candidates, active or deleted, not Hired and not cleaned up yet, whose last activity (applied, decided, a stage
	 * change, an interview, an evaluation, an upload or an offer) is before the given day. Candidates without any of
	 * these dates aren't listed. Oldest first.
	 */
	public List<RetentionInfo> getDue(LocalDate before);

	/**
	 * Removes the candidate's personal data in one transaction and marks them cleaned up (and deleted); the rows stay
	 * for the counts. False when they were cleaned up already.
	 */
	public boolean anonymize(String candidateid, String userid);
}
