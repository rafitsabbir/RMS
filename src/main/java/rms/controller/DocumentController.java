package rms.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rms.model.DocumentInfo;
import rms.model.DocumentUpload;
import rms.service.AssignmentService;
import rms.service.CandidateService;
import rms.service.Outcome;
import rms.service.DocumentService;

/**
 * Candidate documents (Phase 2 of the roles plan), on the candidate profile. Uploads, deletes and the permanent
 * delete answer with a redirect back to the profile and a message. Who may call what is in SecurityConfig:
 * uploading and deleting for Super Admin and HR, downloading for staff and (Phase 3) for interviewers assigned to the
 * candidate, checked here; the permanent delete for Super Admin.
 * Files are only ever sent through downloadDocument, never from a static URL.
 */
@Controller
@RequestMapping(value = "/")
public class DocumentController {
	private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

	@Autowired
	DocumentService documentservice;

	@Autowired
	CandidateService candidateservice;

	@Autowired
	AssignmentService assignmentservice;

	/**
	 * The candidate ID is in the form's URL as well as its body, so SecurityConfig can send an upload that was too
	 * large for the server back to the right profile (the body isn't read then).
	 */
	@RequestMapping(value = "/uploaddocument", method = RequestMethod.POST)
	public ModelAndView upload(@RequestParam("candidateid") String candidateid,
			@RequestParam(value = "doctype", required = false) String doctype,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "issuer", required = false) String issuer,
			@RequestParam(value = "issueyear", required = false) String issueyear,
			@RequestParam(value = "file", required = false) MultipartFile file, HttpSession session,
			RedirectAttributes redirect) throws IOException {
		String userid = CurrentUser.requireUserid(session);
		requireCandidate(candidateid);

		Outcome outcome;
		if (file == null || file.isEmpty()) {
			outcome = new Outcome(false, "Please choose a file.");
		} else {
			DocumentUpload upload = new DocumentUpload(candidateid, doctype, title, issuer, issueyear,
					file.getOriginalFilename(), file.getContentType(), file.getBytes());
			outcome = documentservice.upload(upload, userid);
		}
		redirect.addFlashAttribute(outcome.done() ? "documentMessage" : "documentError", outcome.message());
		return profile(candidateid, outcome.done() ? null : doctype);
	}

	/** The file, as an attachment with the name it was uploaded under. Staff only (SecurityConfig). */
	@RequestMapping(value = "/downloaddocument/{documentkey}", method = RequestMethod.GET)
	public ResponseEntity<Resource> download(@PathVariable("documentkey") int documentkey, HttpSession session) {
		DocumentInfo document = documentservice.findDocument(documentkey);
		if (document == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		// Interviewers (Phase 3): only documents of candidates they are assigned to
		if (!CurrentUser.isStaff(session)
				&& !assignmentservice.isAssigned(document.getCandidateid(), CurrentUser.userid(session))) {
			log.warn("Document {}: user {} isn't assigned to its candidate; download refused", documentkey,
					CurrentUser.userid(session));
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		if (!documentservice.isStorageConfigured()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Document storage isn't configured");
		}
		Path path = documentservice.findFile(document);
		if (path == null) {
			log.warn("Document {} has no stored file; download by {} refused", documentkey, CurrentUser.userid(session));
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		HttpHeaders headers = new HttpHeaders();
		// attachment: the browser saves the file rather than showing it as a page of RMS
		headers.setContentDisposition(ContentDisposition.attachment()
				.filename(document.getOriginalname(), StandardCharsets.UTF_8).build());
		headers.set("X-Content-Type-Options", "nosniff");
		log.info("Document {} downloaded by {}", documentkey, CurrentUser.userid(session));
		return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType(document.getContenttype()))
				.body(new FileSystemResource(path));
	}

	/** Soft delete; the file stays until a Super Admin's permanent delete. */
	@RequestMapping(value = "/deletedocument/{documentkey}", method = RequestMethod.POST)
	public ModelAndView delete(@PathVariable("documentkey") int documentkey, HttpSession session,
			RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		DocumentInfo document = documentservice.findDocument(documentkey);
		if (document == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		if (documentservice.deleteDocument(documentkey, userid)) {
			redirect.addFlashAttribute("documentMessage", document.getDoctypelabel() + " deleted.");
		} else {
			// Deleted by someone else meanwhile
			redirect.addFlashAttribute("documentError", "The document was already deleted.");
		}
		return profile(document.getCandidateid(), null);
	}

	/**
	 * Super Admin only (SecurityConfig): removes the files of all the candidate's documents from RMS_DOC_DIR,
	 * including replaced and deleted ones, and marks their rows. A reason is required.
	 */
	@RequestMapping(value = "/purgedocuments", method = RequestMethod.POST)
	public ModelAndView purge(@RequestParam("candidateid") String candidateid,
			@RequestParam(value = "reason", required = false) String reason, HttpSession session,
			RedirectAttributes redirect) {
		String userid = CurrentUser.requireUserid(session);
		// A deleted candidate has no profile any more: their documents are purged from the Deleted Candidates page
		boolean deleted = candidateservice.findCandidateById(candidateid) == null && candidateservice.getDeletedCandidates()
				.stream().anyMatch(candidate -> candidateid.equals(candidate.getCandidateid()));
		if (!deleted) {
			requireCandidate(candidateid);
		}
		Outcome outcome = documentservice.purge(candidateid, reason, userid);
		redirect.addFlashAttribute(outcome.done() ? "documentMessage" : "documentError", outcome.message());
		return deleted ? new ModelAndView("redirect:/viewdeletedcandidates") : profile(candidateid, null);
	}

	private void requireCandidate(String candidateid) {
		if (candidateservice.findCandidateById(candidateid) == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	/** Back to the profile; the ID may be any text (G22), so it is encoded (a "+" as %2B, not a space). */
	private static ModelAndView profile(String candidateid, String doctype) {
		String url = "/viewcandidate?candidateid=" + URLEncoder.encode(candidateid, StandardCharsets.UTF_8);
		if (doctype != null) {
			url += "&doctype=" + URLEncoder.encode(doctype, StandardCharsets.UTF_8);
		}
		return new ModelAndView("redirect:" + url);
	}
}
