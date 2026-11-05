package rms.dao;

import java.util.List;

import rms.model.OfferInfo;

/** Offers (candidate_offer, migration 011; F8). Only active rows are read; RMS never deletes one. */
public interface OfferDao {
	/** The candidate's offers, newest first, with the name of who made each. */
	public List<OfferInfo> getOffers(String candidateid);

	/** An active offer, or null. */
	public OfferInfo findOffer(int offerkey);

	/** Adds an offer and returns its key. */
	public int addOffer(OfferInfo offer, String userid);

	/** Changes a PENDING offer of that candidate; false when it isn't one (any more). */
	public boolean updateOffer(OfferInfo offer, String userid);
}
