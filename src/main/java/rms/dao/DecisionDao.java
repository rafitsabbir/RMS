package rms.dao;

import java.util.List;

import rms.model.DecisionInfo;

/** Decisions on candidates: the latest in the candidate row, every one in candidate_decision. */
public interface DecisionDao {

	/**
	 * Sets the candidate's status, reason, date and decider, and logs the decision, in one transaction (decidedat is
	 * set here). False when the candidate isn't active.
	 */
	public boolean saveDecision(DecisionInfo decision);

	/** The candidate's decisions, newest first, with the decider's name. */
	public List<DecisionInfo> getDecisions(String candidateid);
}
