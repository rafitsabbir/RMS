package rms.dao;

import java.util.List;

import rms.model.StageInfo;

/** A candidate's hiring stage (candidate.stage) and its history (candidate_stage), migration 008. */
public interface StageDao {

	/** The stored stage of an active candidate (may be NULL or any text), or null when there is no such candidate. */
	public String getStage(String candidateid);

	/**
	 * Moves an active candidate from the stage stored now (expected, compared NULL-safely) to another, and keeps the
	 * change in the history, in one transaction. False when the candidate is gone, its stage changed meanwhile, or its
	 * decision doesn't allow the move (Rejected; Offer or Hired without Selected), checked on the locked row.
	 */
	public boolean moveStage(String candidateid, String expected, String tostage, String note, String userid);

	/** The changes of one candidate, newest first. */
	public List<StageInfo> getStages(String candidateid);
}
