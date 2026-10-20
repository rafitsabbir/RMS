package rms.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import rms.model.ScheduleInfo;

/**
 * An interview as an iCalendar file (RFC 5545) that Outlook, Google Calendar and Apple Calendar import: one VEVENT,
 * no attendees and no e-mail, so importing it sends nothing. startat has no time zone in RMS (the MySQL server's
 * local time), so the event uses floating time: the calendar shows the same wall-clock time, which is right while
 * the users work in the server's zone (open question #30). RMS stores no end time; the event lasts one hour.
 * The UID stays the same for an interview, and SEQUENCE grows with each change, so importing the file again updates
 * the event; a cancelled interview's file marks the event cancelled.
 */
public final class CalendarInvite {

	static final String DURATION = "PT1H";

	private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
	private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

	private CalendarInvite() {
	}

	/** The .ics file of the interview, UTF-8 with CRLF line ends; now is the time stamp of the file (DTSTAMP). */
	public static byte[] of(ScheduleInfo schedule, LocalDateTime nowUtc) {
		String candidate = schedule.getCandidatename() == null ? schedule.getCandidateid()
				: schedule.getCandidatename() + " (" + schedule.getCandidateid() + ")";
		String interviewer = schedule.getInterviewername() == null ? schedule.getInterviewerid()
				: schedule.getInterviewername();
		StringBuilder out = new StringBuilder();
		line(out, "BEGIN:VCALENDAR");
		line(out, "VERSION:2.0");
		line(out, "PRODID:-//RMS//Recruitment Management System//EN");
		line(out, "CALSCALE:GREGORIAN");
		line(out, "METHOD:PUBLISH");
		line(out, "BEGIN:VEVENT");
		line(out, "UID:rms-interview-" + schedule.getSchedulekey());
		line(out, "SEQUENCE:" + sequence(schedule));
		line(out, "DTSTAMP:" + nowUtc.format(UTC));
		line(out, "DTSTART:" + schedule.getStartat().format(LOCAL));
		line(out, "DURATION:" + DURATION);
		line(out, "SUMMARY:" + text("Interview: " + candidate));
		if (schedule.getLocation() != null && !schedule.getLocation().isBlank()) {
			line(out, "LOCATION:" + text(schedule.getLocation()));
		}
		line(out, "DESCRIPTION:" + text("Interviewer: " + interviewer + "\nCandidate: " + candidate
				+ "\nFrom RMS (Recruitment Management System). The time is the RMS server's local time."));
		line(out, "STATUS:" + (schedule.isCancelled() ? "CANCELLED" : "CONFIRMED"));
		line(out, "END:VEVENT");
		line(out, "END:VCALENDAR");
		return out.toString().getBytes(StandardCharsets.UTF_8);
	}

	/** The file name, e.g. interview-C3-2030-06-01.ics (IDs and the date only). */
	public static String fileName(ScheduleInfo schedule) {
		return "interview-" + schedule.getCandidateid().replaceAll("[^A-Za-z0-9]", "") + "-"
				+ schedule.getStartat().toLocalDate() + ".ics";
	}

	/** Seconds since 1970 of the last change (UTC wall clock is fine: it only has to grow). */
	static long sequence(ScheduleInfo schedule) {
		return schedule.getChangedat() == null ? 0 : schedule.getChangedat().toEpochSecond(ZoneOffset.UTC);
	}

	/** RFC 5545 TEXT: backslash, semicolon, comma and line breaks escaped. */
	static String text(String value) {
		return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n")
				.replace("\n", "\\n").replace("\r", "\\n");
	}

	/** One content line, folded at 75 octets (a continuation line starts with a space), ended with CRLF. */
	static void line(StringBuilder out, String content) {
		int octets = 0;
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);
			int size = String.valueOf(c).getBytes(StandardCharsets.UTF_8).length;
			if (Character.isHighSurrogate(c) && i + 1 < content.length()) {
				size = content.substring(i, i + 2).getBytes(StandardCharsets.UTF_8).length;
			}
			if (octets + size > 75) {
				out.append("\r\n ");
				octets = 1;
			}
			out.append(c);
			if (Character.isHighSurrogate(c) && i + 1 < content.length()) {
				out.append(content.charAt(++i));
			}
			octets += size;
		}
		out.append("\r\n");
	}
}
