package rms.service;

import java.util.List;

import rms.model.RetentionInfo;

/**
 * Data retention (F11, owner decision 2026-11-05: configurable, off by default; a Super Admin confirms every
 * clean-up). The period comes from RMS_RETENTION_MONTHS; without it nothing is ever due.
 */
public interface RetentionService {

	/** The period in months, or 0 when retention is off. */
	public int getMonths();

	/** The candidates due for the clean-up now; empty when retention is off. */
	public List<RetentionInfo> getDue();

	/**
	 * Cleans up the chosen candidates that are still due: deletes their document files, then removes their personal
	 * data. A candidate whose files can't be deleted is skipped (nothing of theirs is removed).
	 */
	public Outcome cleanUp(List<String> candidateids, String userid);
}
