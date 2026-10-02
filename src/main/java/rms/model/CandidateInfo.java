package rms.model;

/**
 * A row of the candidate table. positionname and languagename are read through joins for display;
 * candidatestatus is only read (G9).
 */
public class CandidateInfo {

	private String candidateid = null;
	private String firstname = null;
	private String lastname = null;
	private int positionkey = 0;
	private int languagekey = 0;
	private String positionname = null;
	private String languagename = null;
	private String candidatestatus = null;

	@Override
	public String toString() {
		return "CandidateInfo [candidateid=" + candidateid + ", positionkey=" + positionkey
				+ ", languagekey=" + languagekey + ", candidatestatus=" + candidatestatus + "]";
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public int getPositionkey() {
		return positionkey;
	}

	public void setPositionkey(int positionkey) {
		this.positionkey = positionkey;
	}

	public int getLanguagekey() {
		return languagekey;
	}

	public void setLanguagekey(int languagekey) {
		this.languagekey = languagekey;
	}

	public String getPositionname() {
		return positionname;
	}

	public void setPositionname(String positionname) {
		this.positionname = positionname;
	}

	public String getLanguagename() {
		return languagename;
	}

	public void setLanguagename(String languagename) {
		this.languagename = languagename;
	}

	public String getCandidatestatus() {
		return candidatestatus;
	}

	public void setCandidatestatus(String candidatestatus) {
		this.candidatestatus = candidatestatus;
	}

}
