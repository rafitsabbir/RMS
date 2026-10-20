package rms.dao;

import java.util.List;

import rms.model.CandidateInfo;

public interface CandidateDao {

	/** Saves a new candidate under the next generated ID (C1, C2, ...) and returns that ID. */
	public String addCandidate(CandidateInfo candidateinfo);

	public void updateCandidate(CandidateInfo candidateinfo);

	public List<CandidateInfo> getAllCandidate();

	/** Deleted (inactive) candidates with the number of documents whose files are still stored (unpurgedcount). */
	public List<CandidateInfo> getDeletedCandidates();

	public CandidateInfo findCandidateById(String candidateid);

	/**
	 * Up to 10 other active candidates that may be the same person: the same e-mail (any case), the same phone
	 * digits (phonedigits; null to skip) or the same first and last name (any case), with the reason.
	 */
	public List<CandidateInfo> findPossibleDuplicates(CandidateInfo candidateinfo, String phonedigits);

	/** Soft delete: sets isactive=0; the row and its marks stay. */
	public void deleteCandidate(String candidateid);
}
