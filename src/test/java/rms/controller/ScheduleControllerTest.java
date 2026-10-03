package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

import java.time.LocalDateTime;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.AssignmentInfo;
import rms.model.CandidateInfo;
import rms.model.ScheduleInfo;
import rms.model.UserInfo;
import rms.service.CandidateService;
import rms.service.Outcome;
import rms.service.ScheduleService;

/** Interview schedule: the two-step form, save, cancel, and the lists (the rules are in ScheduleServiceImplTest). */
@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

	@Mock
	ScheduleService scheduleservice;

	@Mock
	CandidateService candidateservice;

	@InjectMocks
	ScheduleController controller;

	MockMvc mockMvc;

	MockHttpSession hr;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
		UserInfo user = new UserInfo();
		user.setUserid("U3");
		user.setRole("HR");
		hr = new MockHttpSession();
		hr.setAttribute("user", user);
	}

	@Test
	void listShowsEveryInterview() throws Exception {
		List<ScheduleInfo> list = List.of(schedule(1, "C1", "U2"));
		when(scheduleservice.getAllSchedule()).thenReturn(list);

		mockMvc.perform(get("/viewschedulelist"))
				.andExpect(status().isOk())
				.andExpect(view().name("viewschedule"))
				.andExpect(model().attribute("schedulelist", list));
	}

	@Test
	void mySchedulesIsTheSessionUsersOnly() throws Exception {
		UserInfo interviewer = new UserInfo();
		interviewer.setUserid("U2");
		interviewer.setRole("INTERVIEWER");
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", interviewer);
		List<ScheduleInfo> mine = List.of(schedule(1, "C1", "U2"));
		when(scheduleservice.getScheduleOf("U2")).thenReturn(mine);

		// A request parameter naming another interviewer changes nothing
		mockMvc.perform(get("/myschedule").param("interviewerid", "U5").session(session))
				.andExpect(view().name("myschedule"))
				.andExpect(model().attribute("schedulelist", mine));
		verify(scheduleservice, never()).getScheduleOf("U5");
	}

	@Test
	void mySchedulesWithoutASessionProfileIsForbidden() throws Exception {
		mockMvc.perform(get("/myschedule").session(new MockHttpSession())).andExpect(status().isForbidden());
	}

	@Test
	void createWithoutACandidateOffersTheCandidates() throws Exception {
		List<CandidateInfo> candidates = List.of(candidate("C1"));
		when(candidateservice.getAllCandidate()).thenReturn(candidates);

		mockMvc.perform(get("/createschedule"))
				.andExpect(view().name("createschedule"))
				.andExpect(model().attribute("candidatelist", candidates))
				.andExpect(model().attributeDoesNotExist("scheduleinfo"));
	}

	@Test
	void createWithACandidateShowsTheFormWithTheirInterviewers() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));
		List<AssignmentInfo> interviewers = List.of(new AssignmentInfo());
		when(scheduleservice.getSchedulableInterviewers("C1")).thenReturn(interviewers);

		mockMvc.perform(get("/createschedule").param("candidateid", "C1"))
				.andExpect(view().name("createschedule"))
				.andExpect(model().attribute("interviewerlist", interviewers))
				.andExpect(model().attribute("scheduleinfo", org.hamcrest.Matchers.hasProperty("candidateid",
						org.hamcrest.Matchers.is("C1"))));
	}

	@Test
	void createForAMissingCandidateIs404() throws Exception {
		mockMvc.perform(get("/createschedule").param("candidateid", "C9")).andExpect(status().isNotFound());
	}

	@Test
	void updateShowsTheStoredInterview() throws Exception {
		ScheduleInfo stored = schedule(5, "C1", "U2");
		when(scheduleservice.findScheduleById(5)).thenReturn(stored);
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));

		mockMvc.perform(get("/updateschedule/5"))
				.andExpect(view().name("createschedule"))
				.andExpect(model().attribute("scheduleinfo", stored));
	}

	@Test
	void theEditFormKeepsAnInterviewerWhoIsNoLongerAssigned() throws Exception {
		ScheduleInfo stored = schedule(5, "C1", "U5");
		stored.setInterviewername("Fay Former");
		when(scheduleservice.findScheduleById(5)).thenReturn(stored);
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));
		when(scheduleservice.getSchedulableInterviewers("C1")).thenReturn(List.of());

		mockMvc.perform(get("/updateschedule/5"))
				.andExpect(model().attribute("interviewerlist", org.hamcrest.Matchers.contains(
						org.hamcrest.Matchers.allOf(
								org.hamcrest.Matchers.hasProperty("interviewerid", org.hamcrest.Matchers.is("U5")),
								org.hamcrest.Matchers.hasProperty("interviewername",
										org.hamcrest.Matchers.is("Fay Former (not assigned now)"))))));
		// A new interview offers only the assigned, active interviewers
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));
		mockMvc.perform(get("/createschedule").param("candidateid", "C1"))
				.andExpect(model().attribute("interviewerlist", org.hamcrest.Matchers.empty()));
	}

	@Test
	void updateOfAMissingInterviewIs404() throws Exception {
		mockMvc.perform(get("/updateschedule/5")).andExpect(status().isNotFound());
	}

	@Test
	void aCancelledInterviewCantBeEdited() throws Exception {
		ScheduleInfo stored = schedule(5, "C1", "U2");
		stored.setStatus(ScheduleInfo.CANCELLED);
		when(scheduleservice.findScheduleById(5)).thenReturn(stored);

		mockMvc.perform(get("/updateschedule/5"))
				.andExpect(redirectedUrl("/viewschedulelist"))
				.andExpect(flash().attribute("errorMessage", "A cancelled interview can't be changed."));
	}

	@Test
	void saveAddsAndRedirectsWithAMessage() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));
		when(scheduleservice.save(any(ScheduleInfo.class), anyString())).thenReturn(Outcome.ok("Interview scheduled."));

		mockMvc.perform(post("/saveschedule").session(hr).param("candidateid", "C1").param("interviewerid", "U2")
				.param("startat", "2099-03-04T10:30").param("location", "Room 1"))
				.andExpect(redirectedUrl("/viewschedulelist"))
				.andExpect(flash().attribute("message", "Interview scheduled."));

		ArgumentCaptor<ScheduleInfo> saved = ArgumentCaptor.forClass(ScheduleInfo.class);
		verify(scheduleservice).save(saved.capture(), org.mockito.ArgumentMatchers.eq("U3"));
		assertThat(saved.getValue().getStartat()).isEqualTo(LocalDateTime.of(2099, 3, 4, 10, 30));
		assertThat(saved.getValue().getCandidateid()).isEqualTo("C1");
	}

	@Test
	void aRefusalShowsTheFormAgainWithTheMessage() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));
		when(scheduleservice.save(any(ScheduleInfo.class), anyString()))
				.thenReturn(Outcome.refused("That interviewer already has an interview at that time."));

		mockMvc.perform(post("/saveschedule").session(hr).param("candidateid", "C1").param("interviewerid", "U2")
				.param("startat", "2099-03-04T10:30"))
				.andExpect(status().isOk())
				.andExpect(view().name("createschedule"))
				.andExpect(model().attribute("errorMessage", "That interviewer already has an interview at that time."))
				.andExpect(model().attributeExists("interviewerlist"));
	}

	@Test
	void aBadDateShowsTheFormAgainAndDoesntSave() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));

		mockMvc.perform(post("/saveschedule").session(hr).param("candidateid", "C1").param("interviewerid", "U2")
				.param("startat", "next tuesday"))
				.andExpect(status().isOk())
				.andExpect(view().name("createschedule"))
				.andExpect(model().attributeExists("errorMessage"));
		verify(scheduleservice, never()).save(any(ScheduleInfo.class), anyString());
	}

	@Test
	void aChangeTakesTheCandidateFromTheStoredInterviewNotThePost() throws Exception {
		when(scheduleservice.findScheduleById(5)).thenReturn(schedule(5, "C1", "U2"));
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));
		when(scheduleservice.save(any(ScheduleInfo.class), anyString())).thenReturn(Outcome.ok("Interview updated."));

		mockMvc.perform(post("/saveschedule").session(hr).param("schedulekey", "5").param("candidateid", "C2")
				.param("interviewerid", "U2").param("startat", "2099-03-04T10:30").param("status", "DONE"))
				.andExpect(redirectedUrl("/viewschedulelist"));

		ArgumentCaptor<ScheduleInfo> saved = ArgumentCaptor.forClass(ScheduleInfo.class);
		verify(scheduleservice).save(saved.capture(), anyString());
		assertThat(saved.getValue().getCandidateid()).isEqualTo("C1");
	}

	@Test
	void savingForAMissingCandidateIs404() throws Exception {
		mockMvc.perform(post("/saveschedule").session(hr).param("candidateid", "C9").param("interviewerid", "U2")
				.param("startat", "2099-03-04T10:30"))
				.andExpect(status().isNotFound());
		verify(scheduleservice, never()).save(any(ScheduleInfo.class), anyString());
	}

	@Test
	void savingAMissingInterviewIs404() throws Exception {
		mockMvc.perform(post("/saveschedule").session(hr).param("schedulekey", "5").param("candidateid", "C1"))
				.andExpect(status().isNotFound());
	}

	@Test
	void onlyTheFormsFieldsBind() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1"));
		when(scheduleservice.save(any(ScheduleInfo.class), anyString())).thenReturn(Outcome.ok("Interview scheduled."));

		mockMvc.perform(post("/saveschedule").session(hr).param("candidateid", "C1").param("interviewerid", "U2")
				.param("startat", "2099-03-04T10:30").param("candidatename", "Forged").param("interviewername", "Forged"))
				.andExpect(redirectedUrl("/viewschedulelist"));

		ArgumentCaptor<ScheduleInfo> saved = ArgumentCaptor.forClass(ScheduleInfo.class);
		verify(scheduleservice).save(saved.capture(), anyString());
		assertThat(saved.getValue().getCandidatename()).isNull();
		assertThat(saved.getValue().getInterviewername()).isNull();
	}

	@Test
	void cancelRedirectsWithTheServicesMessage() throws Exception {
		when(scheduleservice.findScheduleById(5)).thenReturn(schedule(5, "C1", "U2"));
		when(scheduleservice.cancel(5, "U3")).thenReturn(Outcome.ok("Interview cancelled."));

		mockMvc.perform(post("/cancelschedule/5").session(hr))
				.andExpect(redirectedUrl("/viewschedulelist"))
				.andExpect(flash().attribute("message", "Interview cancelled."));
	}

	@Test
	void aRefusedCancelShowsTheMessageAsAnError() throws Exception {
		when(scheduleservice.findScheduleById(5)).thenReturn(schedule(5, "C1", "U2"));
		when(scheduleservice.cancel(5, "U3")).thenReturn(Outcome.refused("Only a scheduled interview can be cancelled."));

		mockMvc.perform(post("/cancelschedule/5").session(hr))
				.andExpect(flash().attribute("errorMessage", "Only a scheduled interview can be cancelled."));
	}

	@Test
	void cancelOfAMissingInterviewIs404AndCancelIsPostOnly() throws Exception {
		mockMvc.perform(post("/cancelschedule/5").session(hr)).andExpect(status().isNotFound());
		verify(scheduleservice, never()).cancel(anyInt(), anyString());
		mockMvc.perform(get("/cancelschedule/5")).andExpect(status().isMethodNotAllowed());
	}

	private static ScheduleInfo schedule(int key, String candidateid, String interviewerid) {
		ScheduleInfo info = new ScheduleInfo();
		info.setSchedulekey(key);
		info.setCandidateid(candidateid);
		info.setInterviewerid(interviewerid);
		info.setStartat(LocalDateTime.of(2099, 1, 15, 10, 0));
		return info;
	}

	private static CandidateInfo candidate(String candidateid) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid(candidateid);
		return candidate;
	}
}
