package rms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.CandidateDao;
import rms.model.CandidateInfo;

@Service
public class CandidateServiceImpl implements CandidateService {

	CandidateDao candidatedao;

	@Autowired
	public void setCandidateDao(CandidateDao candidatedao) {
		this.candidatedao = candidatedao;
	}

	@Override
	public boolean addCandidate(CandidateInfo candidateinfo) {
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

	@Override
	public CandidateInfo findCandidateById(String candidateid) {
		return candidatedao.findCandidateById(candidateid);
	}

}
