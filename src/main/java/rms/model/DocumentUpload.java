package rms.model;

/**
 * One upload from the candidate profile, as the service checks it. title, issuer and issueyear are for
 * professional certificates; issueyear is the text typed. originalname is the name the browser sent.
 */
public record DocumentUpload(String candidateid, String doctype, String title, String issuer, String issueyear,
		String originalname, String declaredtype, byte[] content) {

	/** No file name or content: they can be personal data. */
	@Override
	public String toString() {
		return "DocumentUpload [candidateid=" + candidateid + ", doctype=" + doctype + ", size="
				+ (content == null ? 0 : content.length) + "]";
	}
}
