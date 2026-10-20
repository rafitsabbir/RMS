package rms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.CandidateDao;
import rms.model.CandidateFilter;
import rms.model.CandidateInfo;

@Service
public class CandidateServiceImpl implements CandidateService {

	static final int MIN_PHONE_DIGITS = 6;

	CandidateDao candidatedao;

	@Autowired
	public void setCandidateDao(CandidateDao candidatedao) {
		this.candidatedao = candidatedao;
	}

	@Override
	public String addCandidate(CandidateInfo candidateinfo) {
		return candidatedao.addCandidate(candidateinfo);
	}

	@Override
	public void updateCandidate(CandidateInfo candidateinfo) {
		candidatedao.updateCandidate(candidateinfo);
	}

	@Override
	public List<CandidateInfo> getAllCandidate() {
		return candidatedao.getAllCandidate();
	}

	/** Filtered here, not in SQL: the list is small (one row per candidate) and the rules stay in one place. */
	@Override
	public List<CandidateInfo> getCandidates(CandidateFilter filter) {
		List<CandidateInfo> all = candidatedao.getAllCandidate();
		return filter == null || !filter.isActive() ? all : all.stream().filter(filter::matches).toList();
	}

	@Override
	public List<CandidateInfo> getDeletedCandidates() {
		return candidatedao.getDeletedCandidates();
	}

	@Override
	public CandidateInfo findCandidateById(String candidateid) {
		return candidatedao.findCandidateById(candidateid);
	}

	@Override
	public void deleteCandidate(String candidateid) {
		candidatedao.deleteCandidate(candidateid);
	}

	@Override
	public List<CandidateInfo> findPossibleDuplicates(CandidateInfo candidateinfo) {
		return candidatedao.findPossibleDuplicates(candidateinfo, phoneDigits(candidateinfo.getPhone()));
	}

	/** The digits of a phone number, or null with fewer than 6 (too short to say it is the same number). */
	static String phoneDigits(String phone) {
		String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
		return digits.length() < MIN_PHONE_DIGITS ? null : digits;
	}

}
