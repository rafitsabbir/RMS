package rms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.CandidateInfo;
import rms.model.Role;
import rms.service.CandidateService;
import rms.service.Outcome;
import rms.service.StageService;

/** Moving a candidate's hiring stage from the profile (the rules are in StageServiceImplTest). */
@ExtendWith(MockitoExtension.class)
class StageControllerTest {

	@Mock
	StageService stageservice;

	@Mock
	CandidateService candidateservice;

	@InjectMocks
	StageController controller;

	MockMvc mockMvc;

	MockHttpSession hr = EvaluationControllerTest.session("U3", Role.HR);

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void aMoveGoesBackToThePipelineCardWithTheMessage() throws Exception {
		when(candidateservice.findCandidateById("C 1+")).thenReturn(new CandidateInfo());
		when(stageservice.moveStage("C 1+", "SCREENING", "INTERVIEW", "Passed", "U3"))
				.thenReturn(Outcome.ok("Moved to Interview."));

		mockMvc.perform(post("/savestage").session(hr).param("candidateid", "C 1+").param("expected", "SCREENING")
				.param("stage", "INTERVIEW").param("note", "Passed"))
				// The ID may be any text (G22), so it is encoded
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C+1%2B#pipeline"))
				.andExpect(flash().attribute("message", "Moved to Interview."));
	}

	@Test
	void aRefusalIsShownAsAnError() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(new CandidateInfo());
		when(stageservice.moveStage("C1", null, "HIRED", null, "U3"))
				.thenReturn(Outcome.refused("Hired needs the decision Selected. Record Selected first."));

		mockMvc.perform(post("/savestage").session(hr).param("candidateid", "C1").param("stage", "HIRED"))
				.andExpect(flash().attribute("errorMessage", "Hired needs the decision Selected. Record Selected first."));
	}

	@Test
	void aMissingCandidateIs404AndAGetIs405() throws Exception {
		mockMvc.perform(post("/savestage").session(hr).param("candidateid", "C9").param("stage", "SCREENING"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/savestage").session(hr).param("candidateid", "C1")).andExpect(status().isMethodNotAllowed());
		verify(stageservice, never()).moveStage(anyString(), any(), any(), any(), anyString());
	}
}
