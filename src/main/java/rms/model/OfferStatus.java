package rms.model;

import java.util.Locale;

/** Where an offer stands (candidate_offer.status, migration 011; F8, 2026-11-05). */
public enum OfferStatus {
	PENDING("Waiting for an answer"),
	ACCEPTED("Accepted"),
	DECLINED("Declined"),
	WITHDRAWN("Withdrawn");

	private final String label;

	OfferStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/** For the JSPs, which can't call name(). */
	public String getName() {
		return name();
	}

	/** Answered or withdrawn: a new offer is a new row, this one stays as history. */
	public boolean isClosed() {
		return this != PENDING;
	}

	/** The status named exactly so (any case, trimmed), or null. */
	public static OfferStatus parse(String text) {
		if (text != null) {
			String code = text.trim().toUpperCase(Locale.ROOT);
			for (OfferStatus status : values()) {
				if (status.name().equals(code)) {
					return status;
				}
			}
		}
		return null;
	}
}
