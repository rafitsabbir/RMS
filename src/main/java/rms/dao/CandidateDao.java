package rms.dao;

import java.util.List;

import rms.model.CandidateInfo;

public interface CandidateDao {

	public boolean addCandidate(CandidateInfo candidateinfo);

	public void updateCandidate(CandidateInfo candidateinfo);

	public List<CandidateInfo> getAllCandidate();

	public CandidateInfo findCandidateById(String candidateid);
}
