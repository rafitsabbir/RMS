package rms.dao;

import java.util.List;

import rms.model.MarksInfo;
import rms.model.ResultInfo;

public interface MarksDao {

	/**
	 * Saves the interviewer's evaluation of the candidate (updates their existing one, or inserts one) and returns its
	 * markkey; 0 when, at that moment, the candidate is deleted or decided (Selected or Rejected) or the interviewer
	 * isn't assigned.
	 */
	public int saveMarks(MarksInfo marksinfo);

	/** Candidate Status: every active candidate with their evaluation count and averages. */
	public List<ResultInfo> getResults();

	/** A candidate's active evaluations, oldest first. */
	public List<MarksInfo> getEvaluations(String candidateid);

	/** The interviewer's active evaluation of the candidate (the oldest, if there are several), or null. */
	public MarksInfo findEvaluation(String candidateid, String interviewerid);
}
