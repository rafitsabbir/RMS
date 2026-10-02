package rms.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/** A row of the job table: an opening built on a position. positionname is read through a join for display. */
public class JobInfo {

	public static final String OPEN = "OPEN";
	public static final String CLOSED = "CLOSED";

	private int jobkey = 0;
	private int positionkey = 0;
	private String positionname = null;
	private int vacancies = 1;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate closingdate = null;
	private String status = OPEN;

	@Override
	public String toString() {
		return "JobInfo [jobkey=" + jobkey + ", positionkey=" + positionkey + ", vacancies=" + vacancies
				+ ", closingdate=" + closingdate + ", status=" + status + "]";
	}

	/**
	 * For the candidate form's job list, e.g. "Job 1: SOFTWARE ENGINEER (closes 2030-12-31)" or
	 * "Job 2: QA ENGINEER (closed)"; "Job 3 (deleted)" without a status (a deleted job a candidate still has).
	 */
	public String getLabel() {
		if (status == null) {
			return "Job " + jobkey + " (deleted)";
		}
		String name = positionname == null ? "Position " + positionkey : positionname;
		if (CLOSED.equals(status)) {
			return "Job " + jobkey + ": " + name + " (closed)";
		}
		return "Job " + jobkey + ": " + name + (closingdate == null ? "" : " (closes " + closingdate + ")");
	}

	public boolean isOpen() {
		return OPEN.equals(status);
	}

	public int getJobkey() {
		return jobkey;
	}

	public void setJobkey(int jobkey) {
		this.jobkey = jobkey;
	}

	public int getPositionkey() {
		return positionkey;
	}

	public void setPositionkey(int positionkey) {
		this.positionkey = positionkey;
	}

	public String getPositionname() {
		return positionname;
	}

	public void setPositionname(String positionname) {
		this.positionname = positionname;
	}

	public int getVacancies() {
		return vacancies;
	}

	public void setVacancies(int vacancies) {
		this.vacancies = vacancies;
	}

	public LocalDate getClosingdate() {
		return closingdate;
	}

	public void setClosingdate(LocalDate closingdate) {
		this.closingdate = closingdate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
