package rms.service;

import java.util.List;

import rms.model.OfferInfo;

/** Offer tracking (F8, 2026-11-05): Super Admin and HR record offers on the candidate profile; staff read them. */
public interface OfferService {

	/** The candidate's offers, newest first. */
	public List<OfferInfo> getOffers(String candidateid);

	/**
	 * Makes a new offer (offerkey 0) or changes the pending one. A new offer needs the decision Selected and no other
	 * pending offer; an answered offer can't be changed. Accepted moves a candidate at Offer to Hired.
	 */
	public Outcome saveOffer(OfferInfo offer, String userid);
}
