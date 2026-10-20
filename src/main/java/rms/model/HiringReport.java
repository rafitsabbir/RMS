package rms.model;

import java.util.List;

/** The hiring report (Reports page and its CSV): computed on every visit from the active candidates. */
public class HiringReport {

	private HiringGroup overall = new HiringGroup("All candidates");
	private List<HiringGroup> byjob = List.of();
	private List<HiringGroup> bysource = List.of();
	/** Interviewers with evaluations still to do (label = name, total = the count), most first. */
	private List<HiringGroup> pendingbyinterviewer = List.of();
	private DecisionTime toselected = new DecisionTime();
	private DecisionTime torejected = new DecisionTime();

	public HiringGroup getOverall() {
		return overall;
	}

	public void setOverall(HiringGroup overall) {
		this.overall = overall;
	}

	public List<HiringGroup> getByjob() {
		return byjob;
	}

	public void setByjob(List<HiringGroup> byjob) {
		this.byjob = byjob;
	}

	public List<HiringGroup> getBysource() {
		return bysource;
	}

	public void setBysource(List<HiringGroup> bysource) {
		this.bysource = bysource;
	}

	public List<HiringGroup> getPendingbyinterviewer() {
		return pendingbyinterviewer;
	}

	public void setPendingbyinterviewer(List<HiringGroup> pendingbyinterviewer) {
		this.pendingbyinterviewer = pendingbyinterviewer;
	}

	public DecisionTime getToselected() {
		return toselected;
	}

	public void setToselected(DecisionTime toselected) {
		this.toselected = toselected;
	}

	public DecisionTime getTorejected() {
		return torejected;
	}

	public void setTorejected(DecisionTime torejected) {
		this.torejected = torejected;
	}
}
