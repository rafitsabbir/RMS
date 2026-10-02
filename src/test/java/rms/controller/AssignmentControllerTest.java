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
import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.Outcome;

/** Assigning and unassigning interviewers from the profile (Phase 3). */
@ExtendWith(MockitoExtension.class)
class AssignmentControllerTest {

	@Mock
	AssignmentService assignmentservice;

	@Mock
	CandidateService candidateservice;

	@InjectMocks
	AssignmentController controller;

	MockMvc mockMvc;

	MockHttpSession hr;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
		hr = EvaluationControllerTest.session("U3", Role.HR);
	}

	@Test
	void assignRecordsTheActingUserAndReturnsToTheProfile() throws Exception {
		givenCandidate("C1");
		when(assignmentservice.assign("C1", "U2", "U3")).thenReturn(Outcome.ok("Interviewer assigned."));

		mockMvc.perform(post("/assigninterviewer").session(hr).param("candidateid", "C1").param("interviewerid", "U2"))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1#interviewers"))
				.andExpect(flash().attribute("message", "Interviewer assigned."));
	}

	@Test
	void refusedAssignShowsTheError() throws Exception {
		givenCandidate("C1");
		when(assignmentservice.assign("C1", "U3", "U3"))
				.thenReturn(Outcome.refused("Please choose an active interviewer who isn't assigned yet."));

		mockMvc.perform(post("/assigninterviewer").session(hr).param("candidateid", "C1").param("interviewerid", "U3"))
				.andExpect(flash().attribute("errorMessage", "Please choose an active interviewer who isn't assigned yet."));
	}

	@Test
	void unassign() throws Exception {
		givenCandidate("C 1+2");
		when(assignmentservice.unassign("C 1+2", "U2", "U3")).thenReturn(Outcome.ok("Interviewer unassigned."));

		mockMvc.perform(post("/unassigninterviewer").session(hr).param("candidateid", "C 1+2").param("interviewerid", "U2"))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C+1%2B2#interviewers"))
				.andExpect(flash().attribute("message", "Interviewer unassigned."));
	}

	@Test
	void missingCandidateNoSessionUserOrGet() throws Exception {
		mockMvc.perform(post("/assigninterviewer").session(hr).param("candidateid", "C9").param("interviewerid", "U2"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/assigninterviewer").param("candidateid", "C1").param("interviewerid", "U2"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/unassigninterviewer").session(hr).param("candidateid", "C1"))
				.andExpect(status().isMethodNotAllowed());

		verify(assignmentservice, never()).assign(anyString(), any(), anyString());
	}

	private void givenCandidate(String candidateid) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid(candidateid);
		when(candidateservice.findCandidateById(candidateid)).thenReturn(candidate);
	}
}
