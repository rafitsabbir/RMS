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

	/**
	 * Other active candidates that may be the same person (same e-mail, phone digits or full name), for a warning
	 * before saving; empty when there is none. A phone with fewer than 6 digits isn't compared.
	 */
	public List<CandidateInfo> findPossibleDuplicates(CandidateInfo candidateinfo);

}
