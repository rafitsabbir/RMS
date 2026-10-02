package rms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * One row of Candidate Status (Phase 3): an active candidate with the number of evaluations, the average of each
 * criterion over all of them, and the total of the averages. The averages are null without evaluations.
 */
public class ResultInfo {

	private String candidateid = null;
	private String firstname = null;
	private String lastname = null;
	private String positionname = null;
	private String languagename = null;
	private String candidatestatus = null;
	private int evaluations = 0;
	/** In Criterion order; null entries without evaluations. */
	private List<BigDecimal> averages = new ArrayList<BigDecimal>();

	/**
	 * The count and averages of these evaluations, as the Candidate Status query computes them (for the evaluation
	 * detail page, which has the rows already). The candidate fields stay empty.
	 */
	public static ResultInfo summarize(List<MarksInfo> evaluations) {
		ResultInfo result = new ResultInfo();
		result.setEvaluations(evaluations.size());
		List<BigDecimal> averages = new ArrayList<BigDecimal>();
		for (Criterion criterion : Criterion.values()) {
			if (evaluations.isEmpty()) {
				averages.add(null);
				continue;
			}
			long sum = 0;
			for (MarksInfo marks : evaluations) {
				sum += marks.getScore(criterion);
			}
			averages.add(BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(evaluations.size()), 4,
					RoundingMode.HALF_UP));
		}
		result.setAverages(averages);
		return result;
	}

	/** The averages rounded to one decimal, for display. */
	public List<BigDecimal> getRoundedaverages() {
		List<BigDecimal> rounded = new ArrayList<BigDecimal>();
		for (BigDecimal average : averages) {
			rounded.add(average == null ? null : average.setScale(1, RoundingMode.HALF_UP));
		}
		return rounded;
	}

	/** The total of the averages (of the unrounded values), to one decimal; null without evaluations. */
	public BigDecimal getTotal() {
		if (evaluations == 0) {
			return null;
		}
		BigDecimal total = BigDecimal.ZERO;
		for (BigDecimal average : averages) {
			total = total.add(average == null ? BigDecimal.ZERO : average);
		}
		return total.setScale(1, RoundingMode.HALF_UP);
	}

	public String getStatuslabel() {
		return DecisionStatus.labelOf(candidatestatus);
	}

	/** No names: they are personal data. */
	@Override
	public String toString() {
		return "ResultInfo [candidateid=" + candidateid + ", evaluations=" + evaluations + ", averages=" + averages
				+ "]";
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getPositionname() {
		return positionname;
	}

	public void setPositionname(String positionname) {
		this.positionname = positionname;
	}

	public String getLanguagename() {
		return languagename;
	}

	public void setLanguagename(String languagename) {
		this.languagename = languagename;
	}

	public String getCandidatestatus() {
		return candidatestatus;
	}

	public void setCandidatestatus(String candidatestatus) {
		this.candidatestatus = candidatestatus;
	}

	public int getEvaluations() {
		return evaluations;
	}

	public void setEvaluations(int evaluations) {
		this.evaluations = evaluations;
	}

	public List<BigDecimal> getAverages() {
		return averages;
	}

	public void setAverages(List<BigDecimal> averages) {
		this.averages = averages;
	}

}
