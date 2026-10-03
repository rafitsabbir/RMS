package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.dao.AssignmentDao;
import rms.dao.DecisionDao;
import rms.dao.MarksDao;
import rms.dao.ScheduleDao;
import rms.model.AssignmentInfo;
import rms.model.Criterion;
import rms.model.DecisionInfo;
import rms.model.MarksInfo;
import rms.model.ScheduleInfo;
import rms.model.UserInfo;

/**
 * The activity log hooks: marking, selecting, rejecting and the other logged actions are recorded after they worked,
 * with keys and codes only; a refused action leaves no entry. Also the English-only checks of those services.
 */
@ExtendWith(MockitoExtension.class)
class ActivityLoggingTest {

	@Mock
	ActivityService activityservice;

	@Mock
	DecisionDao decisiondao;

	@Mock
	MarksDao marksdao;

	@Mock
	AssignmentDao assignmentdao;

	@Mock
	ScheduleDao scheduledao;

	DecisionServiceImpl decisions;
	MarksServiceImpl marks;
	AssignmentServiceImpl assignments;
	ScheduleServiceImpl schedule;

	@BeforeEach
	void setUp() {
		decisions = new DecisionServiceImpl();
		decisions.setDecisionDao(decisiondao);
		decisions.setActivityService(activityservice);
		marks = new MarksServiceImpl();
		marks.setMarksDao(marksdao);
		marks.setActivityService(activityservice);
		assignments = new AssignmentServiceImpl();
		assignments.setAssignmentDao(assignmentdao);
		assignments.setActivityService(activityservice);
		schedule = new ScheduleServiceImpl();
		schedule.setScheduleDao(scheduledao);
		schedule.setAssignmentDao(assignmentdao);
		schedule.setActivityService(activityservice);
	}

	@Test
	void eachDecisionIsLoggedUnderItsOwnAction() {
		when(decisiondao.saveDecision(any(DecisionInfo.class))).thenReturn(true);

		decisions.saveDecision(decision("S", "Strong"), "U3", 1);
		decisions.saveDecision(decision("R", "Weak"), "U3", 1);
		decisions.saveDecision(decision("H", "Wait"), "U3", 1);

		verify(activityservice).record("U3", "SELECTED", "CANDIDATE", "C1", null);
		verify(activityservice).record("U3", "REJECTED", "CANDIDATE", "C1", null);
		verify(activityservice).record("U3", "ON_HOLD", "CANDIDATE", "C1", null);
	}

