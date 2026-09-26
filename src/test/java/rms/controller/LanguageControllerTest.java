package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.LanguageInfo;
import rms.service.LanguageService;

/** Characterization tests: pin current Language master behaviour before any upgrade. */
@ExtendWith(MockitoExtension.class)
class LanguageControllerTest {

	@Mock
	LanguageService languageservice;

	@InjectMocks
	LanguageController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void createFormHasEmptyLanguageInfo() throws Exception {
		mockMvc.perform(get("/createlanguage"))
				.andExpect(view().name("createlanguage"))
				.andExpect(model().attributeExists("languageinfo"));
	}

	@Test
	void saveWithoutKeyAddsAndRedirectsToList() throws Exception {
		mockMvc.perform(post("/savelanguage").param("languagekey", "0").param("languagename", "kotlin"))
				.andExpect(redirectedUrl("/viewlanguagelist"));

		ArgumentCaptor<LanguageInfo> captor = ArgumentCaptor.forClass(LanguageInfo.class);
		verify(languageservice).addLanguage(captor.capture());
		verify(languageservice, never()).updateLanguage(any(LanguageInfo.class));
		assertThat(captor.getValue().getLanguagename()).isEqualTo("kotlin");
	}

	@Test
	void saveWithKeyUpdatesAndRedirectsToList() throws Exception {
		mockMvc.perform(post("/savelanguage").param("languagekey", "7").param("languagename", "go"))
				.andExpect(redirectedUrl("/viewlanguagelist"));

		ArgumentCaptor<LanguageInfo> captor = ArgumentCaptor.forClass(LanguageInfo.class);
		verify(languageservice).updateLanguage(captor.capture());
		verify(languageservice, never()).addLanguage(any(LanguageInfo.class));
		assertThat(captor.getValue().getLanguagekey()).isEqualTo(7);
		assertThat(captor.getValue().getLanguagename()).isEqualTo("go");
	}

	@Test
	void listShowsAllLanguagesFromService() throws Exception {
		List<LanguageInfo> languages = Arrays.asList(new LanguageInfo(), new LanguageInfo());
		when(languageservice.getAllLanguage()).thenReturn(languages);

		mockMvc.perform(get("/viewlanguagelist"))
				.andExpect(view().name("viewlanguage"))
				.andExpect(model().attribute("languagelist", languages));
	}

	@Test
	void editLoadsLanguageIntoCreateForm() throws Exception {
		LanguageInfo language = new LanguageInfo();
		language.setLanguagekey(7);
		when(languageservice.findLanguageById(7)).thenReturn(language);

		mockMvc.perform(get("/updatelanguage/7"))
				.andExpect(view().name("createlanguage"))
				.andExpect(model().attribute("languageinfo", language));
	}

	@Test
	void deleteIsAGetThatRedirectsToList() throws Exception {
		// characterizes G17: delete is a GET and a hard delete
		mockMvc.perform(get("/deletelanguage/7"))
				.andExpect(redirectedUrl("/viewlanguagelist"));

		verify(languageservice).deleteLanguage(7);
	}

}
