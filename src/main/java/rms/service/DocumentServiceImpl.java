package rms.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Year;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import rms.dao.DocumentDao;
import rms.dao.DocumentFileStore;
import rms.model.DocumentInfo;
import rms.model.DocumentType;
import rms.model.DocumentUpload;

/**
 * Candidate documents: the checks, the file in RMS_DOC_DIR and the candidate_document row. The logs carry only
 * document keys, candidate keys, user keys and counts: no file names, titles or reasons (personal data).
 */
@Service
public class DocumentServiceImpl implements DocumentService {
	private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

	static final String NOT_CONFIGURED = "Document storage isn't configured, so documents can't be uploaded or "
			+ "opened. Please tell IT.";
	static final int MAX_TEXT = 100;
	static final int MAX_REASON = 255;
	static final int FIRST_YEAR = 1950;

	DocumentDao documentdao;
	DocumentFileStore filestore;

	@Autowired
	public void setDocumentDao(DocumentDao documentdao) {
		this.documentdao = documentdao;
	}

	@Autowired
	public void setDocumentFileStore(DocumentFileStore filestore) {
		this.filestore = filestore;
	}

	@Override
	public boolean isStorageConfigured() {
		return filestore.isConfigured();
	}

	@Override
	public List<DocumentInfo> getDocuments(String candidateid) {
		return documentdao.getDocuments(candidateid);
	}

	@Override
	public DocumentInfo findDocument(int documentkey) {
		return documentdao.findDocument(documentkey);
	}

	@Override
	public Path findFile(DocumentInfo document) {
		return filestore.find(document.getStoredname());
	}

	@Override
	public Outcome upload(DocumentUpload upload, String userid) {
		if (!filestore.isConfigured()) {
			return Outcome.refused(NOT_CONFIGURED);
		}
		DocumentType type = DocumentType.parse(upload.doctype());
		if (type == null) {
			return Outcome.refused("Please choose the kind of document.");
		}

		DocumentInfo document = new DocumentInfo();
		document.setCandidateid(upload.candidateid());
		document.setDoctype(type.name());
		document.setUploadedby(userid);
		if (type == DocumentType.PROFESSIONAL) {
			String problem = professionalDetails(upload, document);
			if (problem != null) {
				return Outcome.refused(problem);
			}
		}

		String name = DocumentRules.displayName(upload.originalname());
		String problem = DocumentRules.problem(name, upload.declaredtype(), upload.content());
		if (problem != null) {
			return Outcome.refused(problem);
		}
		document.setOriginalname(name);
		// The kind the checks agreed on, not the browser's text
		document.setContenttype(DocumentRules.kindOf(name).getContenttype());
		document.setFilesize(upload.content().length);

		// Counted before the file is written, and again with the insert (DocumentDaoImpl.addDocument)
		int active = documentdao.countDocuments(upload.candidateid(), type.name());
		if (!type.isSingleSlot() && active >= type.getMaxActive()) {
			return Outcome.refused(tooMany(type));
		}

		try {
			document.setStoredname(filestore.store(upload.content()));
		} catch (IOException e) {
			log.error("A document for candidate {} couldn't be written to {} (by {})", upload.candidateid(),
					DocumentFileStore.ENVIRONMENT_VARIABLE, userid, e);
			return Outcome.refused("The file couldn't be saved. Please try again, or tell IT.");
		}

		int documentkey;
		try {
			documentkey = documentdao.addDocument(document, type.getMaxActive());
		} catch (RuntimeException e) {
			removeQuietly(document.getStoredname());
			throw e;
		}
		if (documentkey == 0) {
			removeQuietly(document.getStoredname());
			return Outcome.refused(tooMany(type));
		}

		log.info("Document {} uploaded by {}", documentkey, userid);
		if (type.isSingleSlot() && active > 0) {
			return Outcome.ok(type.getLabel() + " replaced. The previous file is kept until a Super Admin "
					+ "deletes the candidate's documents permanently.");
		}
		return Outcome.ok(type.getLabel() + " uploaded.");
	}

