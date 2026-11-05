package rms.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.CandidateDao;
import rms.dao.OfferDao;
import rms.model.CandidateInfo;
import rms.model.DecisionStatus;
import rms.model.HiringStage;
import rms.model.OfferInfo;
import rms.model.OfferStatus;

/** Offers (F8). Logs and the activity log carry keys and the status only, never the salary band or the note. */
@Service
public class OfferServiceImpl implements OfferService {
	private static final Logger log = LoggerFactory.getLogger(OfferServiceImpl.class);

	OfferDao offerdao;
	CandidateDao candidatedao;
	StageService stageservice;
	ActivityService activityservice;

	@Autowired
	public void setOfferDao(OfferDao offerdao) {
		this.offerdao = offerdao;
	}

	@Autowired
	public void setCandidateDao(CandidateDao candidatedao) {
		this.candidatedao = candidatedao;
	}

	@Autowired
	public void setStageService(StageService stageservice) {
		this.stageservice = stageservice;
	}

	@Autowired
	public void setActivityService(ActivityService activityservice) {
		this.activityservice = activityservice;
	}

	@Override
	public List<OfferInfo> getOffers(String candidateid) {
		return offerdao.getOffers(candidateid);
	}

	@Override
	public synchronized Outcome saveOffer(OfferInfo offer, String userid) {
		CandidateInfo candidate = offer.getCandidateid() == null ? null
				: candidatedao.findCandidateById(offer.getCandidateid());
		if (candidate == null) {
			return Outcome.refused("The candidate no longer exists.");
		}
		OfferStatus status = OfferStatus.parse(offer.getStatus());
		if (status == null) {
			return Outcome.refused("Please choose the offer's status.");
		}
		offer.setStatus(status.name());
		if (offer.getOfferdate() == null) {
			return Outcome.refused("Please enter the date of the offer.");
		}
		if (offer.getOfferdate().isAfter(LocalDate.now())) {
			return Outcome.refused("The offer date can't be in the future.");
		}
		if (offer.getStartdate() != null && offer.getStartdate().isBefore(offer.getOfferdate())) {
			return Outcome.refused("The start date can't be before the offer date.");
		}
		String band = blankToNull(offer.getSalaryband());
		if (band != null && band.length() > OfferInfo.MAX_SALARYBAND) {
			return Outcome.refused("Please keep the salary band to " + OfferInfo.MAX_SALARYBAND + " characters.");
		}
		if (!EnglishText.isEnglish(band)) {
			return Outcome.refused("The salary band can use English only. " + EnglishText.PROBLEM);
		}
		offer.setSalaryband(band);
		String note = blankToNull(offer.getNote());
		if (note != null && note.length() > OfferInfo.MAX_NOTE) {
			return Outcome.refused("The note can have at most " + OfferInfo.MAX_NOTE + " characters.");
		}
		if (!EnglishText.isEnglishMultiline(note)) {
			return Outcome.refused("The note can use English only. " + EnglishText.PROBLEM);
		}
		offer.setNote(note);
		// The answer's date is RMS's: today, when the offer leaves Pending
		offer.setRespondeddate(status.isClosed() ? LocalDate.now() : null);

		List<OfferInfo> offers = offerdao.getOffers(candidate.getCandidateid());
		String action;
		if (offer.getOfferkey() > 0) {
			OfferInfo stored = offers.stream().filter(o -> o.getOfferkey() == offer.getOfferkey()).findFirst()
					.orElse(null);
			if (stored == null || !stored.isPending() || !offerdao.updateOffer(offer, userid)) {
				return Outcome.refused("Only an offer that waits for an answer can be changed. Make a new offer instead.");
			}
			action = "OFFER_CHANGED";
		} else {
			// Offer and Hired need Selected (hiring stages), so an offer does too
			if (DecisionStatus.parse(candidate.getCandidatestatus()) != DecisionStatus.S) {
				return Outcome.refused("An offer needs the decision Selected. Record Selected first.");
			}
			if (offers.stream().anyMatch(OfferInfo::isPending)) {
				return Outcome.refused("An offer is already waiting for an answer. Change that one instead.");
			}
			offer.setOfferkey(offerdao.addOffer(offer, userid));
			action = "OFFER_MADE";
		}
		activityservice.record(userid, action, "CANDIDATE", candidate.getCandidateid(),
				"offer " + offer.getOfferkey() + ", " + status.name());
		log.info("Offer {} of candidate {} saved by {}: {}", offer.getOfferkey(), candidate.getCandidateid(), userid,
				status.name());

		String message = action.equals("OFFER_MADE") ? "Offer recorded." : "Offer updated.";
		// Accepted: a candidate at Offer moves on to Hired, through the usual stage rules and history
		if (status == OfferStatus.ACCEPTED && HiringStage.of(candidate.getStage()) == HiringStage.OFFER) {
			Outcome moved = stageservice.moveStage(candidate.getCandidateid(), HiringStage.OFFER.name(),
					HiringStage.HIRED.name(), "Offer accepted", userid);
			message += moved.done() ? " The candidate moves to Hired." : " " + moved.message();
		}
		return Outcome.ok(message);
	}

	private static String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}
}
