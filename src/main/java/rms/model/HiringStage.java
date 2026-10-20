package rms.model;

import java.util.List;
import java.util.Locale;

/**
 * Where a candidate is in the hiring process (candidate.stage, migration 008; owner decision 2026-10-20). Applied,
 * Screening, Interview, Offer and Hired are the pipeline in order; Withdrawn ends it at any point. A stored NULL or a
 * text this version doesn't know reads as Applied. Rejected and On hold are decisions (DecisionStatus), not stages:
 * a rejected candidate keeps the stage they reached ("Rejected at Interview").
 */
public enum HiringStage {
	APPLIED("Applied"),
	SCREENING("Screening"),
	INTERVIEW("Interview"),
	OFFER("Offer"),
	HIRED("Hired"),
	WITHDRAWN("Withdrawn");

	/** The pipeline in order, for the step bar: every stage but Withdrawn. */
	public static final List<HiringStage> PIPELINE = List.of(APPLIED, SCREENING, INTERVIEW, OFFER, HIRED);

	private final String label;

	HiringStage(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/** For the JSPs, which can't call name(). */
	public String getName() {
		return name();
	}

	/** Applied, Screening or Interview: Selected moves these to Offer, and scheduling moves the first two on. */
	public boolean isBeforeOffer() {
		return this == APPLIED || this == SCREENING || this == INTERVIEW;
	}

	/** Offer and Hired need the decision Selected. */
	public boolean needsSelected() {
		return this == OFFER || this == HIRED;
	}

	/** The stage stored as this text (any case, trimmed), or Applied for NULL and anything else. */
	public static HiringStage of(String stored) {
		HiringStage stage = parse(stored);
		return stage == null ? APPLIED : stage;
	}

	/** The stage named exactly so (any case, trimmed), or null. */
	public static HiringStage parse(String text) {
		if (text != null) {
			String code = text.trim().toUpperCase(Locale.ROOT);
			for (HiringStage stage : values()) {
				if (stage.name().equals(code)) {
					return stage;
				}
			}
		}
		return null;
	}
}
