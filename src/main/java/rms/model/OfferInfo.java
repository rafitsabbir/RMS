package rms.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * An offer made to a candidate (candidate_offer, migration 011; F8). The salary band is shown to Super Admin and HR
 * only and never logged; keep it out of toString().
 */
public class OfferInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int MAX_SALARYBAND = 100;
	public static final int MAX_NOTE = 500;

	private int offerkey = 0;
	private String candidateid = null;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate offerdate = null;
	private String salaryband = null;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate startdate = null;
	private String status = OfferStatus.PENDING.name();
	/** When the answer (or the withdrawal) was recorded: set by RMS when the status leaves Pending. */
	private LocalDate respondeddate = null;
	private String note = null;
	private String createdby = null;
	private String createdbyname = null;
	private LocalDateTime createdat = null;

	/** Keys, dates and the status only. */
	@Override
	public String toString() {
		return "OfferInfo [offerkey=" + offerkey + ", candidateid=" + candidateid + ", offerdate=" + offerdate
				+ ", status=" + status + "]";
	}

	public String getStatuslabel() {
		OfferStatus parsed = OfferStatus.parse(status);
		return parsed == null ? "" : parsed.getLabel();
	}

	public boolean isPending() {
		return OfferStatus.parse(status) == OfferStatus.PENDING;
	}

	public int getOfferkey() {
		return offerkey;
	}

	public void setOfferkey(int offerkey) {
		this.offerkey = offerkey;
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public LocalDate getOfferdate() {
		return offerdate;
	}

	public void setOfferdate(LocalDate offerdate) {
		this.offerdate = offerdate;
	}

	public String getSalaryband() {
		return salaryband;
	}

	public void setSalaryband(String salaryband) {
		this.salaryband = salaryband;
	}

	public LocalDate getStartdate() {
		return startdate;
	}

	public void setStartdate(LocalDate startdate) {
		this.startdate = startdate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDate getRespondeddate() {
		return respondeddate;
	}

	public void setRespondeddate(LocalDate respondeddate) {
		this.respondeddate = respondeddate;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getCreatedby() {
		return createdby;
	}

	public void setCreatedby(String createdby) {
		this.createdby = createdby;
	}

	public String getCreatedbyname() {
		return createdbyname;
	}

	public void setCreatedbyname(String createdbyname) {
		this.createdbyname = createdbyname;
	}

	public LocalDateTime getCreatedat() {
		return createdat;
	}

	public void setCreatedat(LocalDateTime createdat) {
		this.createdat = createdat;
	}
}
