package rms.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rms.model.ImportPreview;
import rms.service.CandidateImportService;
import rms.service.CsvReader;
import rms.service.CsvWriter;
import rms.service.Outcome;

/**
 * Import candidates from a CSV file (F4; Super Admin and HR, SecurityConfig.HR_PAGES): download the template, upload
 * a file to see what it would add (nothing is saved), then import. Between the two steps only the file's cells are
 * kept in the session; the import checks every line again.
 */
@Controller
@RequestMapping(value = "/")
public class ImportController {
	private static final Logger log = LoggerFactory.getLogger(ImportController.class);

	/** The largest file accepted: about 500 candidates with long names, e-mail addresses and phone numbers. */
	static final long MAX_BYTES = 512 * 1024;

	/** The session attribute holding the previewed file's cells until it is imported or replaced. */
	static final String SESSION_KEY = "candidateimport";

	@Autowired
	CandidateImportService importservice;

	@RequestMapping(value = "/importcandidates", method = RequestMethod.GET)
	public ModelAndView importForm(HttpSession session) {
		session.removeAttribute(SESSION_KEY);
		return form(null, null);
	}

	/** The template: the header line only, in the column order the import reads best. */
	@RequestMapping(value = "/candidatetemplate", method = RequestMethod.GET)
	public ResponseEntity<byte[]> template() {
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("candidate-import-template.csv").build().toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(new CsvWriter().row(CandidateImportService.COLUMNS.toArray()).toBytes());
	}

	@RequestMapping(value = "/previewimport", method = RequestMethod.POST)
	public ModelAndView preview(@RequestParam(value = "file", required = false) MultipartFile file,
			HttpSession session) {
		session.removeAttribute(SESSION_KEY);
		if (file == null || file.isEmpty()) {
			return form(null, "Please choose a CSV file.");
		}
		if (file.getSize() > MAX_BYTES) {
			return form(null, "The file is larger than 512 KB. Please split it.");
		}
		List<List<String>> cells;
		try {
			cells = CsvReader.read(file.getBytes());
		} catch (IOException e) {
			log.warn("Candidate import: the upload couldn't be read: {}", e.getClass().getSimpleName());
			return form(null, "The file couldn't be read. Please try again.");
		}
		ImportPreview preview = importservice.preview(cells);
		if (preview.getProblem() == null) {
			// Serializable lists, so the session stays storable
			ArrayList<ArrayList<String>> kept = new ArrayList<ArrayList<String>>();
			for (List<String> row : cells) {
				kept.add(new ArrayList<String>(row));
			}
			session.setAttribute(SESSION_KEY, kept);
		}
		return form(preview, preview.getProblem());
	}

	@RequestMapping(value = "/confirmimport", method = RequestMethod.POST)
	public ModelAndView confirm(@RequestParam(value = "includeduplicates", defaultValue = "false") boolean includeduplicates,
			HttpSession session, RedirectAttributes redirect) {
		@SuppressWarnings("unchecked")
		List<List<String>> cells = (List<List<String>>) (List<?>) session.getAttribute(SESSION_KEY);
		if (cells == null) {
			return form(null, "Please upload the file again: the preview has expired or was imported already.");
		}
		session.removeAttribute(SESSION_KEY);
		Outcome outcome = importservice.importCandidates(cells, includeduplicates, CurrentUser.requireUserid(session));
		if (!outcome.done()) {
			return form(null, outcome.message());
		}
		redirect.addFlashAttribute("message", outcome.message());
		return new ModelAndView("redirect:/viewcandidatelist");
	}

	private static ModelAndView form(ImportPreview preview, String errorMessage) {
		ModelAndView mv = new ModelAndView("importcandidates");
		mv.addObject("columns", CandidateImportService.COLUMNS);
		mv.addObject("maxrows", CandidateImportService.MAX_ROWS);
		if (preview != null && preview.getProblem() == null) {
			mv.addObject("preview", preview);
		}
		if (errorMessage != null) {
			mv.addObject("errorMessage", errorMessage);
		}
		return mv;
	}
}
