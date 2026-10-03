package rms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** ScheduleInfo and DashboardInfo helpers used by the JSPs. */
class ScheduleModelTest {

	@Test
	void theStartIsShownToTheMinuteAndEmptyWithoutOne() {
		ScheduleInfo info = new ScheduleInfo();
		assertThat(info.getStartlabel()).isEmpty();

		info.setStartat(LocalDateTime.of(2026, 10, 5, 9, 5, 30));

		assertThat(info.getStartlabel()).isEqualTo("2026-10-05 09:05");
	}

	@Test
	void theStatusFlagsFollowTheStatus() {
		ScheduleInfo info = new ScheduleInfo();
		// A new interview is SCHEDULED
		assertThat(info.isScheduled()).isTrue();
		info.setStatus(ScheduleInfo.DONE);
		assertThat(info.isDone()).isTrue();
		assertThat(info.isScheduled()).isFalse();
		info.setStatus(ScheduleInfo.CANCELLED);
		assertThat(info.isCancelled()).isTrue();
		info.setStatus(null);
		assertThat(info.isScheduled() || info.isDone() || info.isCancelled()).isFalse();
	}

	@Test
	void toStringHasKeysOnly() {
		ScheduleInfo info = new ScheduleInfo();
		info.setSchedulekey(4);
		info.setCandidateid("C1");
		info.setInterviewerid("U2");
		info.setCandidatename("Carla Candidate");
		info.setInterviewername("Ivan Interviewer");
		info.setLocation("Room 1");

		assertThat(info.toString()).contains("schedulekey=4", "candidateid=C1", "interviewerid=U2")
				.doesNotContain("Carla", "Ivan", "Room 1");
	}

	@Test
	void theTotalIsTheSumOfTheStatuses() {
		DashboardInfo dashboard = new DashboardInfo();
		dashboard.setSelected(1);
		dashboard.setRejected(2);
		dashboard.setOnhold(3);
		dashboard.setPending(4);

		assertThat(dashboard.getTotalcandidates()).isEqualTo(10);
	}
}
