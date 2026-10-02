package rms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * A row of candidate_document. originalname is only shown and sent as the download's file name; the file is
 * stored under storedname. uploadedbyname is read through a subquery for display.
 */
public class DocumentInfo {

	private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private int documentkey = 0;
	private String candidateid = null;
	private String doctype = null;
	private String originalname = null;
	private String storedname = null;
	private String contenttype = null;
	private long filesize = 0;
	private String title = null;
	private String issuer = null;
	private Integer issueyear = null;
	private String uploadedby = null;
	private String uploadedbyname = null;
	private LocalDateTime uploadedat = null;

	/** No file names: they can be personal data. */
	@Override
	public String toString() {
		return "DocumentInfo [documentkey=" + documentkey + ", candidateid=" + candidateid + ", doctype=" + doctype
				+ ", contenttype=" + contenttype + ", filesize=" + filesize + "]";
	}

	/** The type's label, or the stored text for a type this version doesn't know. */
	public String getDoctypelabel() {
		DocumentType type = DocumentType.parse(doctype);
		return type == null ? doctype : type.getLabel();
	}

	/** The size for display: "512 bytes", "2.0 KB", "1.5 MB". */
	public String getSizelabel() {
		if (filesize < 1024) {
			return filesize + " bytes";
		}
		if (filesize < 1024 * 1024) {
			return String.format(Locale.ROOT, "%.1f KB", filesize / 1024.0);
		}
		return String.format(Locale.ROOT, "%.1f MB", filesize / (1024.0 * 1024.0));
	}

	/** The upload time for display, e.g. "2026-09-03 10:00". */
	public String getUploadedlabel() {
		return uploadedat == null ? "" : uploadedat.format(LABEL);
	}

	public int getDocumentkey() {
		return documentkey;
	}

	public void setDocumentkey(int documentkey) {
		this.documentkey = documentkey;
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getDoctype() {
		return doctype;
	}

	public void setDoctype(String doctype) {
		this.doctype = doctype;
	}

	public String getOriginalname() {
		return originalname;
	}

	public void setOriginalname(String originalname) {
		this.originalname = originalname;
	}

	public String getStoredname() {
		return storedname;
	}

	public void setStoredname(String storedname) {
		this.storedname = storedname;
	}

	public String getContenttype() {
		return contenttype;
	}

	public void setContenttype(String contenttype) {
		this.contenttype = contenttype;
	}

	public long getFilesize() {
		return filesize;
	}

	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public Integer getIssueyear() {
		return issueyear;
	}

	public void setIssueyear(Integer issueyear) {
		this.issueyear = issueyear;
	}

	public String getUploadedby() {
		return uploadedby;
	}

	public void setUploadedby(String uploadedby) {
		this.uploadedby = uploadedby;
	}

	public String getUploadedbyname() {
		return uploadedbyname;
	}

	public void setUploadedbyname(String uploadedbyname) {
		this.uploadedbyname = uploadedbyname;
	}

	public LocalDateTime getUploadedat() {
		return uploadedat;
	}

	public void setUploadedat(LocalDateTime uploadedat) {
		this.uploadedat = uploadedat;
	}

}
