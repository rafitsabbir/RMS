package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.CandidateInfo;
import rms.model.DocumentInfo;
import rms.model.DocumentUpload;
import rms.model.UserInfo;
import rms.service.CandidateService;
import rms.service.DocumentOutcome;
import rms.service.DocumentService;

/** Uploads, downloads, deletes and the permanent delete, through the controller (the checks are in the service). */
@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

	static final byte[] PDF = "%PDF-1.7 synthetic".getBytes();

	@TempDir
	Path folder;

	@Mock
	DocumentService documentservice;

	@Mock
	CandidateService candidateservice;

	@InjectMocks
	DocumentController controller;

	MockMvc mockMvc;

	MockHttpSession session;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
		UserInfo user = new UserInfo();
		user.setUserid("U3");
		session = new MockHttpSession();
		session.setAttribute("user", user);
	}

	@Test
	void uploadPassesTheFileToTheServiceAndReturnsToTheProfile() throws Exception {
		givenCandidate("C1");
		when(documentservice.upload(any(DocumentUpload.class), any())).thenReturn(new DocumentOutcome(true, "CV uploaded."));

		mockMvc.perform(upload("C1", "CV", new MockMultipartFile("file", "cv.pdf", "application/pdf", PDF)))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1"))
				.andExpect(flash().attribute("documentMessage", "CV uploaded."));

		ArgumentCaptor<DocumentUpload> captor = ArgumentCaptor.forClass(DocumentUpload.class);
		verify(documentservice).upload(captor.capture(), any());
		DocumentUpload upload = captor.getValue();
		assertThat(upload.candidateid()).isEqualTo("C1");
		assertThat(upload.doctype()).isEqualTo("CV");
		assertThat(upload.originalname()).isEqualTo("cv.pdf");
		assertThat(upload.declaredtype()).isEqualTo("application/pdf");
		assertThat(upload.content()).isEqualTo(PDF);
		verify(documentservice).upload(any(DocumentUpload.class), eq("U3"));
	}

	@Test
	void uploadAsTheBrowserSendsItWithTheIdOnlyInTheUrl() throws Exception {
		// candidateprofile.jsp: the ID is in the form's action, not also a hidden field (that would read "C1,C1")
		givenCandidate("C1");
		when(documentservice.upload(any(DocumentUpload.class), any())).thenReturn(new DocumentOutcome(true, "CV uploaded."));

		mockMvc.perform(multipart("/uploaddocument?candidateid=C1")
				.file(new MockMultipartFile("file", "cv.pdf", "application/pdf", PDF)).param("doctype", "CV")
				.session(session))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1"));

		ArgumentCaptor<DocumentUpload> captor = ArgumentCaptor.forClass(DocumentUpload.class);
		verify(documentservice).upload(captor.capture(), eq("U3"));
		assertThat(captor.getValue().candidateid()).isEqualTo("C1");
	}

	@Test
	void refusedUploadShowsTheMessageAndKeepsTheKind() throws Exception {
		givenCandidate("C1");
		when(documentservice.upload(any(DocumentUpload.class), any()))
				.thenReturn(new DocumentOutcome(false, "Only PDF, JPG and PNG files can be uploaded."));

		mockMvc.perform(upload("C1", "SSC", new MockMultipartFile("file", "ssc.gif", "image/gif", PDF)))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1&doctype=SSC"))
				.andExpect(flash().attribute("documentError", "Only PDF, JPG and PNG files can be uploaded."));
	}

	@Test
	void uploadWithoutAFileIsRefusedWithoutTheService() throws Exception {
		givenCandidate("C1");

		mockMvc.perform(upload("C1", "CV", new MockMultipartFile("file", "", "application/octet-stream", new byte[0])))
				.andExpect(flash().attribute("documentError", "Please choose a file."));
		mockMvc.perform(multipart("/uploaddocument").param("candidateid", "C1").param("doctype", "CV").session(session))
				.andExpect(flash().attribute("documentError", "Please choose a file."));

		verify(documentservice, never()).upload(any(DocumentUpload.class), any());
	}

	@Test
	void uploadForAMissingOrDeletedCandidateIs404() throws Exception {
		mockMvc.perform(upload("C9", "CV", new MockMultipartFile("file", "cv.pdf", "application/pdf", PDF)))
				.andExpect(status().isNotFound());

		verify(documentservice, never()).upload(any(DocumentUpload.class), any());
	}

	@Test
	void uploadWithoutASessionUserIs403() throws Exception {
		mockMvc.perform(multipart("/uploaddocument").file(new MockMultipartFile("file", "cv.pdf", "application/pdf", PDF))
				.param("candidateid", "C1").param("doctype", "CV"))
				.andExpect(status().isForbidden());

		verify(documentservice, never()).upload(any(DocumentUpload.class), any());
	}

	@Test
	void uploadIsPostOnly() throws Exception {
		mockMvc.perform(get("/uploaddocument").param("candidateid", "C1")).andExpect(status().isMethodNotAllowed());
	}

	@Test
	void redirectEncodesTheCandidateId() throws Exception {
		givenCandidate("C 1+2");
		when(documentservice.upload(any(DocumentUpload.class), any())).thenReturn(new DocumentOutcome(true, "CV uploaded."));

		mockMvc.perform(upload("C 1+2", "CV", new MockMultipartFile("file", "cv.pdf", "application/pdf", PDF)))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C+1%2B2"));
	}

	@Test
	void downloadSendsTheFileAsAnAttachment() throws Exception {
		Path file = Files.write(folder.resolve("stored"), PDF);
		DocumentInfo document = document(1, "C1", "Carla CV (final).pdf", "application/pdf");
		when(documentservice.findDocument(1)).thenReturn(document);
		when(documentservice.isStorageConfigured()).thenReturn(true);
		when(documentservice.findFile(document)).thenReturn(file);

		mockMvc.perform(get("/downloaddocument/1").session(session))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Content-Disposition", containsString("attachment")))
				.andExpect(header().string("Content-Disposition",
						containsString("filename*=UTF-8''Carla%20CV%20%28final%29.pdf")))
				.andExpect(content().bytes(PDF));
	}

	@Test
	void downloadOfAnUnknownOrDeletedDocumentIs404() throws Exception {
		mockMvc.perform(get("/downloaddocument/99").session(session)).andExpect(status().isNotFound());

		verify(documentservice, never()).findFile(any(DocumentInfo.class));
	}

	@Test
	void downloadOfAMissingFileIs404() throws Exception {
		DocumentInfo document = document(1, "C1", "cv.pdf", "application/pdf");
		when(documentservice.findDocument(1)).thenReturn(document);
		when(documentservice.isStorageConfigured()).thenReturn(true);

		mockMvc.perform(get("/downloaddocument/1").session(session)).andExpect(status().isNotFound());
	}

	@Test
	void downloadWithoutStorageIs503() throws Exception {
		when(documentservice.findDocument(1)).thenReturn(document(1, "C1", "cv.pdf", "application/pdf"));

		mockMvc.perform(get("/downloaddocument/1").session(session)).andExpect(status().isServiceUnavailable());

		verify(documentservice, never()).findFile(any(DocumentInfo.class));
	}

	@Test
	void deleteIsSoftAndReturnsToTheProfile() throws Exception {
		DocumentInfo document = document(2, "C1", "ssc.jpg", "image/jpeg");
		document.setDoctype("SSC");
		when(documentservice.findDocument(2)).thenReturn(document);
		when(documentservice.deleteDocument(2, "U3")).thenReturn(true);

		mockMvc.perform(post("/deletedocument/2").session(session))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1"))
				.andExpect(flash().attribute("documentMessage", "SSC certificate deleted."));
	}

	@Test
	void deleteOfADocumentDeletedMeanwhileSaysSo() throws Exception {
		when(documentservice.findDocument(2)).thenReturn(document(2, "C1", "ssc.jpg", "image/jpeg"));
		when(documentservice.deleteDocument(2, "U3")).thenReturn(false);

		mockMvc.perform(post("/deletedocument/2").session(session))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1"))
				.andExpect(flash().attribute("documentError", "The document was already deleted."));
	}

	@Test
	void deleteOfAnUnknownDocumentIs404AndGetIsRefused() throws Exception {
		mockMvc.perform(post("/deletedocument/99").session(session)).andExpect(status().isNotFound());
		mockMvc.perform(get("/deletedocument/2").session(session)).andExpect(status().isMethodNotAllowed());

		verify(documentservice, never()).deleteDocument(anyInt(), anyString());
	}

	@Test
	void purgePassesTheReasonAndShowsTheOutcome() throws Exception {
		givenCandidate("C1");
		when(documentservice.purge("C1", "Retention period ended", "U3"))
				.thenReturn(new DocumentOutcome(true, "3 files deleted permanently."));

		mockMvc.perform(post("/purgedocuments").session(session).param("candidateid", "C1")
				.param("reason", "Retention period ended"))
				.andExpect(redirectedUrl("/viewcandidate?candidateid=C1"))
				.andExpect(flash().attribute("documentMessage", "3 files deleted permanently."));
	}

	@Test
	void refusedPurgeShowsTheError() throws Exception {
		givenCandidate("C1");
		when(documentservice.purge("C1", null, "U3"))
				.thenReturn(new DocumentOutcome(false, "Please give the reason for deleting the documents permanently."));

		mockMvc.perform(post("/purgedocuments").session(session).param("candidateid", "C1"))
				.andExpect(flash().attribute("documentError",
						"Please give the reason for deleting the documents permanently."));
	}

	@Test
	void purgeForAMissingCandidateIs404AndGetIsRefused() throws Exception {
		mockMvc.perform(post("/purgedocuments").session(session).param("candidateid", "C9").param("reason", "x"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/purgedocuments").session(session).param("candidateid", "C1"))
				.andExpect(status().isMethodNotAllowed());

		verify(documentservice, never()).purge(anyString(), any(), anyString());
	}

	private void givenCandidate(String candidateid) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid(candidateid);
		when(candidateservice.findCandidateById(candidateid)).thenReturn(candidate);
	}

	private MockMultipartHttpServletRequestBuilder upload(String candidateid, String doctype, MockMultipartFile file) {
		MockMultipartHttpServletRequestBuilder request = multipart("/uploaddocument").file(file);
		request.param("candidateid", candidateid).param("doctype", doctype).session(session);
		return request;
	}

	private static DocumentInfo document(int key, String candidateid, String name, String type) {
		DocumentInfo document = new DocumentInfo();
		document.setDocumentkey(key);
		document.setCandidateid(candidateid);
		document.setDoctype("CV");
		document.setOriginalname(name);
		document.setContenttype(type);
		document.setStoredname("a0000000000000000000000000000001");
		return document;
	}
}
