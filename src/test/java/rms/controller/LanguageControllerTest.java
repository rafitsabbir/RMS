package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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

/** Characterization tests: pin current Language master behaviour. */
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
		when(languageservice.addLanguage(any(LanguageInfo.class))).thenReturn(true);

		mockMvc.perform(post("/savelanguage").param("languagekey", "0").param("languagename", "kotlin"))
				.andExpect(redirectedUrl("/viewlanguagelist"));

		ArgumentCaptor<LanguageInfo> captor = ArgumentCaptor.forClass(LanguageInfo.class);
		verify(languageservice).addLanguage(captor.capture());
		verify(languageservice, never()).updateLanguage(any(LanguageInfo.class));
		assertThat(captor.getValue().getLanguagename()).isEqualTo("kotlin");
	}

	@Test
	void saveWithKeyUpdatesAndRedirectsToList() throws Exception {
		when(languageservice.findLanguageById(7)).thenReturn(new LanguageInfo());
		when(languageservice.updateLanguage(any(LanguageInfo.class))).thenReturn(true);

		mockMvc.perform(post("/savelanguage").param("languagekey", "7").param("languagename", "go"))
				.andExpect(redirectedUrl("/viewlanguagelist"));

		ArgumentCaptor<LanguageInfo> captor = ArgumentCaptor.forClass(LanguageInfo.class);
		verify(languageservice).updateLanguage(captor.capture());
		verify(languageservice, never()).addLanguage(any(LanguageInfo.class));
		assertThat(captor.getValue().getLanguagekey()).isEqualTo(7);
		assertThat(captor.getValue().getLanguagename()).isEqualTo("go");
	}

	@Test
	void saveWithBlankNameShowsFormWithError() throws Exception {
		mockMvc.perform(post("/savelanguage").param("languagekey", "0").param("languagename", "   "))
				.andExpect(status().isOk())
				.andExpect(view().name("createlanguage"))
				.andExpect(model().attribute("errorMessage", "Please enter a language name."));

		verifyNoInteractions(languageservice);
	}

	@Test
	void saveWithoutNameShowsFormWithError() throws Exception {
		mockMvc.perform(post("/savelanguage").param("languagekey", "0"))
				.andExpect(view().name("createlanguage"))
				.andExpect(model().attribute("errorMessage", "Please enter a language name."));

		verifyNoInteractions(languageservice);
	}

	@Test
	void addOfDuplicateShowsFormWithErrorAndKeepsInput() throws Exception {
		when(languageservice.addLanguage(any(LanguageInfo.class))).thenReturn(false);

		mockMvc.perform(post("/savelanguage").param("languagekey", "0").param("languagename", " kotlin "))
				.andExpect(status().isOk())
				.andExpect(view().name("createlanguage"))
				.andExpect(model().attribute("errorMessage", "Language KOTLIN already exists."))
				.andExpect(model().attribute("languageinfo", hasProperty("languagename", is(" kotlin "))));
	}

	@Test
	void updateToDuplicateShowsFormWithErrorAndKeepsKey() throws Exception {
		when(languageservice.findLanguageById(7)).thenReturn(new LanguageInfo());
		when(languageservice.updateLanguage(any(LanguageInfo.class))).thenReturn(false);

		mockMvc.perform(post("/savelanguage").param("languagekey", "7").param("languagename", "go"))
				.andExpect(view().name("createlanguage"))
				.andExpect(model().attribute("errorMessage", "Language GO already exists."))
				.andExpect(model().attribute("languageinfo", hasProperty("languagekey", is(7))));
	}

	@Test
	void saveOfDeletedLanguageIs404() throws Exception {
		when(languageservice.findLanguageById(7)).thenReturn(null);

		mockMvc.perform(post("/savelanguage").param("languagekey", "7").param("languagename", "x"))
				.andExpect(status().isNotFound());

		verify(languageservice, never()).updateLanguage(any(LanguageInfo.class));
		verify(languageservice, never()).addLanguage(any(LanguageInfo.class));
	}

	@Test
	void listShowsAllLanguagesFromService() throws Exception {
		List<LanguageInfo> items = Arrays.asList(new LanguageInfo(), new LanguageInfo());
		when(languageservice.getAllLanguage()).thenReturn(items);

		mockMvc.perform(get("/viewlanguagelist"))
				.andExpect(view().name("viewlanguage"))
				.andExpect(model().attribute("languagelist", items));
	}

	@Test
	void editLoadsLanguageIntoCreateForm() throws Exception {
		LanguageInfo item = new LanguageInfo();
		item.setLanguagekey(7);
		when(languageservice.findLanguageById(7)).thenReturn(item);

		mockMvc.perform(get("/updatelanguage/7"))
				.andExpect(view().name("createlanguage"))
				.andExpect(model().attribute("languageinfo", item));
	}

	@Test
	void editOfMissingOrDeletedLanguageIs404() throws Exception {
		when(languageservice.findLanguageById(99)).thenReturn(null);

		mockMvc.perform(get("/updatelanguage/99")).andExpect(status().isNotFound());
	}

	@Test
	void deleteIsAPostThatRedirectsToList() throws Exception {
		mockMvc.perform(post("/deletelanguage/7"))
				.andExpect(redirectedUrl("/viewlanguagelist"));

		verify(languageservice).deleteLanguage(7);
	}

	@Test
	void deleteByGetIsRefused() throws Exception {
		mockMvc.perform(get("/deletelanguage/7")).andExpect(status().isMethodNotAllowed());

		verifyNoInteractions(languageservice);
	}

}
