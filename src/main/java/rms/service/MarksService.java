package rms.service;

import java.util.List;

import rms.model.MarksInfo;
import rms.model.ResultInfo;

public interface MarksService {

	/**
	 * Checks and saves the interviewer's evaluation (scores 1 to 10, comment lengths). Refused when the candidate is
	 * decided (Selected or Rejected) or the interviewer isn't assigned at the moment of saving.
	 */
	public Outcome saveEvaluation(MarksInfo marksinfo, String interviewerid);

	/** Candidate Status: active candidates with their evaluation count and averages. */
	public List<ResultInfo> getResults();

	public List<MarksInfo> getEvaluations(String candidateid);

	public MarksInfo findEvaluation(String candidateid, String interviewerid);
}
