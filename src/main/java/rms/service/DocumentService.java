package rms.service;

import java.nio.file.Path;
import java.util.List;

import rms.model.DocumentInfo;
import rms.model.DocumentUpload;

public interface DocumentService {

	/** Whether RMS_DOC_DIR is a usable folder; uploads and downloads need it. */
	public boolean isStorageConfigured();

	/** A candidate's active documents, oldest first. */
	public List<DocumentInfo> getDocuments(String candidateid);

	/** An active document of an active candidate, or null. */
	public DocumentInfo findDocument(int documentkey);

	/** The document's stored file, or null when it is missing. Needs isStorageConfigured. */
	public Path findFile(DocumentInfo document);

	/** Checks and saves an upload for an active candidate (the caller checks the candidate). */
	public Outcome upload(DocumentUpload upload, String userid);

	/** Soft delete; the file stays. False when the document isn't active. */
	public boolean deleteDocument(int documentkey, String userid);

	/**
	 * Super Admin's permanent delete: removes the files of all the candidate's documents (active, replaced and
	 * deleted) from RMS_DOC_DIR and marks their rows. A reason is required.
	 */
	public Outcome purge(String candidateid, String reason, String userid);
}
