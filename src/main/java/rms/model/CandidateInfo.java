package rms.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * A row of the candidate table. positionname, languagename and the job's status are read through joins for
 * display. candidatestatus and the decision fields are written only by a decision (DecisionDaoImpl, Phase 3). The
 * document counts are read through subqueries on candidate_document (active documents only).
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
	private String email = null;
	private String phone = null;
	private String source = null;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate applieddate = null;
	/** 0: not linked to a job. */
	private int jobkey = 0;
	/** The linked job's status (OPEN or CLOSED), or null when it is deleted or missing. */
	private String jobstatus = null;
	private int cvcount = 0;
	/** How many of the six single-slot types (CV included) have an active document. */
	private int slotcount = 0;
	private int professionalcount = 0;
	/** The latest decision (Phase 3): reason, date and who made it; the status is candidatestatus. */
	private String decisionreason = null;
	private LocalDate decisiondate = null;
	private String decidedby = null;
	private String decidedbyname = null;
	/** Only on the deleted candidates list: documents whose files are still stored. */
	private int unpurgedcount = 0;

	/** No names, e-mail or phone: they are personal data. */
	@Override
	public String toString() {
		return "CandidateInfo [candidateid=" + candidateid + ", positionkey=" + positionkey
				+ ", languagekey=" + languagekey + ", jobkey=" + jobkey + ", candidatestatus=" + candidatestatus + "]";
	}

	/** The source's label, or the stored text for a source this version doesn't know; null when not recorded. */
	public String getSourcelabel() {
		CandidateSource parsed = CandidateSource.parse(source);
		return parsed == null ? source : parsed.getLabel();
	}

	/** Selected, Rejected, On hold or Pending (rms.model.DecisionStatus). */
	public String getStatuslabel() {
		return DecisionStatus.labelOf(candidatestatus);
	}

	/** Selected or Rejected: the candidate's evaluations can't be changed. */
	public boolean isLocked() {
		return DecisionStatus.locks(candidatestatus);
	}

	/** False only for a source text this version doesn't know (the form offers it so saving keeps it). */
	public boolean isKnownsource() {
		return source == null || source.isEmpty() || CandidateSource.parse(source) != null;
	}

	public boolean isHascv() {
		return cvcount > 0;
	}

	public int getSlottypes() {
		return DocumentType.SLOT_TYPES;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public LocalDate getApplieddate() {
		return applieddate;
	}

	public void setApplieddate(LocalDate applieddate) {
		this.applieddate = applieddate;
	}

	public int getJobkey() {
		return jobkey;
	}

	public void setJobkey(int jobkey) {
		this.jobkey = jobkey;
	}

	public String getJobstatus() {
		return jobstatus;
	}

	public void setJobstatus(String jobstatus) {
		this.jobstatus = jobstatus;
	}

	public int getUnpurgedcount() {
		return unpurgedcount;
	}

	public void setUnpurgedcount(int unpurgedcount) {
		this.unpurgedcount = unpurgedcount;
	}

	public int getCvcount() {
		return cvcount;
	}

	public void setCvcount(int cvcount) {
		this.cvcount = cvcount;
	}

	public int getSlotcount() {
		return slotcount;
	}

	public void setSlotcount(int slotcount) {
		this.slotcount = slotcount;
	}

	public int getProfessionalcount() {
		return professionalcount;
	}

	public void setProfessionalcount(int professionalcount) {
		this.professionalcount = professionalcount;
	}

	public String getDecisionreason() {
		return decisionreason;
	}

	public void setDecisionreason(String decisionreason) {
		this.decisionreason = decisionreason;
	}

	public LocalDate getDecisiondate() {
		return decisiondate;
	}

	public void setDecisiondate(LocalDate decisiondate) {
		this.decisiondate = decisiondate;
	}

	public String getDecidedby() {
		return decidedby;
	}

	public void setDecidedby(String decidedby) {
		this.decidedby = decidedby;
	}

	public String getDecidedbyname() {
		return decidedbyname;
	}

	public void setDecidedbyname(String decidedbyname) {
		this.decidedbyname = decidedbyname;
	}

}
