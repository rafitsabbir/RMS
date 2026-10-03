package rms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * A row of interview_schedule (Phase 4): one interview of a candidate by an assigned interviewer. The names are read
 * through subqueries for display. startat has no time zone: it is the wall-clock time in the MySQL server's time
 * zone, which is what NOW() uses (db/migrations/006-interview-schedule.sql).
 */
public class ScheduleInfo {

	public static final String SCHEDULED = "SCHEDULED";
	public static final String DONE = "DONE";
	public static final String CANCELLED = "CANCELLED";

	public static final int MAX_LOCATION = 200;

	private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private int schedulekey = 0;
	private String candidateid = null;
	private String candidatename = null;
	private String interviewerid = null;
	private String interviewername = null;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime startat = null;
	private String location = null;
	private String status = SCHEDULED;

	/** Keys and the status only: no names, no location. */
	@Override
	public String toString() {
		return "ScheduleInfo [schedulekey=" + schedulekey + ", candidateid=" + candidateid + ", interviewerid="
				+ interviewerid + ", startat=" + startat + ", status=" + status + "]";
	}

	public boolean isScheduled() {
		return SCHEDULED.equals(status);
	}

	public boolean isDone() {
		return DONE.equals(status);
	}

	public boolean isCancelled() {
		return CANCELLED.equals(status);
	}

	/** The date and time as shown on the pages, e.g. "2026-10-05 14:30"; empty without one. */
	public String getStartlabel() {
		return startat == null ? "" : startat.format(LABEL);
	}

	public int getSchedulekey() {
		return schedulekey;
	}

	public void setSchedulekey(int schedulekey) {
		this.schedulekey = schedulekey;
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public String getCandidatename() {
		return candidatename;
	}

	public void setCandidatename(String candidatename) {
		this.candidatename = candidatename;
	}

	public String getInterviewerid() {
		return interviewerid;
	}

	public void setInterviewerid(String interviewerid) {
		this.interviewerid = interviewerid;
	}

	public String getInterviewername() {
		return interviewername;
	}

	public void setInterviewername(String interviewername) {
		this.interviewername = interviewername;
	}

	public LocalDateTime getStartat() {
		return startat;
	}

	public void setStartat(LocalDateTime startat) {
		this.startat = startat;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
