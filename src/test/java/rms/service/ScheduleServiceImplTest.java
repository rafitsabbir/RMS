package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.dao.AssignmentDao;
import rms.dao.ScheduleDao;
import rms.model.AssignmentInfo;
import rms.model.ScheduleInfo;

/** The schedule rules: an assigned, active interviewer; a time; no double booking; cancel only a scheduled one. */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

	static final LocalDateTime START = LocalDateTime.of(2099, 3, 4, 10, 30);

	@Mock
	ScheduleDao scheduledao;

	@Mock
	AssignmentDao assignmentdao;

	@Mock
	ActivityService activityservice;

	ScheduleServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ScheduleServiceImpl();
		service.setScheduleDao(scheduledao);
		service.setAssignmentDao(assignmentdao);
		service.setActivityService(activityservice);
	}

	@Test
	void anInterviewIsScheduledForAnAssignedActiveInterviewer() {
		assigned("C1", "U2", true);
		ScheduleInfo info = info(0, "C1", "U2", START);
		// Whatever status the form posts, a new interview starts SCHEDULED
		info.setStatus(ScheduleInfo.DONE);

		Outcome outcome = service.save(info, "U3");

		assertThat(outcome.done()).isTrue();
		assertThat(info.getStatus()).isEqualTo(ScheduleInfo.SCHEDULED);
		verify(scheduledao).addSchedule(info, "U3");
	}

	@Test
	void anInterviewerWhoIsntAssignedIsRefused() {
		assigned("C1", "U2", true);

		Outcome outcome = service.save(info(0, "C1", "U5", START), "U3");

		assertThat(outcome.done()).isFalse();
		assertThat(outcome.message()).contains("assigned");
		verify(scheduledao, never()).addSchedule(any(), anyString());
	}

	@Test
	void anInactiveInterviewerIsRefusedEvenIfAssigned() {
		assigned("C1", "U5", false);

		Outcome outcome = service.save(info(0, "C1", "U5", START), "U3");

		assertThat(outcome.done()).isFalse();
		verify(scheduledao, never()).addSchedule(any(), anyString());
	}

	@Test
	void anInterviewerAssignedToAnotherCandidateIsRefused() {
		// The assignments are read for this candidate only
		when(assignmentdao.getAssignments("C2")).thenReturn(List.of());

		assertThat(service.save(info(0, "C2", "U2", START), "U3").done()).isFalse();
	}

	@Test
	void anInterviewerAndATimeAreRequired() {
		assertThat(service.save(info(0, "C1", " ", START), "U3").message()).contains("interviewer");
		assertThat(service.save(info(0, "C1", null, START), "U3").done()).isFalse();
		assertThat(service.save(info(0, "C1", "U2", null), "U3").message()).contains("date and time");
		verify(scheduledao, never()).addSchedule(any(), anyString());
	}

	@Test
	void theSameInterviewerAtTheSameTimeIsRefused() {
		assigned("C1", "U2", true);
		when(scheduledao.hasConflict("U2", START, 0)).thenReturn(true);

		Outcome outcome = service.save(info(0, "C1", "U2", START), "U3");

		assertThat(outcome.done()).isFalse();
		assertThat(outcome.message()).contains("already has an interview");
		verify(scheduledao, never()).addSchedule(any(), anyString());
	}

	@Test
	void aChangeIsntItsOwnConflict() {
		storedWith("U2");
		when(scheduledao.updateSchedule(any(), anyString())).thenReturn(true);
		ScheduleInfo info = info(5, "C1", "U2", START);
		info.setStatus(ScheduleInfo.SCHEDULED);

		assertThat(service.save(info, "U3").done()).isTrue();

		// The check excludes the interview being changed
		verify(scheduledao).hasConflict("U2", START, 5);
	}

	@Test
	void anInterviewMarkedDoneIsNotCheckedForAConflict() {
		storedWith("U2");
		when(scheduledao.updateSchedule(any(), anyString())).thenReturn(true);
		ScheduleInfo info = info(5, "C1", "U2", START);
		info.setStatus(ScheduleInfo.DONE);

		assertThat(service.save(info, "U3").message()).isEqualTo("Interview updated.");
		verify(scheduledao, never()).hasConflict(anyString(), any(), anyInt());
	}

	@Test
	void anInterviewCanStillBeMarkedDoneAfterItsInterviewerWasUnassigned() {
		// The interviewer is no longer assigned (or active), but a change that keeps them needs no check
		storedWith("U5");
		when(scheduledao.updateSchedule(any(), anyString())).thenReturn(true);
		ScheduleInfo info = info(5, "C1", "U5", START);
		info.setStatus(ScheduleInfo.DONE);

		assertThat(service.save(info, "U3").done()).isTrue();
		verify(assignmentdao, never()).getAssignments(anyString());
	}

	@Test
	void changingTheInterviewerToAnUnassignedOneIsRefused() {
		storedWith("U2");
		assigned("C1", "U2", true);
		ScheduleInfo info = info(5, "C1", "U5", START);
		info.setStatus(ScheduleInfo.SCHEDULED);

		assertThat(service.save(info, "U3").done()).isFalse();
		verify(scheduledao, never()).updateSchedule(any(), anyString());
	}

	@Test
	void aChangeToAMissingInterviewIsRefused() {
		ScheduleInfo info = info(5, "C1", "U2", START);
		info.setStatus(ScheduleInfo.DONE);

		assertThat(service.save(info, "U3").message()).contains("cancelled or removed");
	}

	@Test
	void theStartIsCutToWholeMinutes() {
		assigned("C1", "U2", true);
		ScheduleInfo info = info(0, "C1", "U2", START.plusSeconds(45));

		assertThat(service.save(info, "U3").done()).isTrue();
		assertThat(info.getStartat()).isEqualTo(START);
		verify(scheduledao).hasConflict("U2", START, 0);
	}

	@Test
	void aChangeMayOnlyMakeItScheduledOrDone() {
		ScheduleInfo info = info(5, "C1", "U2", START);
		info.setStatus(ScheduleInfo.CANCELLED);

		// Cancelling has its own action
		assertThat(service.save(info, "U3").done()).isFalse();
		info.setStatus("WHATEVER");
		assertThat(service.save(info, "U3").done()).isFalse();
		verify(scheduledao, never()).updateSchedule(any(), anyString());
	}

	@Test
	void aChangeToACancelledOrRemovedInterviewIsRefused() {
		storedWith("U2");
		when(scheduledao.updateSchedule(any(), anyString())).thenReturn(false);
		ScheduleInfo info = info(5, "C1", "U2", START);

		Outcome outcome = service.save(info, "U3");

		assertThat(outcome.done()).isFalse();
		assertThat(outcome.message()).contains("cancelled or removed");
	}

	@Test
	void theLocationIsTrimmedBlankIsNullAndTooLongIsRefused() {
		assigned("C1", "U2", true);
		ScheduleInfo padded = info(0, "C1", "U2", START);
		padded.setLocation("  Room 1  ");
		ScheduleInfo blank = info(0, "C1", "U2", START);
		blank.setLocation("   ");
		ScheduleInfo tooLong = info(0, "C1", "U2", START);
		tooLong.setLocation("x".repeat(ScheduleInfo.MAX_LOCATION + 1));

		assertThat(service.save(padded, "U3").done()).isTrue();
		assertThat(padded.getLocation()).isEqualTo("Room 1");
		assertThat(service.save(blank, "U3").done()).isTrue();
		assertThat(blank.getLocation()).isNull();
		assertThat(service.save(tooLong, "U3").done()).isFalse();
	}

	@Test
	void cancelWorksOnAScheduledInterviewOnly() {
		when(scheduledao.cancelSchedule(5, "U3")).thenReturn(true);
		when(scheduledao.cancelSchedule(6, "U3")).thenReturn(false);

		assertThat(service.cancel(5, "U3")).isEqualTo(Outcome.ok("Interview cancelled."));
		assertThat(service.cancel(6, "U3").done()).isFalse();
	}

	@Test
	void schedulableInterviewersAreTheActiveAssignedOnes() {
		when(assignmentdao.getAssignments("C1")).thenReturn(List.of(assignment("U2", true), assignment("U5", false)));

		assertThat(service.getSchedulableInterviewers("C1")).extracting(AssignmentInfo::getInterviewerid)
				.containsExactly("U2");
	}

	private void storedWith(String interviewerid) {
		when(scheduledao.findScheduleById(5)).thenReturn(info(5, "C1", interviewerid, START));
	}

	private void assigned(String candidateid, String interviewerid, boolean active) {
		when(assignmentdao.getAssignments(candidateid)).thenReturn(List.of(assignment(interviewerid, active)));
	}

	private static AssignmentInfo assignment(String interviewerid, boolean active) {
		AssignmentInfo assignment = new AssignmentInfo();
		assignment.setInterviewerid(interviewerid);
		assignment.setIntervieweractive(active);
		return assignment;
	}

	private static ScheduleInfo info(int key, String candidateid, String interviewerid, LocalDateTime startat) {
		ScheduleInfo info = new ScheduleInfo();
		info.setSchedulekey(key);
		info.setCandidateid(candidateid);
		info.setInterviewerid(interviewerid);
		info.setStartat(startat);
		return info;
	}
}
