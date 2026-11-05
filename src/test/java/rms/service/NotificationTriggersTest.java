package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.dao.AssignmentDao;
import rms.dao.MarksDao;
import rms.dao.ScheduleDao;
import rms.model.AssignmentInfo;
import rms.model.MarksInfo;
import rms.model.ScheduleInfo;
import rms.model.UserInfo;

/** F1: which actions send a staff e-mail; a refused action sends none. */
@ExtendWith(MockitoExtension.class)
class NotificationTriggersTest {

	static final LocalDateTime START = LocalDateTime.of(2099, 3, 4, 10, 30);

	@Mock
	ScheduleDao scheduledao;

	@Mock
	AssignmentDao assignmentdao;

	@Mock
	MarksDao marksdao;

	@Mock
	ActivityService activityservice;

	@Mock
	StageService stageservice;

	@Mock
	NotificationService notifications;

	ScheduleServiceImpl schedule;

	AssignmentServiceImpl assignment;

	MarksServiceImpl marks;

	@BeforeEach
	void setUp() {
		schedule = new ScheduleServiceImpl();
		schedule.setScheduleDao(scheduledao);
		schedule.setAssignmentDao(assignmentdao);
		schedule.setActivityService(activityservice);
		schedule.setStageService(stageservice);
		schedule.setNotificationService(notifications);
		assignment = new AssignmentServiceImpl();
		assignment.setAssignmentDao(assignmentdao);
		assignment.setActivityService(activityservice);
		assignment.setNotificationService(notifications);
		marks = new MarksServiceImpl();
		marks.setMarksDao(marksdao);
		marks.setActivityService(activityservice);
		marks.setNotificationService(notifications);
	}

	@Test
	void servicesSendNothingUntilSpringWiresTheRealService() {
		assertThat(new ScheduleServiceImpl().notificationservice).isSameAs(NotificationService.NONE);
		assertThat(new AssignmentServiceImpl().notificationservice).isSameAs(NotificationService.NONE);
		assertThat(new MarksServiceImpl().notificationservice).isSameAs(NotificationService.NONE);
	}

	@Test
	void aNewInterviewNotifiesWithItsKey() {
		assigned("C1", "U2");
		when(scheduledao.addSchedule(any(ScheduleInfo.class), anyString())).thenReturn(41);

		assertThat(schedule.save(interview(0, "U2"), "U3").done()).isTrue();

		verify(notifications).interviewScheduled(41);
	}

	@Test
	void aChangedInterviewNotifiesTheNewAndThePreviousInterviewer() {
		assigned("C1", "U5");
		when(scheduledao.findScheduleById(7)).thenReturn(interview(7, "U2"));
		when(scheduledao.updateSchedule(any(ScheduleInfo.class), anyString())).thenReturn(true);

		assertThat(schedule.save(interview(7, "U5"), "U3").done()).isTrue();

		verify(notifications).interviewChanged(7, "U2");
	}

	@Test
	void markingAnInterviewDoneSendsNothingButANewPlaceDoes() {
		ScheduleInfo stored = interview(7, "U2");
		stored.setLocation("Room 1");
		when(scheduledao.findScheduleById(7)).thenReturn(stored);
		when(scheduledao.updateSchedule(any(ScheduleInfo.class), anyString())).thenReturn(true);
		ScheduleInfo done = interview(7, "U2");
		done.setLocation("Room 1");
		done.setStatus(ScheduleInfo.DONE);

		assertThat(schedule.save(done, "U3").done()).isTrue();
		verify(notifications, never()).interviewChanged(anyInt(), any());

		ScheduleInfo moved = interview(7, "U2");
		moved.setLocation("Room 2");
		assertThat(schedule.save(moved, "U3").done()).isTrue();
		verify(notifications).interviewChanged(7, "U2");
	}

