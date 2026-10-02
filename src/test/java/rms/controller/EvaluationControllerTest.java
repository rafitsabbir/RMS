package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.AssignmentInfo;
import rms.model.CandidateInfo;
import rms.model.Criterion;
import rms.model.DecisionInfo;
import rms.model.MarksInfo;
import rms.model.Role;
import rms.model.UserInfo;
import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.DecisionService;
import rms.service.DocumentService;
import rms.service.MarksService;
import rms.service.Outcome;

/** My Evaluations, the evaluation form, a candidate's evaluations and the decision (Phase 3). */
@ExtendWith(MockitoExtension.class)
class EvaluationControllerTest {

	@Mock
	MarksService marksservice;

	@Mock
	AssignmentService assignmentservice;

	@Mock
	CandidateService candidateservice;

	@Mock
	DecisionService decisionservice;

	@Mock
	DocumentService documentservice;

	@InjectMocks
	EvaluationController controller;

	MockMvc mockMvc;

	MockHttpSession interviewer;

	MockHttpSession hr;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
		interviewer = session("U2", Role.INTERVIEWER);
		hr = session("U3", Role.HR);
		lenient().when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1", null));
		lenient().when(candidateservice.findCandidateById("C2")).thenReturn(candidate("C2", "S"));
	}

	@Test
	void myEvaluationsListsTheSessionUsersCandidates() throws Exception {
		List<AssignmentInfo> mine = List.of(new AssignmentInfo());
		when(assignmentservice.getMyCandidates("U2")).thenReturn(mine);

		mockMvc.perform(get("/myevaluations").session(interviewer))
				.andExpect(view().name("myevaluations"))
				.andExpect(model().attribute("candidatelist", mine));
	}

	@Test
	void evaluateShowsAnEmptyFormToAnAssignedInterviewer() throws Exception {
		when(assignmentservice.isAssigned("C1", "U2")).thenReturn(true);

		mockMvc.perform(get("/evaluate").param("candidateid", "C1").session(interviewer))
				.andExpect(status().isOk())
				.andExpect(view().name("evaluate"))
				.andExpect(model().attribute("editable", true))
				.andExpect(model().attribute("criteria", Criterion.values()))
				.andExpect(model().attributeExists("documents"));
	}

	@Test
	void evaluateIsReadOnlyWhenDecidedOrUnassigned() throws Exception {
		MarksInfo mine = new MarksInfo();
		when(assignmentservice.isAssigned("C2", "U2")).thenReturn(true);
		when(marksservice.findEvaluation("C2", "U2")).thenReturn(mine);
		mockMvc.perform(get("/evaluate").param("candidateid", "C2").session(interviewer))
				.andExpect(model().attribute("editable", false))
				.andExpect(model().attribute("marksinfo", mine));

		when(assignmentservice.isAssigned("C1", "U2")).thenReturn(false);
		when(marksservice.findEvaluation("C1", "U2")).thenReturn(mine);
		mockMvc.perform(get("/evaluate").param("candidateid", "C1").session(interviewer))
				.andExpect(model().attribute("editable", false))
				.andExpect(model().attribute("assigned", false))
				// No longer assigned: no document list, like the profile and the downloads
				.andExpect(model().attributeDoesNotExist("documents"));
	}

	@Test
	void evaluateIsRefusedForCandidatesNotAssignedAndNotEvaluated() throws Exception {
		mockMvc.perform(get("/evaluate").param("candidateid", "C1").session(interviewer))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/evaluate").param("candidateid", "C9").session(interviewer))
				.andExpect(status().isNotFound());
		// No session profile
		mockMvc.perform(get("/evaluate").param("candidateid", "C1")).andExpect(status().isForbidden());
	}

	@Test
	void saveTakesTheInterviewerFromTheSessionAndRedirects() throws Exception {
		when(assignmentservice.isAssigned("C1", "U2")).thenReturn(true);
		when(marksservice.saveEvaluation(any(MarksInfo.class), eq("U2"))).thenReturn(Outcome.ok("Evaluation saved."));

		mockMvc.perform(evaluation("C1", "7").param("interviewerid", "U9").param("markkey", "99")
				.param("comments", "Good.").param("stresscomment", "Calm."))
				.andExpect(redirectedUrl("/myevaluations"))
				.andExpect(flash().attribute("message", "Evaluation saved."));

		ArgumentCaptor<MarksInfo> captor = ArgumentCaptor.forClass(MarksInfo.class);
		verify(marksservice).saveEvaluation(captor.capture(), eq("U2"));
		MarksInfo saved = captor.getValue();
		// interviewerid and markkey aren't bound from the post
		assertThat(saved.getInterviewerid()).isNull();
		assertThat(saved.getMarkkey()).isZero();
		assertThat(saved.getScores()).containsOnly(7);
		assertThat(saved.getComments()).isEqualTo("Good.");
		assertThat(saved.getComment(Criterion.STRESS)).isEqualTo("Calm.");
	}

	@Test
	void saveRefusalsShowTheFormAgain() throws Exception {
		when(assignmentservice.isAssigned("C1", "U2")).thenReturn(true);
		when(marksservice.saveEvaluation(any(MarksInfo.class), eq("U2")))
				.thenReturn(Outcome.refused("Please score every criterion from 1 to 10."));

		mockMvc.perform(evaluation("C1", "0"))
				.andExpect(view().name("evaluate"))
				.andExpect(model().attribute("errorMessage", "Please score every criterion from 1 to 10."))
				.andExpect(model().attribute("editable", true));
		// A score that isn't a number
		mockMvc.perform(evaluation("C1", "seven"))
				.andExpect(model().attribute("errorMessage", "Please score every criterion from 1 to 10."));
	}

	@Test
	void saveIsRefusedWhenDecidedUnassignedOrNotAllowed() throws Exception {
		// Decided (C2 is Selected): the stored evaluation is shown, read-only
		when(assignmentservice.isAssigned("C2", "U2")).thenReturn(true);
		mockMvc.perform(evaluation("C2", "7"))
				.andExpect(view().name("evaluate"))
				.andExpect(model().attribute("editable", false))
				.andExpect(model().attribute("errorMessage",
						"This candidate has been selected, so the evaluations can't be changed."));
		// Unassigned with an evaluation
		when(marksservice.findEvaluation("C1", "U2")).thenReturn(new MarksInfo());
		mockMvc.perform(evaluation("C1", "7"))
				.andExpect(model().attribute("editable", false))
				.andExpect(model().attribute("errorMessage",
						"You are no longer assigned to this candidate, so your evaluation can't be changed."));
		// Never assigned
		when(marksservice.findEvaluation("C1", "U2")).thenReturn(null);
		mockMvc.perform(evaluation("C1", "7")).andExpect(status().isForbidden());

		verify(marksservice, never()).saveEvaluation(any(MarksInfo.class), anyString());
	}

	@Test
	void saveIsPostOnly() throws Exception {
		mockMvc.perform(get("/saveevaluation").session(interviewer)).andExpect(status().isMethodNotAllowed());
	}

	@Test
	void viewEvaluationsShowsScoresAveragesAndDecisions() throws Exception {
		when(marksservice.getEvaluations("C2")).thenReturn(List.of(scored(8), scored(5)));
		when(decisionservice.getDecisions("C2")).thenReturn(List.of(new DecisionInfo()));

		mockMvc.perform(get("/viewevaluations").param("candidateid", "C2").session(hr))
				.andExpect(view().name("viewevaluations"))
				.andExpect(model().attribute("summary",
						org.hamcrest.Matchers.hasProperty("evaluations", org.hamcrest.Matchers.is(2))))
				.andExpect(model().attribute("decisioninfo",
						org.hamcrest.Matchers.hasProperty("status", org.hamcrest.Matchers.is("S"))))
				.andExpect(model().attribute("decisioninfo",
						org.hamcrest.Matchers.hasProperty("decisiondate", org.hamcrest.Matchers.is(LocalDate.now()))));
		mockMvc.perform(get("/viewevaluations").param("candidateid", "C9").session(hr))
				.andExpect(status().isNotFound());
	}

	@Test
	void saveDecisionPassesTheDeciderAndTheEvaluationCount() throws Exception {
		when(marksservice.getEvaluations("C1")).thenReturn(List.of());
		when(decisionservice.saveDecision(any(DecisionInfo.class), eq("U3"), eq(0)))
				.thenReturn(Outcome.ok("Decision saved: On hold. Note: there are no evaluations yet."));

		mockMvc.perform(decision("C1", "H", "Waiting", "2026-09-30").param("decidedby", "U9"))
				.andExpect(redirectedUrl("/viewevaluations?candidateid=C1"))
				.andExpect(flash().attribute("message", "Decision saved: On hold. Note: there are no evaluations yet."));

		ArgumentCaptor<DecisionInfo> captor = ArgumentCaptor.forClass(DecisionInfo.class);
		verify(decisionservice).saveDecision(captor.capture(), eq("U3"), anyInt());
		assertThat(captor.getValue().getStatus()).isEqualTo("H");
		assertThat(captor.getValue().getReason()).isEqualTo("Waiting");
		assertThat(captor.getValue().getDecisiondate()).isEqualTo(LocalDate.of(2026, 9, 30));
		// decidedby isn't bound from the post
		assertThat(captor.getValue().getDecidedby()).isNull();
	}

	@Test
	void decisionRefusalsShowThePageAgain() throws Exception {
		when(marksservice.getEvaluations("C1")).thenReturn(List.of());
		when(decisionservice.saveDecision(any(DecisionInfo.class), eq("U3"), eq(0)))
				.thenReturn(Outcome.refused("Please give the reason for the decision."));

		mockMvc.perform(decision("C1", "S", "", "2026-09-30"))
				.andExpect(view().name("viewevaluations"))
				.andExpect(model().attribute("errorMessage", "Please give the reason for the decision."));
		mockMvc.perform(decision("C1", "S", "r", "30/09/2026"))
				.andExpect(model().attribute("errorMessage", "Please enter the date of the decision as a date."));
		mockMvc.perform(decision("C9", "S", "r", "2026-09-30")).andExpect(status().isNotFound());
	}

	private MockHttpServletRequestBuilder evaluation(String candidateid, String score) {
		MockHttpServletRequestBuilder request = post("/saveevaluation").session(interviewer)
				.param("candidateid", candidateid);
		for (Criterion criterion : Criterion.values()) {
			request.param(criterion.getField(), score);
		}
		return request;
	}

	private MockHttpServletRequestBuilder decision(String candidateid, String status, String reason, String date) {
		return post("/savedecision").session(hr).param("candidateid", candidateid).param("status", status)
				.param("reason", reason).param("decisiondate", date);
	}

	static MockHttpSession session(String userid, Role role) {
		UserInfo user = new UserInfo();
		user.setUserid(userid);
		user.setRole(role.name());
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", user);
		return session;
	}

	private static CandidateInfo candidate(String id, String status) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid(id);
		candidate.setFirstname("Carla");
		candidate.setLastname("Candidate");
		candidate.setCandidatestatus(status);
		return candidate;
	}

	private static MarksInfo scored(int score) {
		MarksInfo marks = new MarksInfo();
		for (Criterion criterion : Criterion.values()) {
			marks.setScore(criterion, score);
		}
		return marks;
	}
}
