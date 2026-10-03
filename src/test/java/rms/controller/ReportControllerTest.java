package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import rms.model.ActivityInfo;
import rms.model.CandidateInfo;
import rms.model.Criterion;
import rms.model.ResultInfo;
import rms.model.Role;
import rms.model.ScheduleInfo;
import rms.service.ActivityService;
import rms.service.CandidateService;
import rms.service.MarksService;
import rms.service.ScheduleService;

/** Reports: CSV downloads for staff. Who may open them is SecurityConfig's, tested in SecurityConfigTest. */
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

	@Mock
	CandidateService candidateservice;

	@Mock
	MarksService marksservice;

	@Mock
	ScheduleService scheduleservice;

	@Mock
	ActivityService activityservice;

	@InjectMocks
	ReportController controller;

	MockMvc mockMvc;

	MockHttpSession session = EvaluationControllerTest.session("U3", Role.HR);

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void theReportsPageRenders() throws Exception {
		mockMvc.perform(get("/reports")).andExpect(status().isOk()).andExpect(view().name("reports"));
	}

	@Test
	void candidatesDownloadAsCsvWithTheDecision() throws Exception {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid("C1");
		candidate.setFirstname("Carla");
		candidate.setLastname("Candidate, Jr");
		candidate.setPhone("+000 111");
		candidate.setApplieddate(LocalDate.of(2026, 9, 1));
		candidate.setCandidatestatus("S");
		candidate.setDecisionreason("=HYPERLINK(\"x\")");
		when(candidateservice.getAllCandidate()).thenReturn(List.of(candidate));

		String csv = body("/exportcandidates");

		assertThat(csv).startsWith("﻿Candidate ID,First name,Last name,Position,Language,E-mail,Phone,Source,"
				+ "Applied date,Status,Decision date,Decided by,Reason for decision\r\n");
		assertThat(csv).contains("C1,Carla,\"Candidate, Jr\",", "'+000 111", "2026-09-01,Selected", "'=HYPERLINK");
		verify(activityservice).record("U3", "REPORT_DOWNLOADED", "REPORT", "CANDIDATES", null);
	}

	@Test
	void theDownloadIsAnAttachmentThatIsNeverCached() throws Exception {
		mockMvc.perform(get("/exportcandidates").session(session))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/csv"))
				.andExpect(header().string("Content-Disposition",
						org.hamcrest.Matchers.startsWith("attachment; filename=\"candidates-" + LocalDate.now())))
				.andExpect(header().string("Cache-Control", "no-store"));
	}

	@Test
	void resultsDownloadWithAveragesAndTheTotal() throws Exception {
		ResultInfo scored = new ResultInfo();
		scored.setCandidateid("C1");
		scored.setFirstname("Carla");
		scored.setLastname("Candidate");
		scored.setEvaluations(2);
		java.util.List<BigDecimal> averages = new java.util.ArrayList<>();
		for (int i = 0; i < Criterion.values().length; i++) {
			averages.add(new BigDecimal("7.25"));
		}
		scored.setAverages(averages);
		scored.setCandidatestatus("H");
		ResultInfo unscored = new ResultInfo();
		unscored.setCandidateid("C2");
		when(marksservice.getResults()).thenReturn(List.of(scored, unscored));

		String csv = body("/exportresults");

		String[] lines = csv.split("\r\n");
		assertThat(lines).hasSize(3);
		assertThat(lines[0]).contains("Candidate ID", "Evaluations", "Total (average)", "Status");
		assertThat(lines[0].split(",")).hasSize(Criterion.values().length + 8);
		assertThat(lines[1]).startsWith("C1,Carla,Candidate,,,2,7.3,").endsWith(",On hold");
		// No evaluations: no averages and no total
		assertThat(lines[2]).startsWith("C2,,,,,0,,");
		verify(activityservice).record("U3", "REPORT_DOWNLOADED", "REPORT", "CANDIDATE_STATUS", null);
	}

	@Test
	void theScheduleDownloadsWithTheServersTime() throws Exception {
		ScheduleInfo interview = new ScheduleInfo();
		interview.setSchedulekey(1);
		interview.setCandidateid("C1");
		interview.setCandidatename("Carla Candidate");
		interview.setInterviewerid("U2");
		interview.setInterviewername("Ivan Interviewer");
		interview.setStartat(LocalDateTime.of(2099, 1, 15, 10, 0));
		interview.setLocation("Room 1");
		when(scheduleservice.getAllSchedule()).thenReturn(List.of(interview));

		String csv = body("/exportschedule");

		assertThat(csv).contains("When (server time),Candidate ID", "2099-01-15 10:00,C1,Carla Candidate,U2,"
				+ "Ivan Interviewer,Room 1,SCHEDULED");
		verify(activityservice).record("U3", "REPORT_DOWNLOADED", "REPORT", "INTERVIEW_SCHEDULE", null);
	}

	@Test
	void theActivityLogDownloadsWithReadableActions() throws Exception {
		ActivityInfo entry = new ActivityInfo();
		entry.setUserid("U3");
		entry.setUsername("Hana HR");
		entry.setAction("ON_HOLD");
		entry.setEntitytype("CANDIDATE");
		entry.setEntityid("C1");
		entry.setCreatedat(LocalDateTime.of(2026, 10, 3, 9, 0, 0));
		when(activityservice.getRecent(null)).thenReturn(List.of(entry));

		String csv = body("/exportactivity");

		assertThat(csv).contains("2026-10-03 09:00:00,U3,Hana HR,On hold,CANDIDATE,C1,");
		verify(activityservice).record("U3", "REPORT_DOWNLOADED", "REPORT", "ACTIVITY_LOG", null);
	}

	private String body(String url) throws Exception {
		byte[] bytes = mockMvc.perform(get(url).session(session)).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsByteArray();
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
