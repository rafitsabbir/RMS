package rms.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.MarksDao;
import rms.model.Criterion;
import rms.model.MarksInfo;
import rms.model.ResultInfo;

/** Evaluations (Phase 3). Logs carry only keys: no scores or comments. */
@Service
public class MarksServiceImpl implements MarksService {
	private static final Logger log = LoggerFactory.getLogger(MarksServiceImpl.class);

	static final int MAX_COMMENTS = 1000;
	static final int MAX_CRITERION_COMMENT = 255;
	static final String NOT_SAVED = "This evaluation can't be saved any more: the candidate has been selected or "
			+ "rejected, or you are no longer assigned to them.";

	MarksDao marksdao;

	@Autowired
	public void setMarksDao(MarksDao marksdao) {
		this.marksdao = marksdao;
	}

	@Override
	public Outcome saveEvaluation(MarksInfo marksinfo, String interviewerid) {
		// 1 to 10 for every new or changed evaluation (decision E); older rows count as they are
		for (Criterion criterion : Criterion.values()) {
			int score = marksinfo.getScore(criterion);
			if (score < Criterion.MIN_SCORE || score > Criterion.MAX_SCORE) {
				return Outcome.refused("Please score every criterion from " + Criterion.MIN_SCORE + " to "
						+ Criterion.MAX_SCORE + ".");
			}
		}
		String comments = blankToNull(marksinfo.getComments());
		if (comments != null && comments.length() > MAX_COMMENTS) {
			return Outcome.refused("The overall comment can have at most " + MAX_COMMENTS + " characters.");
		}
		marksinfo.setComments(comments);
		for (Criterion criterion : Criterion.values()) {
			String comment = blankToNull(marksinfo.getComment(criterion));
			if (comment != null && comment.length() > MAX_CRITERION_COMMENT) {
				return Outcome.refused("The comment on " + criterion.getLabel() + " can have at most "
						+ MAX_CRITERION_COMMENT + " characters.");
			}
			marksinfo.setComment(criterion, comment);
		}
		marksinfo.setInterviewerid(interviewerid);

		int markkey = marksdao.saveMarks(marksinfo);
		if (markkey == 0) {
			return Outcome.refused(NOT_SAVED);
		}
		log.info("Evaluation {} of candidate {} saved by {}", markkey, marksinfo.getCandidateid(), interviewerid);
		return Outcome.ok("Evaluation saved.");
	}

	@Override
	public List<ResultInfo> getResults() {
		return marksdao.getResults();
	}

	@Override
	public List<MarksInfo> getEvaluations(String candidateid) {
		return marksdao.getEvaluations(candidateid);
	}

	@Override
	public MarksInfo findEvaluation(String candidateid, String interviewerid) {
		return marksdao.findEvaluation(candidateid, interviewerid);
	}

	private static String blankToNull(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}
}
