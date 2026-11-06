package rms.model;

import java.time.LocalDate;

/** A candidate due for the retention clean-up (F11): not hired, last activity older than the period. */
public class RetentionInfo {

	private String candidateid = null;
	private String name = null;
	private String stage = null;
	private String candidatestatus = null;
	private boolean active = true;
	private LocalDate lastactivity = null;

	public String getStagelabel() {
		return HiringStage.of(stage).getLabel();
	}

	public String getStatuslabel() {
		return DecisionStatus.labelOf(candidatestatus);
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public String getCandidatestatus() {
		return candidatestatus;
	}

	public void setCandidatestatus(String candidatestatus) {
		this.candidatestatus = candidatestatus;
	}

	/** False for a deleted candidate (they are due too). */
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public LocalDate getLastactivity() {
		return lastactivity;
	}

	public void setLastactivity(LocalDate lastactivity) {
		this.lastactivity = lastactivity;
	}
}
