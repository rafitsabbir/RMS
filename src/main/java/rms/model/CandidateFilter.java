package rms.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * The filters of the Candidates list and its CSV (GET parameters, so a filtered list can be bookmarked). Every filter
 * is optional; an empty one doesn't filter. They combine with AND.
 */
public class CandidateFilter {

	/** jobkey value for "candidates without a job". */
	public static final int NO_JOB = -1;
	/** status value for Pending (no decision, or a value this version doesn't know). */
	public static final String PENDING = "P";
	/** source value for "source not recorded". */
	public static final String NO_SOURCE = "NONE";

	/** 0: any job; NO_JOB: none; else that job. */
	private int jobkey = 0;
	/** A HiringStage name, or empty for any (2026-10-20). */
	private String stage = null;
	/** S, R, H, PENDING, or empty for any. */
	private String status = null;
	/** A CandidateSource name, NO_SOURCE, or empty for any. */
	private String source = null;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate appliedfrom = null;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate appliedto = null;
	private boolean nocv = false;
	private boolean notevaluated = false;

	/** Whether any filter is set. */
	public boolean isActive() {
		return jobkey != 0 || !blank(stage) || !blank(status) || !blank(source) || appliedfrom != null
				|| appliedto != null || nocv || notevaluated;
	}

	/** Whether the candidate passes every filter that is set. With an applied-date range, no date doesn't pass. */
	public boolean matches(CandidateInfo candidate) {
		if (jobkey == NO_JOB ? candidate.getJobkey() > 0 : jobkey > 0 && candidate.getJobkey() != jobkey) {
			return false;
		}
		if (!blank(stage) && HiringStage.of(candidate.getStage()) != HiringStage.parse(stage)) {
			return false;
		}
		if (!blank(status)) {
			DecisionStatus decision = DecisionStatus.parse(candidate.getCandidatestatus());
			if (PENDING.equals(status) ? decision != null : decision == null || !decision.name().equals(status)) {
				return false;
			}
		}
		if (!blank(source)) {
			String stored = blank(candidate.getSource()) ? NO_SOURCE : candidate.getSource();
			if (!source.equals(stored)) {
				return false;
			}
		}
		LocalDate applied = candidate.getApplieddate();
		if ((appliedfrom != null || appliedto != null) && applied == null) {
			return false;
		}
		if (appliedfrom != null && applied.isBefore(appliedfrom) || appliedto != null && applied.isAfter(appliedto)) {
			return false;
		}
		if (nocv && candidate.isHascv()) {
			return false;
		}
		return !notevaluated || candidate.getEvaluationcount() == 0;
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	public int getJobkey() {
		return jobkey;
	}

	public void setJobkey(int jobkey) {
		this.jobkey = jobkey;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public LocalDate getAppliedfrom() {
		return appliedfrom;
	}

	public void setAppliedfrom(LocalDate appliedfrom) {
		this.appliedfrom = appliedfrom;
	}

	public LocalDate getAppliedto() {
		return appliedto;
	}

	public void setAppliedto(LocalDate appliedto) {
		this.appliedto = appliedto;
	}

	public boolean isNocv() {
		return nocv;
	}

	public void setNocv(boolean nocv) {
		this.nocv = nocv;
	}

	public boolean isNotevaluated() {
		return notevaluated;
	}

	public void setNotevaluated(boolean notevaluated) {
		this.notevaluated = notevaluated;
	}
}
