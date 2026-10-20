package rms.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import rms.model.ScheduleInfo;

/** The interview's .ics file: RFC 5545 structure, escaping, folding, floating time, updates and cancellation. */
class CalendarInviteTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 8, 0, 0);

	@Test
	void oneEventInFloatingTimeWithoutAttendees() {
		String ics = ics(interview());

		assertThat(ics).startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n").endsWith("END:VEVENT\r\nEND:VCALENDAR\r\n");
		assertThat(ics).contains("METHOD:PUBLISH\r\n", "UID:rms-interview-12\r\n", "DTSTAMP:20261020T080000Z\r\n",
				"DTSTART:20300601T100000\r\n", "DURATION:PT1H\r\n", "SUMMARY:Interview: Nora Newcomer (C3)\r\n",
				"LOCATION:Room 2\r\n", "STATUS:CONFIRMED\r\n");
		// Floating time (no Z, no TZID): RMS times are the server's wall clock; and nothing that sends e-mail
		assertThat(ics).doesNotContain("DTSTART:20300601T100000Z", "TZID", "ATTENDEE", "ORGANIZER");
		assertThat(ics.replace("\r\n", "")).doesNotContain("\n", "\r");
	}

	@Test
	void textIsEscaped() {
		ScheduleInfo schedule = interview();
		schedule.setLocation("Room 2, floor 3; east\\wing");

		assertThat(ics(schedule)).contains("LOCATION:Room 2\\, floor 3\\; east\\\\wing\r\n");
		assertThat(CalendarInvite.text("a\nb\r\nc")).isEqualTo("a\\nb\\nc");
	}

	@Test
	void longLinesAreFoldedAt75Octets() {
		ScheduleInfo schedule = interview();
		schedule.setLocation("Main building, meeting room on the second floor next to the reception desk, ask for HR");

		String ics = ics(schedule);
		assertThat(Arrays.stream(ics.split("\r\n")).mapToInt(l -> l.getBytes(StandardCharsets.UTF_8).length).max()
				.getAsInt()).isLessThanOrEqualTo(75);
		// Unfolding (CRLF plus one space removed) gives the line back
		assertThat(ics.replace("\r\n ", "")).contains("LOCATION:Main building\\, meeting room on the second floor next to "
				+ "the reception desk\\, ask for HR\r\n");
	}

	@Test
	void aChangeRaisesTheSequenceAndACancellationCancelsTheEvent() {
		ScheduleInfo schedule = interview();
		schedule.setChangedat(LocalDateTime.of(2026, 10, 1, 9, 0));
		long before = CalendarInvite.sequence(schedule);
		schedule.setChangedat(LocalDateTime.of(2026, 10, 2, 9, 0));
		schedule.setStatus(ScheduleInfo.CANCELLED);

		assertThat(CalendarInvite.sequence(schedule)).isGreaterThan(before);
		assertThat(ics(schedule)).contains("UID:rms-interview-12\r\n", "STATUS:CANCELLED\r\n",
				"SEQUENCE:" + CalendarInvite.sequence(schedule) + "\r\n");
	}

	@Test
	void withoutNamesTheIdsAreUsedAndNoLocationLineIsWritten() {
		ScheduleInfo schedule = interview();
		schedule.setCandidatename(null);
		schedule.setInterviewername(null);
		schedule.setLocation(" ");

		assertThat(ics(schedule)).contains("SUMMARY:Interview: C3\r\n", "DESCRIPTION:Interviewer: U2\\nCandidate: C3")
				.doesNotContain("LOCATION:");
	}

	@Test
	void theFileNameHoldsIdsAndTheDateOnly() {
		assertThat(CalendarInvite.fileName(interview())).isEqualTo("interview-C3-2030-06-01.ics");
	}

	private static String ics(ScheduleInfo schedule) {
		return new String(CalendarInvite.of(schedule, NOW), StandardCharsets.UTF_8);
	}

	private static ScheduleInfo interview() {
		ScheduleInfo schedule = new ScheduleInfo();
		schedule.setSchedulekey(12);
		schedule.setCandidateid("C3");
		schedule.setCandidatename("Nora Newcomer");
		schedule.setInterviewerid("U2");
		schedule.setInterviewername("Ivan Interviewer");
		schedule.setStartat(LocalDateTime.of(2030, 6, 1, 10, 0));
		schedule.setLocation("Room 2");
		return schedule;
	}
}
