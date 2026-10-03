package rms.service;

import java.util.List;

import rms.model.CandidateInfo;

public interface CandidateService {
	public String addCandidate(CandidateInfo candidateinfo);
	public void updateCandidate(CandidateInfo candidateinfo);
	public List<CandidateInfo> getAllCandidate();
	public List<CandidateInfo> getDeletedCandidates();
	public CandidateInfo findCandidateById(String candidateid);
	public void deleteCandidate(String candidateid);

}
