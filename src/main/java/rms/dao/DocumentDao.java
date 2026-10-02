package rms.dao;

import java.util.List;

import rms.model.DocumentInfo;

/** candidate_document rows. Rows are never deleted: a delete or a replace sets isactive=0, a purge purgedat. */
public interface DocumentDao {

	/** A candidate's active documents, oldest first. */
	public List<DocumentInfo> getDocuments(String candidateid);

	/** An active document of an active candidate, or null. */
	public DocumentInfo findDocument(int documentkey);

	/** How many active documents of the type the candidate has. */
	public int countDocuments(String candidateid, String doctype);

	/**
	 * Saves a new document (uploadedat is set here) and returns its key, or 0 when the candidate already has
	 * maxActive active documents of the type. With maxActive 1 the active one is replaced instead: it is
	 * soft-deleted in the same transaction.
	 */
	public int addDocument(DocumentInfo documentinfo, int maxActive);

	/** Soft delete; the file stays. False when the document isn't active. */
	public boolean deleteDocument(int documentkey, String userid);

	/** Every document of the candidate whose file hasn't been purged: active, replaced and deleted ones. */
	public List<DocumentInfo> getUnpurgedDocuments(String candidateid);

	/** Records that the document's file was removed: inactive, with who, when and why. */
	public void markPurged(int documentkey, String userid, String reason);
}