	@Test
	void aRefusedDecisionLeavesNoEntry() {
		assertThat(decisions.saveDecision(decision("S", " "), "U3", 1).done()).isFalse();
		when(decisiondao.saveDecision(any(DecisionInfo.class))).thenReturn(false);
		assertThat(decisions.saveDecision(decision("S", "Strong"), "U3", 1).done()).isFalse();

		verify(activityservice, never()).record(anyString(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void theReasonMustBeEnglish() {
		Outcome outcome = decisions.saveDecision(decision("S", "Very good আমি"), "U3", 1);

		assertThat(outcome.done()).isFalse();
		assertThat(outcome.message()).contains("English");
		verify(decisiondao, never()).saveDecision(any(DecisionInfo.class));
		// Line breaks are fine in a reason
		when(decisiondao.saveDecision(any(DecisionInfo.class))).thenReturn(true);
		assertThat(decisions.saveDecision(decision("S", "Line one\nLine two"), "U3", 1).done()).isTrue();
	}

	@Test
	void anEvaluationIsLoggedAsMarkedWithItsTotalOnly() {
		when(marksdao.saveMarks(any(MarksInfo.class))).thenReturn(7);
		MarksInfo evaluation = scored(8);
		evaluation.setComments("Calm and clear.");

		assertThat(marks.saveEvaluation(evaluation, "U2").done()).isTrue();

		verify(activityservice).record("U2", "MARKED", "CANDIDATE", "C1", "total 80");
	}

	@Test
	void anEvaluationThatCantBeSavedLeavesNoEntry() {
		when(marksdao.saveMarks(any(MarksInfo.class))).thenReturn(0);

		assertThat(marks.saveEvaluation(scored(8), "U2").done()).isFalse();
		assertThat(marks.saveEvaluation(scored(11), "U2").done()).isFalse();

		verify(activityservice, never()).record(anyString(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void evaluationCommentsMustBeEnglish() {
		MarksInfo overall = scored(5);
		overall.setComments("ممتاز");
		MarksInfo perCriterion = scored(5);
		perCriterion.setComment(Criterion.STRESS, "café");

		assertThat(marks.saveEvaluation(overall, "U2").message()).contains("English");
		assertThat(marks.saveEvaluation(perCriterion, "U2").message()).contains("English");
		verify(marksdao, never()).saveMarks(any(MarksInfo.class));
	}

	@Test
	void assigningAndUnassigningAreLogged() {
		UserInfo interviewer = new UserInfo();
		interviewer.setUserid("U2");
		when(assignmentdao.getAssignableInterviewers("C1")).thenReturn(List.of(interviewer));
		when(assignmentdao.assign("C1", "U2", "U3")).thenReturn(true);
		when(assignmentdao.unassign("C1", "U2", "U3")).thenReturn(true);

		assignments.assign("C1", "U2", "U3");
		assignments.unassign("C1", "U2", "U3");

		verify(activityservice).record("U3", "ASSIGNED", "CANDIDATE", "C1", "interviewer U2");
		verify(activityservice).record("U3", "UNASSIGNED", "CANDIDATE", "C1", "interviewer U2");
	}

	@Test
	void schedulingChangingAndCancellingAnInterviewAreLogged() {
		AssignmentInfo assignment = new AssignmentInfo();
		assignment.setInterviewerid("U2");
		assignment.setIntervieweractive(true);
		when(assignmentdao.getAssignments("C1")).thenReturn(List.of(assignment));
		when(scheduledao.updateSchedule(any(ScheduleInfo.class), anyString())).thenReturn(true);
		when(scheduledao.cancelSchedule(5, "U3")).thenReturn(true);
		ScheduleInfo stored = interview(5);
		when(scheduledao.findScheduleById(5)).thenReturn(stored);

		schedule.save(interview(0), "U3");
		schedule.save(interview(5), "U3");
		schedule.cancel(5, "U3");

		verify(activityservice).record("U3", "INTERVIEW_SCHEDULED", "CANDIDATE", "C1", "interviewer U2");
		verify(activityservice).record("U3", "INTERVIEW_CHANGED", "INTERVIEW", "5", "candidate C1, status SCHEDULED");
		verify(activityservice).record("U3", "INTERVIEW_CANCELLED", "INTERVIEW", "5", null);
	}

	@Test
	void theInterviewLocationMustBeEnglish() {
		ScheduleInfo info = interview(0);
		info.setLocation("会議室");

		Outcome outcome = schedule.save(info, "U3");

		assertThat(outcome.done()).isFalse();
		assertThat(outcome.message()).contains("English");
		verify(scheduledao, never()).addSchedule(any(ScheduleInfo.class), anyString());
	}

	private static ScheduleInfo interview(int key) {
		ScheduleInfo info = new ScheduleInfo();
		info.setSchedulekey(key);
		info.setCandidateid("C1");
		info.setInterviewerid("U2");
		info.setStartat(LocalDateTime.of(2099, 3, 4, 10, 30));
		info.setStatus(ScheduleInfo.SCHEDULED);
		return info;
	}

	private static MarksInfo scored(int score) {
		MarksInfo evaluation = new MarksInfo();
		evaluation.setCandidateid("C1");
		for (Criterion criterion : Criterion.values()) {
			evaluation.setScore(criterion, score);
		}
		return evaluation;
	}

	private static DecisionInfo decision(String status, String reason) {
		DecisionInfo decision = new DecisionInfo();
		decision.setCandidateid("C1");
		decision.setStatus(status);
		decision.setReason(reason);
		decision.setDecisiondate(LocalDate.now());
		return decision;
	}
}
