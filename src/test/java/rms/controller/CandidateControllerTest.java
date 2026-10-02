package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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
import rms.model.CandidateInfo;
import rms.model.LanguageInfo;
import rms.model.PositionInfo;
import rms.service.CandidateService;
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
