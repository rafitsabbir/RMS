package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.model.CandidateInfo;
import rms.model.OfferInfo;
import rms.model.Role;
import rms.service.CandidateService;
import rms.service.OfferService;
import rms.service.Outcome;

/** F8: the offer form posts back to the profile with the service's message. */
@ExtendWith(MockitoExtension.class)
class OfferControllerTest {

	@Mock
	OfferService offerservice;

	@Mock
	CandidateService candidateservice;

	@InjectMocks
	OfferController controller;

	MockMvc mockMvc;

	MockHttpSession hr;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
		hr = EvaluationControllerTest.session("U3", Role.HR);
	}

	@Test
	void anOfferIsSavedWithTheFormsFieldsOnly() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(new CandidateInfo());
		when(offerservice.saveOffer(any(OfferInfo.class), eq("U3"))).thenReturn(Outcome.ok("Offer recorded."));

		mockMvc.perform(post("/saveoffer").session(hr).param("candidateid", "C1").param("offerdate", "2026-10-25")
				.param("salaryband", "Band 3").param("status", "PENDING").param("createdby", "U9")
				.param("respondeddate", "2026-01-01"))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1#offer"))
				.andExpect(flash().attribute("message", "Offer recorded."));

		ArgumentCaptor<OfferInfo> saved = ArgumentCaptor.forClass(OfferInfo.class);
		verify(offerservice).saveOffer(saved.capture(), eq("U3"));
		assertThat(saved.getValue().getOfferdate()).isEqualTo(LocalDate.of(2026, 10, 25));
		assertThat(saved.getValue().getSalaryband()).isEqualTo("Band 3");
		// RMS's own fields aren't bound from the post
		assertThat(saved.getValue().getCreatedby()).isNull();
		assertThat(saved.getValue().getRespondeddate()).isNull();
	}

	@Test
	void aDateThatIsntOneOrAMissingCandidateIsRefused() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(new CandidateInfo());

		mockMvc.perform(post("/saveoffer").session(hr).param("candidateid", "C1").param("offerdate", "25/10/2026"))
				.andExpect(flash().attribute("errorMessage", "Please enter the dates as dates."));
		mockMvc.perform(post("/saveoffer").session(hr).param("candidateid", "C9")).andExpect(status().isNotFound());

		verify(offerservice, never()).saveOffer(any(), any());
	}
}
