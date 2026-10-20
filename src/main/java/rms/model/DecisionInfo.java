package rms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * A decision on a candidate (candidate_decision, Phase 3), and the decision form. decidedbyname is read through a
 * subquery for display.
 */
public class DecisionInfo {

	private int decisionkey = 0;
	private String candidateid = null;
	private String status = null;
	private String reason = null;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate decisiondate = null;
	private String decidedby = null;
	private String decidedbyname = null;
	private LocalDateTime decidedat = null;
	/** Set by DecisionDaoImpl.saveDecision: the stage after the decision, and whether the decision moved it. */
	private String stagenow = null;
	private boolean stagemoved = false;
	private String stagefrom = null;

	public String getStatuslabel() {
		return DecisionStatus.labelOf(status);
	}

	/** No reason: it can be personal data. */
	@Override
	public String toString() {
		return "DecisionInfo [decisionkey=" + decisionkey + ", candidateid=" + candidateid + ", status=" + status
				+ ", decisiondate=" + decisiondate + ", decidedby=" + decidedby + "]";
	}

	public int getDecisionkey() {
		return decisionkey;
	}

	public void setDecisionkey(int decisionkey) {
		this.decisionkey = decisionkey;
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDate getDecisiondate() {
		return decisiondate;
	}

	public void setDecisiondate(LocalDate decisiondate) {
		this.decisiondate = decisiondate;
	}

	public String getDecidedby() {
		return decidedby;
	}

	public void setDecidedby(String decidedby) {
		this.decidedby = decidedby;
	}

	public String getDecidedbyname() {
		return decidedbyname;
	}

	public void setDecidedbyname(String decidedbyname) {
		this.decidedbyname = decidedbyname;
	}

	public LocalDateTime getDecidedat() {
		return decidedat;
	}

	public void setDecidedat(LocalDateTime decidedat) {
		this.decidedat = decidedat;
	}


	public String getStagenow() {
		return stagenow;
	}

	public void setStagenow(String stagenow) {
		this.stagenow = stagenow;
	}

	public boolean isStagemoved() {
		return stagemoved;
	}

	public void setStagemoved(boolean stagemoved) {
		this.stagemoved = stagemoved;
	}

	public String getStagefrom() {
		return stagefrom;
	}

	public void setStagefrom(String stagefrom) {
		this.stagefrom = stagefrom;
	}
}
