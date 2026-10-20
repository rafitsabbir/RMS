package rms.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.CandidateDao;
import rms.dao.StageDao;
import rms.model.CandidateInfo;
import rms.model.DecisionStatus;
import rms.model.HiringStage;
import rms.model.StageInfo;

/**
 * The rules of the hiring stages (owner decision 2026-10-20, standard stages linked to the decision):
 * Offer and Hired need the decision Selected; nothing moves while the decision is Rejected (the pipeline is closed
 * until the decision changes); Hired and Withdrawn may be left again, for mistakes. Selected moves a candidate to
 * Offer (DecisionDaoImpl, in the decision's transaction); scheduling an interview moves Applied and Screening on to
 * Interview. Every change is kept in candidate_stage and logged as STAGE_CHANGED with the stage codes only.
 */
@Service
public class StageServiceImpl implements StageService {
	private static final Logger log = LoggerFactory.getLogger(StageServiceImpl.class);

	static final int MAX_NOTE = 500;

	StageDao stagedao;
	CandidateDao candidatedao;
	ActivityService activityservice;

	@Autowired
	public void setStageDao(StageDao stagedao) {
		this.stagedao = stagedao;
	}

	@Autowired
	public void setCandidateDao(CandidateDao candidatedao) {
		this.candidatedao = candidatedao;
	}

	@Autowired
	public void setActivityService(ActivityService activityservice) {
		this.activityservice = activityservice;
	}

	@Override
	public Outcome moveStage(String candidateid, String expected, String tostage, String note, String userid) {
		HiringStage target = HiringStage.parse(tostage);
		if (target == null) {
			return Outcome.refused("Please choose a stage.");
		}
		CandidateInfo candidate = candidateid == null ? null : candidatedao.findCandidateById(candidateid);
		if (candidate == null) {
			return Outcome.refused("The candidate no longer exists.");
		}
		DecisionStatus decision = DecisionStatus.parse(candidate.getCandidatestatus());
		if (decision == DecisionStatus.R) {
			return Outcome.refused("The candidate is Rejected, so the pipeline is closed. Change the decision first.");
		}
		if (target.needsSelected() && decision != DecisionStatus.S) {
			return Outcome.refused(target.getLabel() + " needs the decision Selected. Record Selected first.");
		}
		String text = note == null ? "" : note.trim();
		if (!text.isEmpty() && !EnglishText.isEnglishMultiline(text)) {
			return Outcome.refused("The note can use English only. " + EnglishText.PROBLEM);
		}
		if (text.length() > MAX_NOTE) {
			return Outcome.refused("The note can have at most " + MAX_NOTE + " characters.");
		}
		String stored = candidate.getStage();
		if (HiringStage.of(stored) == target) {
			return Outcome.refused("The candidate is already at " + target.getLabel() + ".");
		}
		// The form's stage must still be the stored one (both read as stages, so NULL and APPLIED match)
		if (expected != null && HiringStage.of(expected) != HiringStage.of(stored)) {
			return Outcome.refused("The stage was changed meanwhile; it is now " + HiringStage.of(stored).getLabel()
					+ ". Please check and try again.");
		}
		if (!stagedao.moveStage(candidateid, stored, target.name(), text, userid)) {
			return Outcome.refused("The stage was changed meanwhile. Please check and try again.");
		}
		record(userid, candidateid, stored, target);
		return Outcome.ok("Moved to " + target.getLabel() + ".");
	}

	@Override
	public void interviewScheduled(String candidateid, String userid) {
		CandidateInfo candidate = candidatedao.findCandidateById(candidateid);
		if (candidate == null || DecisionStatus.parse(candidate.getCandidatestatus()) == DecisionStatus.R) {
			return;
		}
		HiringStage current = HiringStage.of(candidate.getStage());
		if (current != HiringStage.APPLIED && current != HiringStage.SCREENING) {
			return;
		}
		// A failure here leaves the interview saved and the stage as it was: the move is a convenience
		try {
			if (stagedao.moveStage(candidateid, candidate.getStage(), HiringStage.INTERVIEW.name(), "Interview scheduled",
					userid)) {
				record(userid, candidateid, candidate.getStage(), HiringStage.INTERVIEW);
			}
		} catch (RuntimeException e) {
			log.warn("Stage of candidate {} not moved to Interview: {}", candidateid, e.getClass().getSimpleName());
		}
	}

	@Override
	public List<StageInfo> getStages(String candidateid) {
		return stagedao.getStages(candidateid);
	}

	@Override
	public void record(String userid, String candidateid, String from, HiringStage to) {
		activityservice.record(userid, "STAGE_CHANGED", "CANDIDATE", candidateid, HiringStage.of(from).name() + ">" + to.name());
	}
}
