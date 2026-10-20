package rms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One change of a candidate's hiring stage (candidate_stage, migration 008). changedbyname is read for display. */
public class StageInfo {

	private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private int stagekey = 0;
	private String candidateid = null;
	private String fromstage = null;
	private String tostage = null;
	private String note = null;
	private String changedby = null;
	private String changedbyname = null;
	private LocalDateTime changedat = null;

	/** No note: it can be personal data. */
	@Override
	public String toString() {
		return "StageInfo [stagekey=" + stagekey + ", candidateid=" + candidateid + ", fromstage=" + fromstage
				+ ", tostage=" + tostage + ", changedby=" + changedby + "]";
	}

	/** A NULL from-stage was Applied (from before migration 008's backfill). */
	public String getFromlabel() {
		return HiringStage.of(fromstage).getLabel();
	}

	public String getTolabel() {
		return HiringStage.of(tostage).getLabel();
	}

	public String getChangedlabel() {
		return changedat == null ? "" : changedat.format(LABEL);
	}

	public int getStagekey() {
		return stagekey;
	}

	public void setStagekey(int stagekey) {
		this.stagekey = stagekey;
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getFromstage() {
		return fromstage;
	}

	public void setFromstage(String fromstage) {
		this.fromstage = fromstage;
	}

	public String getTostage() {
		return tostage;
	}

	public void setTostage(String tostage) {
		this.tostage = tostage;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getChangedby() {
		return changedby;
	}

	public void setChangedby(String changedby) {
		this.changedby = changedby;
	}

	public String getChangedbyname() {
		return changedbyname;
	}

	public void setChangedbyname(String changedbyname) {
		this.changedbyname = changedbyname;
	}

	public LocalDateTime getChangedat() {
		return changedat;
	}

	public void setChangedat(LocalDateTime changedat) {
		this.changedat = changedat;
	}
}
