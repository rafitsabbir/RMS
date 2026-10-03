package rms.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.DecisionDao;
import rms.model.DecisionInfo;
import rms.model.DecisionStatus;

/**
 * Selected / Rejected / On hold, set by Super Admin or HR only, never automatically (owner decision). Every change is
 * kept in candidate_decision. Logs carry only keys: no status or reason.
 */
@Service
public class DecisionServiceImpl implements DecisionService {
	private static final Logger log = LoggerFactory.getLogger(DecisionServiceImpl.class);

	static final int MAX_REASON = 500;
	/** No decision dated before this; the date can't be in the future either. */
	static final LocalDate FIRST_DATE = LocalDate.of(2000, 1, 1);

	DecisionDao decisiondao;

	ActivityService activityservice;

	
	public void setActivityService(ActivityService activityservice) {
		this.activityservice = activityservice;
	}

	@Autowired
	public void setDecisionDao(DecisionDao decisiondao) {
		this.decisiondao = decisiondao;
	}

	@Override
	public Outcome saveDecision(DecisionInfo decision, String userid, int evaluations) {
		DecisionStatus status = DecisionStatus.parse(decision.getStatus());
		if (status == null) {
			return Outcome.refused("Please choose Selected, Rejected or On hold.");
		}
		String reason = decision.getReason() == null ? "" : decision.getReason().trim();
		if (reason.isEmpty()) {
			return Outcome.refused("Please give the reason for the decision.");
		}
		if (!EnglishText.isEnglishMultiline(reason)) {
			return Outcome.refused("The reason can use English only. " + EnglishText.PROBLEM);
		}
		if (reason.length() > MAX_REASON) {
			return Outcome.refused("The reason can have at most " + MAX_REASON + " characters.");
		}
		if (decision.getDecisiondate() == null) {
			return Outcome.refused("Please enter the date of the decision.");
		}
		if (decision.getDecisiondate().isAfter(LocalDate.now())) {
			return Outcome.refused("The date of the decision can't be in the future.");
		}
		if (decision.getDecisiondate().isBefore(FIRST_DATE)) {
			return Outcome.refused("Please check the date of the decision.");
		}
		decision.setStatus(status.name());
		decision.setReason(reason);
		decision.setDecidedby(userid);

		if (!decisiondao.saveDecision(decision)) {
			return Outcome.refused("The candidate no longer exists.");
		}
		activityservice.record(userid, status.getLabel().toUpperCase(java.util.Locale.ROOT).replace(' ', '_'), "CANDIDATE",
				decision.getCandidateid(), null);
		log.info("Decision on candidate {} saved by {}", decision.getCandidateid(), userid);
		String message = "Decision saved: " + status.getLabel() + ".";
		if (status.isLocking()) {
			message += " The candidate's evaluations can no longer be changed.";
		}
		if (evaluations == 0) {
			message += " Note: there are no evaluations yet.";
		}
		return Outcome.ok(message);
	}

	@Override
	public List<DecisionInfo> getDecisions(String candidateid) {
		return decisiondao.getDecisions(candidateid);
	}
}