	/** Checks a professional certificate's title, issuer and year into the document; the problem, or null. */
	private static String professionalDetails(DocumentUpload upload, DocumentInfo document) {
		String title = trim(upload.title());
		String issuer = trim(upload.issuer());
		String year = trim(upload.issueyear());
		if (title.isEmpty()) {
			return "Please enter the certificate's title, for example PMP.";
		}
		if (title.length() > MAX_TEXT || issuer.length() > MAX_TEXT) {
			return "The title and the issuer can have at most " + MAX_TEXT + " characters.";
		}
		Integer issueyear = null;
		if (!year.isEmpty()) {
			int thisYear = Year.now().getValue();
			issueyear = year.matches("[0-9]{4}") ? Integer.valueOf(year) : null;
			if (issueyear == null || issueyear < FIRST_YEAR || issueyear > thisYear) {
				return "The year must be between " + FIRST_YEAR + " and " + thisYear + ", or empty.";
			}
		}
		document.setTitle(title);
		document.setIssuer(issuer.isEmpty() ? null : issuer);
		document.setIssueyear(issueyear);
		return null;
	}

	private static String tooMany(DocumentType type) {
		return "This candidate already has " + type.getMaxActive() + " " + type.getLabel().toLowerCase(Locale.ROOT)
				+ "s. Delete one before adding another.";
	}

	@Override
	public boolean deleteDocument(int documentkey, String userid) {
		boolean deleted = documentdao.deleteDocument(documentkey, userid);
		if (deleted) {
			log.info("Document {} deleted by {}", documentkey, userid);
		}
		return deleted;
	}

	@Override
	public Outcome purge(String candidateid, String reason, String userid) {
		String why = trim(reason);
		if (why.isEmpty()) {
			return Outcome.refused("Please give the reason for deleting the documents permanently.");
		}
		if (why.length() > MAX_REASON) {
			return Outcome.refused("The reason can have at most " + MAX_REASON + " characters.");
		}
		if (!filestore.isConfigured()) {
			return Outcome.refused(NOT_CONFIGURED);
		}

		int purged = 0;
		int failed = 0;
		for (DocumentInfo document : documentdao.getUnpurgedDocuments(candidateid)) {
			try {
				filestore.delete(document.getStoredname());
			} catch (IOException e) {
				// The row stays as it was, so a later purge tries the file again
				failed++;
				continue;
			}
			try {
				documentdao.markPurged(document.getDocumentkey(), userid, why);
			} catch (DataAccessException e) {
				// The file is gone but the row isn't marked; a later purge marks it (the file delete is then a no-op)
				log.error("Document {}: file removed, but the row couldn't be marked", document.getDocumentkey(), e);
				failed++;
				continue;
			}
			purged++;
		}

		log.info("Documents of candidate {} deleted permanently by {}: {}", candidateid, userid, purged);
		if (failed > 0) {
			log.error("Documents of candidate {}: {} files couldn't be removed from {}", candidateid, failed,
					DocumentFileStore.ENVIRONMENT_VARIABLE);
			return Outcome.refused(purged + " " + files(purged) + " deleted permanently, but " + failed + " "
					+ files(failed) + " couldn't be removed and " + (failed == 1 ? "is" : "are")
					+ " kept. Please tell IT.");
		}
		if (purged == 0) {
			return Outcome.ok("There were no stored files to delete.");
		}
		return Outcome.ok(purged + " " + files(purged) + " deleted permanently.");
	}

	private static String files(int count) {
		return count == 1 ? "file" : "files";
	}

	/** Removes a file whose row wasn't saved; a failure only leaves an orphan file, so it is logged, not raised. */
	private void removeQuietly(String storedname) {
		try {
			filestore.delete(storedname);
		} catch (IOException e) {
			log.warn("An unused document file couldn't be removed from {}", DocumentFileStore.ENVIRONMENT_VARIABLE);
		}
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
