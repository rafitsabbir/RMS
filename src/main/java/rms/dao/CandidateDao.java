package rms.dao;

import java.util.List;

import rms.model.CandidateInfo;

public interface CandidateDao {

	/** Saves a new candidate under the next generated ID (C1, C2, ...) and returns that ID. */
	public String addCandidate(CandidateInfo candidateinfo);

	public void updateCandidate(CandidateInfo candidateinfo);

	public List<CandidateInfo> getAllCandidate();

	public CandidateInfo findCandidateById(String candidateid);
}
