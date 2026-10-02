package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.Map;
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
import rms.model.CandidateInfo;
import rms.model.DocumentInfo;
import rms.model.DocumentType;
import rms.model.JobInfo;
import rms.model.Role;
import rms.model.LanguageInfo;
import rms.model.PositionInfo;
import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.DocumentService;
import rms.service.JobService;
import rms.service.LanguageService;
import rms.service.PositionService;

/** Characterization tests: pin the candidate management behaviour (G1). */
@ExtendWith(MockitoExtension.class)
class CandidateControllerTest {

	@Mock
	CandidateService candidateservice;

	@Mock
	PositionService positionservice;

	@Mock
	LanguageService languageservice;

	@Mock
	JobService jobservice;

	@Mock
	DocumentService documentservice;

	@Mock
	AssignmentService assignmentservice;

	MockHttpSession staff = EvaluationControllerTest.session("U1", Role.SUPER_ADMIN);

	@InjectMocks
	CandidateController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
		// The seed's active rows: positions 1-2, languages 1-2 (3 is deleted in both)
		lenient().when(positionservice.getAllPosition()).thenReturn(new ArrayList<>(List.of(
				position(1, "SOFTWARE ENGINEER"), position(2, "QA ENGINEER"))));
		lenient().when(languageservice.getAllLanguage()).thenReturn(new ArrayList<>(List.of(
				language(1, "JAVA"), language(2, "PYTHON"))));
		lenient().when(positionservice.findPositionById(1)).thenReturn(position(1, "SOFTWARE ENGINEER"));
		lenient().when(positionservice.findPositionById(2)).thenReturn(position(2, "QA ENGINEER"));
		lenient().when(languageservice.findLanguageById(1)).thenReturn(language(1, "JAVA"));
		lenient().when(languageservice.findLanguageById(2)).thenReturn(language(2, "PYTHON"));
		// The seed's jobs: 1 open (position 1), 2 closed (position 2); 3 is deleted
		lenient().when(jobservice.getOpenJob()).thenReturn(new ArrayList<>(List.of(job(1, 1, "OPEN"))));
		lenient().when(jobservice.findJobById(1)).thenReturn(job(1, 1, "OPEN"));
		lenient().when(jobservice.findJobById(2)).thenReturn(job(2, 2, "CLOSED"));
	}

	@Test
	void createFormHasEmptyCandidateAndActiveChoices() throws Exception {
		mockMvc.perform(get("/createcandidate"))
				.andExpect(status().isOk())
				.andExpect(view().name("createcandidate"))
				.andExpect(model().attribute("update", false))
				.andExpect(model().attribute("candidateinfo", hasProperty("candidateid", is((String) null))))
				.andExpect(model().attribute("positionlist", contains(
						hasProperty("positionkey", is(1)), hasProperty("positionkey", is(2)))))
				.andExpect(model().attribute("languagelist", contains(
						hasProperty("languagekey", is(1)), hasProperty("languagekey", is(2)))))
				.andExpect(model().attributeDoesNotExist("errorMessage"));
	}

	@Test
	void saveNewCandidateTrimsAddsAndRedirectsToList() throws Exception {
		when(candidateservice.addCandidate(any(CandidateInfo.class))).thenReturn("C3");

		mockMvc.perform(save(null, " Dana ", " Doe ", "2", "1"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		ArgumentCaptor<CandidateInfo> captor = ArgumentCaptor.forClass(CandidateInfo.class);
		verify(candidateservice).addCandidate(captor.capture());
		verify(candidateservice, never()).updateCandidate(any(CandidateInfo.class));
		CandidateInfo saved = captor.getValue();
		// The DAO generates the ID
		assertThat(saved.getCandidateid()).isNull();
		assertThat(saved.getFirstname()).isEqualTo("Dana");
		assertThat(saved.getLastname()).isEqualTo("Doe");
		assertThat(saved.getPositionkey()).isEqualTo(2);
		assertThat(saved.getLanguagekey()).isEqualTo(1);
	}

	@Test
	void saveNewCandidateIgnoresAPostedId() throws Exception {
		when(candidateservice.addCandidate(any(CandidateInfo.class))).thenReturn("C3");

		mockMvc.perform(save("C1", "Dana", "Doe", "1", "1"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		ArgumentCaptor<CandidateInfo> captor = ArgumentCaptor.forClass(CandidateInfo.class);
		verify(candidateservice).addCandidate(captor.capture());
		verify(candidateservice, never()).findCandidateById(any());
		assertThat(captor.getValue().getCandidateid()).isNull();
	}
	@Test
	void saveRefusesMissingValues() throws Exception {
		expectError(save("C3", " ", "Doe", "1", "1"), "Please enter the first name.");
		expectError(post("/savecandidate").param("candidateid", "C3").param("lastname", "Doe")
				.param("positionkey", "1").param("languagekey", "1"), "Please enter the first name.");
		expectError(save("C3", "Dana", "", "1", "1"), "Please enter the last name.");
		expectError(save("C3", "Dana", "Doe", "0", "1"), "Please choose a position.");
		expectError(save("C3", "Dana", "Doe", "1", "0"), "Please choose a language.");

		verify(candidateservice, never()).addCandidate(any(CandidateInfo.class));
	}

	@Test
	void saveNewCandidateRefusesDeletedOrUnknownChoices() throws Exception {
		expectError(save("C3", "Dana", "Doe", "3", "1"), "Please choose a position.");
		expectError(save("C3", "Dana", "Doe", "1", "99"), "Please choose a language.");

		verify(candidateservice, never()).addCandidate(any(CandidateInfo.class));
	}

	@Test
	void saveWithUpdateFlagUpdatesAndRedirectsToList() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1", 1, 1));

		mockMvc.perform(save("C1", "Carla", "Smith", "2", "2").param("update", "true"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		ArgumentCaptor<CandidateInfo> captor = ArgumentCaptor.forClass(CandidateInfo.class);
		verify(candidateservice).updateCandidate(captor.capture());
		verify(candidateservice, never()).addCandidate(any(CandidateInfo.class));
		assertThat(captor.getValue().getLastname()).isEqualTo("Smith");
		assertThat(captor.getValue().getPositionkey()).isEqualTo(2);
	}

	@Test
	void updateMayKeepADeletedPositionAndLanguage() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1", 3, 3));

		mockMvc.perform(save("C1", "Carla", "Candidate", "3", "3").param("update", "true"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		verify(candidateservice).updateCandidate(any(CandidateInfo.class));
	}

	@Test
	void updateMayNotMoveToADeletedPosition() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1", 1, 1));

		mockMvc.perform(save("C1", "Carla", "Candidate", "3", "1").param("update", "true"))
				.andExpect(view().name("createcandidate"))
				.andExpect(model().attribute("update", true))
				.andExpect(model().attribute("errorMessage", "Please choose a position."));

		verify(candidateservice, never()).updateCandidate(any(CandidateInfo.class));
	}

	@Test
	void saveWithUpdateFlagForMissingCandidateIs404() throws Exception {
		mockMvc.perform(save("C9", "Dana", "Doe", "1", "1").param("update", "true"))
				.andExpect(status().isNotFound());

		verify(candidateservice, never()).updateCandidate(any(CandidateInfo.class));
		verify(candidateservice, never()).addCandidate(any(CandidateInfo.class));
	}

	@Test
	void editFormIsPrefilled() throws Exception {
		CandidateInfo stored = candidate("C1", 1, 1);
		when(candidateservice.findCandidateById("C1")).thenReturn(stored);

		mockMvc.perform(get("/updatecandidate").param("candidateid", "C1"))
				.andExpect(status().isOk())
				.andExpect(view().name("createcandidate"))
				.andExpect(model().attribute("update", true))
				.andExpect(model().attribute("candidateinfo", stored))
				.andExpect(model().attribute("positionlist", contains(
						hasProperty("positionkey", is(1)), hasProperty("positionkey", is(2)))));
	}

	@Test
	void editFormOffersTheCandidatesDeletedPositionAndLanguage() throws Exception {
		CandidateInfo stored = candidate("C1", 3, 3);
		stored.setPositionname("RETIRED ROLE");
		when(candidateservice.findCandidateById("C1")).thenReturn(stored);

		mockMvc.perform(get("/updatecandidate").param("candidateid", "C1"))
				.andExpect(model().attribute("positionlist", contains(
						hasProperty("positionname", is("SOFTWARE ENGINEER")),
						hasProperty("positionname", is("QA ENGINEER")),
						hasProperty("positionname", is("RETIRED ROLE (deleted)")))))
				// No language row joined (a missing key): the key stands in for the name
				.andExpect(model().attribute("languagelist", contains(
						hasProperty("languagename", is("JAVA")),
						hasProperty("languagename", is("PYTHON")),
						hasProperty("languagename", is("Language 3 (deleted)")))));
	}

	@Test
	void editFormForMissingCandidateIs404() throws Exception {
		mockMvc.perform(get("/updatecandidate").param("candidateid", "C9"))
				.andExpect(status().isNotFound());
	}

	@Test
	void listShowsAllCandidates() throws Exception {
		List<CandidateInfo> candidates = List.of(candidate("C1", 1, 1), candidate("C2", 2, 2));
		when(candidateservice.getAllCandidate()).thenReturn(candidates);

		mockMvc.perform(get("/viewcandidatelist"))
				.andExpect(status().isOk())
				.andExpect(view().name("viewcandidate"))
				.andExpect(model().attribute("candidatelist", candidates));
	}

	@Test
	void saveIsPostOnly() throws Exception {
		mockMvc.perform(get("/savecandidate").param("candidateid", "C3"))
				.andExpect(status().isMethodNotAllowed());

		verify(candidateservice, never()).addCandidate(any(CandidateInfo.class));
	}

	@Test
	void deleteSoftDeletesAndRedirectsToList() throws Exception {
		mockMvc.perform(post("/deletecandidate").param("candidateid", "C2"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		verify(candidateservice).deleteCandidate("C2");
	}

	@Test
	void deleteIsPostOnly() throws Exception {
		mockMvc.perform(get("/deletecandidate").param("candidateid", "C2"))
				.andExpect(status().isMethodNotAllowed());

		verify(candidateservice, never()).deleteCandidate(any());
	}

	// --- Phase 2: contact details, source, applied date, job, profile ---

	@Test
	void saveKeepsTheContactDetailsSourceAndAppliedDate() throws Exception {
		when(candidateservice.addCandidate(any(CandidateInfo.class))).thenReturn("C3");

		mockMvc.perform(save(null, "Dana", "Doe", "1", "1").param("email", " dana@example.test ")
				.param("phone", "+1 (555) 010-0000").param("source", "AGENCY").param("applieddate", "2026-09-20"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		CandidateInfo saved = added();
		assertThat(saved.getEmail()).isEqualTo("dana@example.test");
		assertThat(saved.getPhone()).isEqualTo("+1 (555) 010-0000");
		assertThat(saved.getSource()).isEqualTo("AGENCY");
		assertThat(saved.getApplieddate()).isEqualTo(LocalDate.of(2026, 9, 20));
		assertThat(saved.getJobkey()).isZero();
	}

	@Test
	void optionalFieldsMayBeEmpty() throws Exception {
		when(candidateservice.addCandidate(any(CandidateInfo.class))).thenReturn("C3");

		mockMvc.perform(save(null, "Dana", "Doe", "1", "1").param("email", "").param("phone", "")
				.param("source", "").param("applieddate", "").param("jobkey", "0"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		CandidateInfo saved = added();
		assertThat(saved.getEmail()).isEmpty();
		assertThat(saved.getApplieddate()).isNull();
	}

	@Test
	void saveRefusesBadContactDetailsSourceAndDate() throws Exception {
		expectError(save(null, "Dana", "Doe", "1", "1").param("email", "dana"),
				"Please enter a valid e-mail address, or leave it empty.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("email", "dana @example.test"),
				"Please enter a valid e-mail address, or leave it empty.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("email", "a@" + "x".repeat(150) + ".test"),
				"Please enter a valid e-mail address, or leave it empty.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("phone", "call me"),
				"Please enter a valid phone number (digits, spaces and + ( ) - . /), or leave it empty.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("phone", "1".repeat(31)),
				"Please enter a valid phone number (digits, spaces and + ( ) - . /), or leave it empty.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("source", "NEWSPAPER"),
				"Please choose where the candidate came from, or leave it empty.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("applieddate", "20/09/2026"),
				"Please enter the applied date as a date, or leave it empty.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("applieddate", LocalDate.now().plusDays(1).toString()),
				"The applied date can't be in the future.");

		verify(candidateservice, never()).addCandidate(any(CandidateInfo.class));
	}

	@Test
	void anOpenJobSetsThePosition() throws Exception {
		when(candidateservice.addCandidate(any(CandidateInfo.class))).thenReturn("C3");

		// Position 2 chosen, but job 1 is for position 1 (decision M)
		mockMvc.perform(save(null, "Dana", "Doe", "2", "1").param("jobkey", "1"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		CandidateInfo saved = added();
		assertThat(saved.getJobkey()).isEqualTo(1);
		assertThat(saved.getPositionkey()).isEqualTo(1);
	}

	@Test
	void withAJobNoPositionNeedsToBeChosen() throws Exception {
		when(candidateservice.addCandidate(any(CandidateInfo.class))).thenReturn("C3");

		mockMvc.perform(save(null, "Dana", "Doe", "0", "1").param("jobkey", "1"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		assertThat(added().getPositionkey()).isEqualTo(1);
	}

	@Test
	void newCandidateCantGetAClosedDeletedOrUnknownJob() throws Exception {
		expectError(save(null, "Dana", "Doe", "1", "1").param("jobkey", "2"),
				"That job is closed. Please choose an open job, or none.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("jobkey", "3"), "Please choose an open job, or none.");
		expectError(save(null, "Dana", "Doe", "1", "1").param("jobkey", "x"), "Please choose a job, or none.");

		verify(candidateservice, never()).addCandidate(any(CandidateInfo.class));
	}

	@Test
	void editMayKeepAClosedOrDeletedJob() throws Exception {
		CandidateInfo closed = candidate("C1", 2, 1);
		closed.setJobkey(2);
		when(candidateservice.findCandidateById("C1")).thenReturn(closed);
		mockMvc.perform(save("C1", "Carla", "Candidate", "1", "1").param("jobkey", "2").param("update", "true"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		CandidateInfo deleted = candidate("C2", 1, 1);
		deleted.setJobkey(3);
		when(candidateservice.findCandidateById("C2")).thenReturn(deleted);
		mockMvc.perform(save("C2", "Cody", "Candidate", "2", "1").param("jobkey", "3").param("update", "true"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		ArgumentCaptor<CandidateInfo> captor = ArgumentCaptor.forClass(CandidateInfo.class);
		verify(candidateservice, times(2)).updateCandidate(captor.capture());
		// The closed job's position, and for the deleted job the position the candidate already had
		assertThat(captor.getAllValues()).extracting(CandidateInfo::getPositionkey).containsExactly(2, 1);
	}

	@Test
	void editFormOffersTheCandidatesClosedOrDeletedJob() throws Exception {
		CandidateInfo stored = candidate("C1", 1, 1);
		stored.setJobkey(3);
		stored.setPositionname("SOFTWARE ENGINEER");
		when(candidateservice.findCandidateById("C1")).thenReturn(stored);

		mockMvc.perform(get("/updatecandidate").param("candidateid", "C1"))
				.andExpect(model().attribute("joblist", contains(
						hasProperty("label", is("Job 1: SOFTWARE ENGINEER")),
						hasProperty("label", is("Job 3 (deleted)")))))
				.andExpect(model().attributeExists("sourcelist"));
	}

	@Test
	void postedFieldsOutsideTheFormAreIgnored() throws Exception {
		when(candidateservice.addCandidate(any(CandidateInfo.class))).thenReturn("C3");

		mockMvc.perform(save(null, "Dana", "Doe", "1", "1").param("candidatestatus", "S").param("cvcount", "1"))
				.andExpect(redirectedUrl("/viewcandidatelist"));

		CandidateInfo saved = added();
		assertThat(saved.getCandidatestatus()).isNull();
		assertThat(saved.getCvcount()).isZero();
	}

	@Test
	@SuppressWarnings("unchecked")
	void profileGroupsTheDocumentsByKind() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1", 1, 1));
		when(documentservice.getDocuments("C1")).thenReturn(List.of(document(1, "CV"), document(2, "PROFESSIONAL"),
				document(3, "PROFESSIONAL"), document(4, "FUTURE_KIND")));
		when(documentservice.isStorageConfigured()).thenReturn(true);

		Map<DocumentType, List<DocumentInfo>> documents = (Map<DocumentType, List<DocumentInfo>>) mockMvc
				.perform(get("/viewcandidate").session(staff).param("candidateid", "C1"))
				.andExpect(status().isOk())
				.andExpect(view().name("candidateprofile"))
				.andExpect(model().attribute("storageready", true))
				.andExpect(model().attribute("selecteddoctype", "CV"))
				.andExpect(model().attributeDoesNotExist("documentError"))
				.andReturn().getModelAndView().getModel().get("documents");

		assertThat(documents.get(DocumentType.CV)).extracting(DocumentInfo::getDocumentkey).containsExactly(1);
		assertThat(documents.get(DocumentType.PROFESSIONAL)).extracting(DocumentInfo::getDocumentkey)
				.containsExactly(2, 3);
		assertThat(documents.get(DocumentType.PHD)).isEmpty();
		// A kind this version doesn't know isn't shown
		assertThat(documents.values().stream().mapToInt(List::size).sum()).isEqualTo(3);
	}

	@Test
	void profilePreselectsTheKindAndShowsTheSizeMessage() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1", 1, 1));

		mockMvc.perform(get("/viewcandidate").session(staff).param("candidateid", "C1").param("doctype", "PHD").param("toolarge", ""))
				.andExpect(model().attribute("selecteddoctype", "PHD"))
				.andExpect(model().attribute("storageready", false))
				.andExpect(model().attribute("documentError", "The file is larger than 5 MB."));
		mockMvc.perform(get("/viewcandidate").session(staff).param("candidateid", "C1").param("doctype", "../etc"))
				.andExpect(model().attribute("selecteddoctype", "CV"));
	}

	@Test
	void profileOfAMissingOrDeletedCandidateIs404() throws Exception {
		mockMvc.perform(get("/viewcandidate").session(staff).param("candidateid", "C9")).andExpect(status().isNotFound());

		verify(documentservice, never()).getDocuments(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void profileWithoutDocumentsHasAnEmptyGroupPerKind() throws Exception {
		when(candidateservice.findCandidateById("C2")).thenReturn(candidate("C2", 2, 2));

		Map<DocumentType, List<DocumentInfo>> documents = (Map<DocumentType, List<DocumentInfo>>) mockMvc
				.perform(get("/viewcandidate").session(staff).param("candidateid", "C2"))
				.andExpect(model().attribute("doctypes", DocumentType.values()))
				.andReturn().getModelAndView().getModel().get("documents");

		assertThat(documents).containsOnlyKeys(DocumentType.values());
		assertThat(documents.values()).allMatch(List::isEmpty);
	}

	// --- Phase 3: interviewers on the profile ---

	@Test
	void staffSeeTheAssignmentsAndHrCanAssign() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1", 1, 1));
		List<rms.model.AssignmentInfo> assignments = List.of(new rms.model.AssignmentInfo());
		when(assignmentservice.getAssignments("C1")).thenReturn(assignments);
		when(assignmentservice.getAssignableInterviewers("C1")).thenReturn(List.of());

		mockMvc.perform(get("/viewcandidate").session(staff).param("candidateid", "C1"))
				.andExpect(model().attribute("assignments", assignments))
				.andExpect(model().attributeExists("assignable"));
		// A Hiring Manager sees them but can't assign
		mockMvc.perform(get("/viewcandidate").param("candidateid", "C1")
				.session(EvaluationControllerTest.session("U4", Role.HIRING_MANAGER)))
				.andExpect(model().attribute("assignments", assignments))
				.andExpect(model().attributeDoesNotExist("assignable"));
	}

	@Test
	void anInterviewerSeesOnlyAssignedCandidatesWithoutTheAssignments() throws Exception {
		when(candidateservice.findCandidateById("C1")).thenReturn(candidate("C1", 1, 1));
		MockHttpSession interviewer = EvaluationControllerTest.session("U2", Role.INTERVIEWER);
		when(assignmentservice.isAssigned("C1", "U2")).thenReturn(true);

		mockMvc.perform(get("/viewcandidate").session(interviewer).param("candidateid", "C1"))
				.andExpect(status().isOk())
				.andExpect(model().attributeDoesNotExist("assignments", "assignable"));

		when(assignmentservice.isAssigned("C1", "U2")).thenReturn(false);
		mockMvc.perform(get("/viewcandidate").session(interviewer).param("candidateid", "C1"))
				.andExpect(status().isForbidden());
		// No role at all: refused too (fails closed)
		mockMvc.perform(get("/viewcandidate").param("candidateid", "C1")).andExpect(status().isForbidden());
	}

	private CandidateInfo added() {
		ArgumentCaptor<CandidateInfo> captor = ArgumentCaptor.forClass(CandidateInfo.class);
		verify(candidateservice).addCandidate(captor.capture());
		return captor.getValue();
	}

	private static JobInfo job(int jobkey, int positionkey, String status) {
		JobInfo job = new JobInfo();
		job.setJobkey(jobkey);
		job.setPositionkey(positionkey);
		job.setPositionname(positionkey == 1 ? "SOFTWARE ENGINEER" : "QA ENGINEER");
		job.setStatus(status);
		return job;
	}

	private static DocumentInfo document(int key, String doctype) {
		DocumentInfo document = new DocumentInfo();
		document.setDocumentkey(key);
		document.setCandidateid("C1");
		document.setDoctype(doctype);
		return document;
	}

	private void expectError(MockHttpServletRequestBuilder request, String errorMessage) throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(view().name("createcandidate"))
				.andExpect(model().attribute("errorMessage", errorMessage));
	}

	private static MockHttpServletRequestBuilder save(String id, String first, String last, String positionkey,
			String languagekey) {
		MockHttpServletRequestBuilder request = post("/savecandidate");
		if (id != null) {
			request.param("candidateid", id);
		}
		return request.param("firstname", first).param("lastname", last)
				.param("positionkey", positionkey).param("languagekey", languagekey);
	}

	private static CandidateInfo candidate(String id, int positionkey, int languagekey) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid(id);
		candidate.setFirstname("Carla");
		candidate.setLastname("Candidate");
		candidate.setPositionkey(positionkey);
		candidate.setLanguagekey(languagekey);
		return candidate;
	}

	private static PositionInfo position(int key, String name) {
		PositionInfo position = new PositionInfo();
		position.setPositionkey(key);
		position.setPositionname(name);
		return position;
	}

	private static LanguageInfo language(int key, String name) {
		LanguageInfo language = new LanguageInfo();
		language.setLanguagekey(key);
		language.setLanguagename(name);
		return language;
	}
}
