package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.ImportPreview;
import rms.model.Role;
import rms.service.CandidateImportService;
import rms.service.Outcome;

/** F4: the import's three steps; the file's cells wait in the session between preview and import. */
@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

	@Mock
	CandidateImportService importservice;

	@InjectMocks
	ImportController controller;

	MockMvc mockMvc;

	MockHttpSession hr;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(new WebConfig().viewResolver()).build();
		hr = EvaluationControllerTest.session("U3", Role.HR);
	}

	@Test
	void theTemplateIsTheHeaderLine() throws Exception {
		mockMvc.perform(get("/candidatetemplate"))
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"candidate-import-template.csv\""))
				.andExpect(content().bytes(("﻿First name,Last name,Position,Language,E-mail,Phone,Source,"
						+ "Applied date,Job Id\r\n").getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void aPreviewKeepsTheCellsAndTheImportUsesThemOnce() throws Exception {
		when(importservice.preview(any())).thenReturn(new ImportPreview());
		when(importservice.importCandidates(any(), eq(true), eq("U3"))).thenReturn(Outcome.ok("2 candidates imported."));

		mockMvc.perform(multipart("/previewimport").file(csv("First name,Last name\r\nNora,Newcomer\r\n")).session(hr))
				.andExpect(view().name("importcandidates"))
				.andExpect(model().attributeExists("preview"));
		assertThat(hr.getAttribute(ImportController.SESSION_KEY))
				.isEqualTo(List.of(List.of("First name", "Last name"), List.of("Nora", "Newcomer")));

		mockMvc.perform(post("/confirmimport").param("includeduplicates", "true").session(hr))
				.andExpect(redirectedUrl("/viewcandidatelist"))
				.andExpect(flash().attribute("message", "2 candidates imported."));
		assertThat(hr.getAttribute(ImportController.SESSION_KEY)).isNull();

		// A second confirm (a double click, or the back button) imports nothing
		mockMvc.perform(post("/confirmimport").session(hr))
				.andExpect(model().attribute("errorMessage",
						"Please upload the file again: the preview has expired or was imported already."));
	}

	@Test
	void aFileProblemShowsNoPreviewAndKeepsNothing() throws Exception {
		ImportPreview broken = new ImportPreview();
		broken.setProblem("The file is empty.");
		when(importservice.preview(any())).thenReturn(broken);

		mockMvc.perform(multipart("/previewimport").file(csv("")).session(hr));
		mockMvc.perform(multipart("/previewimport").file(csv("x")).session(hr))
				.andExpect(model().attribute("errorMessage", "The file is empty."))
				.andExpect(model().attributeDoesNotExist("preview"));

		assertThat(hr.getAttribute(ImportController.SESSION_KEY)).isNull();
	}

	@Test
	void noFileOrATooLargeOneIsRefusedBeforeReading() throws Exception {
		mockMvc.perform(multipart("/previewimport").session(hr))
				.andExpect(model().attribute("errorMessage", "Please choose a CSV file."));
		byte[] large = new byte[(int) ImportController.MAX_BYTES + 1];
		mockMvc.perform(multipart("/previewimport").file(new MockMultipartFile("file", "big.csv", "text/csv", large))
				.session(hr)).andExpect(model().attribute("errorMessage", "The file is larger than 512 KB. Please split it."));

		verify(importservice, never()).preview(any());
		verify(importservice, never()).importCandidates(any(), anyBoolean(), anyString());
	}

	@Test
	void theFormStartsWithoutAnOldPreview() throws Exception {
		hr.setAttribute(ImportController.SESSION_KEY, List.of());

		mockMvc.perform(get("/importcandidates").session(hr))
				.andExpect(view().name("importcandidates"))
				.andExpect(model().attribute("maxrows", 500));

		assertThat(hr.getAttribute(ImportController.SESSION_KEY)).isNull();
	}

	private static MockMultipartFile csv(String text) {
		return new MockMultipartFile("file", "candidates.csv", "text/csv", text.getBytes(StandardCharsets.UTF_8));
	}
}
