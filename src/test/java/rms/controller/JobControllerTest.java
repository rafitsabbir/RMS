package rms.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.JobInfo;
import rms.model.PositionInfo;
import rms.service.ActivityService;
import rms.service.JobService;
import rms.service.PositionService;

/** Jobs: list, add, edit and soft delete (copied from the Position module). */
@ExtendWith(MockitoExtension.class)
class JobControllerTest {

	@Mock
	JobService jobservice;

	@Mock
	PositionService positionservice;

	@Mock
	ActivityService activityservice;

	@InjectMocks
	JobController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
		lenient().when(positionservice.getAllPosition()).thenReturn(new ArrayList<>(List.of(
				position(1, "SOFTWARE ENGINEER"), position(2, "QA ENGINEER"))));
		lenient().when(positionservice.findPositionById(1)).thenReturn(position(1, "SOFTWARE ENGINEER"));
		lenient().when(positionservice.findPositionById(2)).thenReturn(position(2, "QA ENGINEER"));
	}

	@Test
	void listShowsTheActiveJobs() throws Exception {
		List<JobInfo> jobs = List.of(job(1, 1, "OPEN"), job(2, 2, "CLOSED"));
		when(jobservice.getAllJob()).thenReturn(jobs);

		mockMvc.perform(get("/viewjoblist"))
				.andExpect(status().isOk())
				.andExpect(view().name("viewjob"))
				.andExpect(model().attribute("joblist", jobs));
	}

	@Test
	void createFormIsAnOpenJobWithOneVacancy() throws Exception {
		mockMvc.perform(get("/createjob"))
				.andExpect(view().name("createjob"))
				.andExpect(model().attribute("jobinfo", hasProperty("jobkey", is(0))))
				.andExpect(model().attribute("jobinfo", hasProperty("vacancies", is(1))))
				.andExpect(model().attribute("jobinfo", hasProperty("status", is("OPEN"))))
				.andExpect(model().attribute("positionlist", contains(
						hasProperty("positionkey", is(1)), hasProperty("positionkey", is(2)))));
	}

	@Test
	void saveNewJobAddsIt() throws Exception {
		mockMvc.perform(save("0", "2", "3", "2030-12-31", "OPEN"))
				.andExpect(redirectedUrl("/viewjoblist"));

		ArgumentCaptor<JobInfo> captor = ArgumentCaptor.forClass(JobInfo.class);
		verify(jobservice).addJob(captor.capture());
		verify(jobservice, never()).updateJob(any(JobInfo.class));
		JobInfo saved = captor.getValue();
		assertThat(saved.getPositionkey()).isEqualTo(2);
		assertThat(saved.getVacancies()).isEqualTo(3);
		assertThat(saved.getClosingdate()).isEqualTo(LocalDate.of(2030, 12, 31));
		assertThat(saved.getStatus()).isEqualTo("OPEN");
	}

	@Test
	void closingDateIsOptional() throws Exception {
		mockMvc.perform(save("0", "1", "1", "", "CLOSED")).andExpect(redirectedUrl("/viewjoblist"));

		ArgumentCaptor<JobInfo> captor = ArgumentCaptor.forClass(JobInfo.class);
		verify(jobservice).addJob(captor.capture());
		assertThat(captor.getValue().getClosingdate()).isNull();
	}

	@Test
	void saveExistingJobUpdatesIt() throws Exception {
		when(jobservice.findJobById(1)).thenReturn(job(1, 1, "OPEN"));

		mockMvc.perform(save("1", "1", "2", "", "CLOSED")).andExpect(redirectedUrl("/viewjoblist"));

		ArgumentCaptor<JobInfo> captor = ArgumentCaptor.forClass(JobInfo.class);
		verify(jobservice).updateJob(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo("CLOSED");
		verify(jobservice, never()).addJob(any(JobInfo.class));
	}

	@Test
	void saveOfADeletedJobIs404() throws Exception {
		mockMvc.perform(save("9", "1", "1", "", "OPEN")).andExpect(status().isNotFound());

		verify(jobservice, never()).updateJob(any(JobInfo.class));
		verify(jobservice, never()).addJob(any(JobInfo.class));
	}

	@Test
	void refusals() throws Exception {
		expectError(save("0", "0", "1", "", "OPEN"), "Please choose a position.");
		expectError(save("0", "3", "1", "", "OPEN"), "Please choose a position.");
		expectError(save("0", "x", "1", "", "OPEN"), "Please choose a position.");
		expectError(save("0", "1", "0", "", "OPEN"), "Please enter the number of vacancies, from 1 to 999.");
		expectError(save("0", "1", "1000", "", "OPEN"), "Please enter the number of vacancies, from 1 to 999.");
		expectError(save("0", "1", "many", "", "OPEN"), "Please enter the number of vacancies, from 1 to 999.");
		expectError(save("0", "1", "1", "31/12/2030", "OPEN"),
				"Please enter the closing date as a date, or leave it empty.");
		expectError(save("0", "1", "1", "", "open"), "Please choose whether the job is open or closed.");
		expectError(save("0", "1", "1", "", ""), "Please choose whether the job is open or closed.");

		verify(jobservice, never()).addJob(any(JobInfo.class));
	}

	@Test
	void editMayKeepADeletedPosition() throws Exception {
		JobInfo stored = job(1, 3, "OPEN");
		stored.setPositionname("RETIRED ROLE");
		when(jobservice.findJobById(1)).thenReturn(stored);

		mockMvc.perform(get("/updatejob/1"))
				.andExpect(view().name("createjob"))
				.andExpect(model().attribute("positionlist", contains(
						hasProperty("positionname", is("SOFTWARE ENGINEER")),
						hasProperty("positionname", is("QA ENGINEER")),
						hasProperty("positionname", is("RETIRED ROLE (deleted)")))));
		mockMvc.perform(save("1", "3", "1", "", "OPEN")).andExpect(redirectedUrl("/viewjoblist"));

		verify(jobservice).updateJob(any(JobInfo.class));
	}

	@Test
	void editOfAMissingJobIs404() throws Exception {
		mockMvc.perform(get("/updatejob/9")).andExpect(status().isNotFound());
	}

	@Test
	void postedFieldsOutsideTheFormAreIgnored() throws Exception {
		mockMvc.perform(save("0", "1", "1", "", "OPEN").param("positionname", "INJECTED"))
				.andExpect(redirectedUrl("/viewjoblist"));

		ArgumentCaptor<JobInfo> captor = ArgumentCaptor.forClass(JobInfo.class);
		verify(jobservice).addJob(captor.capture());
		assertThat(captor.getValue().getPositionname()).isNull();
	}

	@Test
	void deleteIsSoftAndPostOnly() throws Exception {
		mockMvc.perform(post("/deletejob/2")).andExpect(redirectedUrl("/viewjoblist"));
		verify(jobservice).deleteJob(2);

		mockMvc.perform(get("/deletejob/2")).andExpect(status().isMethodNotAllowed());
		mockMvc.perform(get("/savejob")).andExpect(status().isMethodNotAllowed());
		// Only the POST deleted
		verify(jobservice).deleteJob(anyInt());
	}

	private void expectError(MockHttpServletRequestBuilder request, String errorMessage) throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(view().name("createjob"))
				.andExpect(model().attribute("errorMessage", errorMessage));
	}

	private static MockHttpServletRequestBuilder save(String jobkey, String positionkey, String vacancies,
			String closingdate, String status) {
		return post("/savejob").param("jobkey", jobkey).param("positionkey", positionkey)
				.param("vacancies", vacancies).param("closingdate", closingdate).param("status", status);
	}

	private static JobInfo job(int jobkey, int positionkey, String status) {
		JobInfo job = new JobInfo();
		job.setJobkey(jobkey);
		job.setPositionkey(positionkey);
		job.setStatus(status);
		return job;
	}

	private static PositionInfo position(int key, String name) {
		PositionInfo position = new PositionInfo();
		position.setPositionkey(key);
		position.setPositionname(name);
		return position;
	}

	@Test
	void jobChangesAreLogged() throws Exception {
		mockMvc.perform(save("0", "2", "3", "2030-12-31", "OPEN")).andExpect(redirectedUrl("/viewjoblist"));
		mockMvc.perform(post("/deletejob/2")).andExpect(redirectedUrl("/viewjoblist"));

		verify(activityservice).record(isNull(), eq("JOB_ADDED"), eq("JOB"), isNull(), eq("position 2"));
		verify(activityservice).record(isNull(), eq("JOB_DELETED"), eq("JOB"), eq("2"), isNull());
	}
}
