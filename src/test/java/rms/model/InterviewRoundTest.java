package rms.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** F2: the rounds, their codes and labels; NULL and unknown texts are "no round". */
class InterviewRoundTest {

	@Test
	void codesAndLabels() {
		assertThat(InterviewRound.values()).extracting(InterviewRound::getName, InterviewRound::getLabel)
				.containsExactly(org.assertj.core.api.Assertions.tuple("FIRST", "First interview"),
						org.assertj.core.api.Assertions.tuple("TECHNICAL", "Technical interview"),
						org.assertj.core.api.Assertions.tuple("FINAL", "Final interview"));
		assertThat(InterviewRound.parse(" technical ")).isEqualTo(InterviewRound.TECHNICAL);
		assertThat(InterviewRound.code("final")).isEqualTo("FINAL");
	}

	@Test
	void nullBlankAndUnknownAreNoRound() {
		for (String text : new String[] {null, "", "  ", "LUNCH"}) {
			assertThat(InterviewRound.parse(text)).isNull();
			assertThat(InterviewRound.code(text)).isNull();
			assertThat(InterviewRound.label(text)).isEqualTo("No round");
		}
		ScheduleInfo schedule = new ScheduleInfo();
		assertThat(schedule.getRoundlabel()).isEqualTo("No round");
		MarksInfo marks = new MarksInfo();
		marks.setInterviewround("FIRST");
		assertThat(marks.getRoundlabel()).isEqualTo("First interview");
	}
}
