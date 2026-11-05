package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.dao.CandidateDao;
import rms.dao.OfferDao;
import rms.model.CandidateInfo;
import rms.model.OfferInfo;

/** F8: the offer rules, the move to Hired on Accepted, and a log without the salary band. */
@ExtendWith(MockitoExtension.class)
class OfferServiceImplTest {

	@Mock
	OfferDao offerdao;

	@Mock
	CandidateDao candidatedao;

	@Mock
	StageService stageservice;

	@Mock
	ActivityService activityservice;

	OfferServiceImpl service;

	CandidateInfo candidate;

	@BeforeEach
	void setUp() {
		service = new OfferServiceImpl();
		service.setOfferDao(offerdao);
		service.setCandidateDao(candidatedao);
		service.setStageService(stageservice);
		service.setActivityService(activityservice);
		candidate = new CandidateInfo();
		candidate.setCandidateid("C1");
		candidate.setCandidatestatus("S");
		candidate.setStage("OFFER");
		lenient().when(candidatedao.findCandidateById("C1")).thenReturn(candidate);
		lenient().when(offerdao.getOffers("C1")).thenReturn(List.of());
	}

	@Test
	void aSelectedCandidateGetsAnOfferAndTheLogHasNoSalary() {
		when(offerdao.addOffer(any(OfferInfo.class), anyString())).thenReturn(7);
		OfferInfo offer = offer(0, "pending");
		offer.setSalaryband("  Band 3  ");

		assertThat(service.saveOffer(offer, "U3")).isEqualTo(Outcome.ok("Offer recorded."));

		assertThat(offer.getStatus()).isEqualTo("PENDING");
		assertThat(offer.getSalaryband()).isEqualTo("Band 3");
		assertThat(offer.getRespondeddate()).isNull();
		verify(activityservice).record("U3", "OFFER_MADE", "CANDIDATE", "C1", "offer 7, PENDING");
		verify(stageservice, never()).moveStage(any(), any(), any(), any(), any());
	}

	@Test
	void acceptedMovesACandidateAtOfferToHired() {
		OfferInfo stored = offer(4, "PENDING");
		when(offerdao.getOffers("C1")).thenReturn(List.of(stored));
		when(offerdao.updateOffer(any(OfferInfo.class), anyString())).thenReturn(true);
		when(stageservice.moveStage("C1", "OFFER", "HIRED", "Offer accepted", "U3")).thenReturn(Outcome.ok("Moved to Hired."));
		OfferInfo answer = offer(4, "ACCEPTED");

		assertThat(service.saveOffer(answer, "U3").message()).isEqualTo("Offer updated. The candidate moves to Hired.");

		assertThat(answer.getRespondeddate()).isEqualTo(LocalDate.now());
		verify(activityservice).record("U3", "OFFER_CHANGED", "CANDIDATE", "C1", "offer 4, ACCEPTED");
	}

	@Test
	void acceptedAtAnotherStageOnlySavesTheOffer() {
		candidate.setStage("HIRED");
		when(offerdao.addOffer(any(OfferInfo.class), anyString())).thenReturn(8);

		assertThat(service.saveOffer(offer(0, "ACCEPTED"), "U3").message()).isEqualTo("Offer recorded.");

		verify(stageservice, never()).moveStage(any(), any(), any(), any(), any());
	}

	@Test
	void theRulesRefuseWithAMessage() {
		expect(offer(0, "MAYBE"), "Please choose the offer's status.");
		OfferInfo nodate = offer(0, "PENDING");
		nodate.setOfferdate(null);
		expect(nodate, "Please enter the date of the offer.");
		OfferInfo future = offer(0, "PENDING");
		future.setOfferdate(LocalDate.now().plusDays(1));
		expect(future, "The offer date can't be in the future.");
		OfferInfo early = offer(0, "PENDING");
		early.setStartdate(early.getOfferdate().minusDays(1));
		expect(early, "The start date can't be before the offer date.");
		OfferInfo band = offer(0, "PENDING");
		band.setSalaryband("x".repeat(101));
		expect(band, "Please keep the salary band to 100 characters.");
		OfferInfo foreign = offer(0, "PENDING");
		foreign.setNote("Gehalt über Plan");
		expect(foreign, "The note can use English only. " + EnglishText.PROBLEM);

		candidate.setCandidatestatus("H");
		expect(offer(0, "PENDING"), "An offer needs the decision Selected. Record Selected first.");
		candidate.setCandidatestatus("S");
		when(offerdao.getOffers("C1")).thenReturn(List.of(offer(4, "PENDING")));
		expect(offer(0, "PENDING"), "An offer is already waiting for an answer. Change that one instead.");
		// An answered offer, or one of another candidate, can't be changed
		when(offerdao.getOffers("C1")).thenReturn(List.of(offer(4, "DECLINED")));
		expect(offer(4, "ACCEPTED"), "Only an offer that waits for an answer can be changed. Make a new offer instead.");
		expect(offer(5, "ACCEPTED"), "Only an offer that waits for an answer can be changed. Make a new offer instead.");
		OfferInfo gone = offer(0, "PENDING");
		gone.setCandidateid("C9");
		expect(gone, "The candidate no longer exists.");

		verify(offerdao, never()).addOffer(any(), any());
		verify(offerdao, never()).updateOffer(any(), any());
		verify(activityservice, never()).record(any(), any(), any(), any(), any());
	}

	private void expect(OfferInfo offer, String message) {
		assertThat(service.saveOffer(offer, "U3")).isEqualTo(Outcome.refused(message));
	}

	private static OfferInfo offer(int key, String status) {
		OfferInfo offer = new OfferInfo();
		offer.setOfferkey(key);
		offer.setCandidateid("C1");
		offer.setOfferdate(LocalDate.now().minusDays(2));
		offer.setStatus(status);
		return offer;
	}
}