	@Test
	void aNewRoundIsAChangeTheInterviewerHearsOfAndAnUnknownRoundIsRefused() {
		when(scheduledao.findScheduleById(7)).thenReturn(interview(7, "U2"));
		when(scheduledao.updateSchedule(any(ScheduleInfo.class), anyString())).thenReturn(true);
		ScheduleInfo technical = interview(7, "U2");
		technical.setInterviewround("technical");

		assertThat(schedule.save(technical, "U3").done()).isTrue();
		assertThat(technical.getInterviewround()).isEqualTo("TECHNICAL");
		verify(notifications).interviewChanged(7, "U2");

		ScheduleInfo unknown = interview(0, "U2");
		unknown.setInterviewround("LUNCH");
		assertThat(schedule.save(unknown, "U3").message()).isEqualTo("Please choose a round from the list.");
	}

	@Test
	void anEvaluationKeepsItsRoundAndAnUnknownOneIsRefused() {
		when(marksdao.saveMarks(any(MarksInfo.class))).thenReturn(9);
		MarksInfo first = EvaluationServicesTest.scored(7);
		first.setInterviewround(" first ");

		assertThat(marks.saveEvaluation(first, "U2").done()).isTrue();
		assertThat(first.getInterviewround()).isEqualTo("FIRST");
		verify(notifications).evaluationSaved("C1", "U2", "FIRST");
		verify(activityservice).record("U2", "MARKED", "CANDIDATE", "C1", "total 70, round FIRST");

		MarksInfo unknown = EvaluationServicesTest.scored(7);
		unknown.setInterviewround("LUNCH");
		assertThat(marks.saveEvaluation(unknown, "U2").message()).isEqualTo("Please choose a round from the list.");
	}

	@Test
	void aCancelledInterviewNotifiesOnlyWhenItWasCancelled() {
		when(scheduledao.cancelSchedule(5, "U3")).thenReturn(true);
		when(scheduledao.cancelSchedule(6, "U3")).thenReturn(false);

		schedule.cancel(5, "U3");
		schedule.cancel(6, "U3");

		verify(notifications).interviewCancelled(5);
		verify(notifications, never()).interviewCancelled(6);
	}

	@Test
	void aRefusedInterviewSendsNothing() {
		// No interviewer chosen
		assertThat(schedule.save(interview(0, " "), "U3").done()).isFalse();

		verifyNoInteractions(notifications);
	}

	@Test
	void anAssignmentNotifiesTheInterviewer() {
		when(assignmentdao.getAssignableInterviewers("C1")).thenReturn(List.of(user("U2")));
		when(assignmentdao.assign("C1", "U2", "U3")).thenReturn(true);

		assertThat(assignment.assign("C1", "U2", "U3").done()).isTrue();

		verify(notifications).interviewerAssigned("C1", "U2");
	}

	@Test
	void aRefusedAssignmentSendsNothing() {
		when(assignmentdao.getAssignableInterviewers("C1")).thenReturn(List.of());

		assertThat(assignment.assign("C1", "U2", "U3").done()).isFalse();

		verifyNoInteractions(notifications);
	}

	@Test
	void aSavedEvaluationNotifiesHrAndAnUnsavedOneDoesNot() {
		when(marksdao.saveMarks(any(MarksInfo.class))).thenReturn(9, 0);

		assertThat(marks.saveEvaluation(EvaluationServicesTest.scored(7), "U2").done()).isTrue();
		assertThat(marks.saveEvaluation(EvaluationServicesTest.scored(7), "U2").done()).isFalse();

		verify(notifications).evaluationSaved("C1", "U2", null);
		verify(notifications, never()).interviewScheduled(anyInt());
	}

	private void assigned(String candidateid, String interviewerid) {
		AssignmentInfo info = new AssignmentInfo();
		info.setInterviewerid(interviewerid);
		info.setIntervieweractive(true);
		when(assignmentdao.getAssignments(candidateid)).thenReturn(List.of(info));
	}

	private static ScheduleInfo interview(int key, String interviewerid) {
		ScheduleInfo info = new ScheduleInfo();
		info.setSchedulekey(key);
		info.setCandidateid("C1");
		info.setInterviewerid(interviewerid);
		info.setStartat(START);
		return info;
	}

	private static UserInfo user(String userid) {
		UserInfo user = new UserInfo();
		user.setUserid(userid);
		return user;
	}
}
