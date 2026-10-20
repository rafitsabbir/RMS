package rms.service;

import java.util.List;

import rms.model.HiringStage;
import rms.model.StageInfo;

/** Hiring stages (owner decision 2026-10-20): moves by Super Admin and HR, the link to scheduling, and the history. */
public interface StageService {

	/**
	 * Moves a candidate by hand. expected is the stage the form showed (a stage changed meanwhile is refused, so a
	 * stale form can't undo a newer move).
	 */
	public Outcome moveStage(String candidateid, String expected, String tostage, String note, String userid);

	/** An interview was scheduled: a candidate at Applied or Screening moves to Interview (not when Rejected). */
	public void interviewScheduled(String candidateid, String userid);

	public List<StageInfo> getStages(String candidateid);

	/** The activity entry of a stage change (STAGE_CHANGED, the stage codes only), also for the decision's move. */
	public void record(String userid, String candidateid, String from, HiringStage to);
}
