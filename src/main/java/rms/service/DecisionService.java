package rms.service;

import java.util.List;

import rms.model.DecisionInfo;

public interface DecisionService {

	/**
	 * Checks and saves a decision (status S, R or H, a reason, a date not in the future), made by userid. Allowed
	 * without evaluations; evaluations is their number, for the note in the message.
	 */
	public Outcome saveDecision(DecisionInfo decision, String userid, int evaluations);

	public List<DecisionInfo> getDecisions(String candidateid);
}
