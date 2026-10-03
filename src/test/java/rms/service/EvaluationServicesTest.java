package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.dao.AssignmentDao;
import rms.dao.DecisionDao;
import rms.dao.MarksDao;
import rms.model.Criterion;
import rms.model.DecisionInfo;
import rms.model.MarksInfo;
import rms.model.UserInfo;

/** The Phase 3 services: evaluation checks, assignment rules and decision checks. */
class EvaluationServicesTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class Evaluations {

		@Mock
		MarksDao marksdao;

		@Mock
		ActivityService activityservice;

		@InjectMocks
		MarksServiceImpl service;

		@Test
		void savesWithTheSessionsInterviewerAndTrimmedComments() {
			when(marksdao.saveMarks(any(MarksInfo.class))).thenReturn(7);
			MarksInfo marks = scored(6);
			marks.setInterviewerid("U9");
			marks.setComments("  Good.  ");
			marks.setComment(Criterion.STRESS, " ");

			assertThat(service.saveEvaluation(marks, "U2")).isEqualTo(Outcome.ok("Evaluation saved."));

			ArgumentCaptor<MarksInfo> captor = ArgumentCaptor.forClass(MarksInfo.class);
			verify(marksdao).saveMarks(captor.capture());
			assertThat(captor.getValue().getInterviewerid()).isEqualTo("U2");
			assertThat(captor.getValue().getComments()).isEqualTo("Good.");
			assertThat(captor.getValue().getComment(Criterion.STRESS)).isNull();
		}

		@Test
		void everyScoreMustBeOneToTen() {
			String message = "Please score every criterion from 1 to 10.";
			for (int bad : new int[] { 0, 11, -1 }) {
				MarksInfo marks = scored(5);
				marks.setScore(Criterion.ATTITUDE, bad);
				assertThat(service.saveEvaluation(marks, "U2")).isEqualTo(Outcome.refused(message));
			}
			MarksInfo edges = scored(1);
			edges.setScore(Criterion.PERSONALITY, 10);
			when(marksdao.saveMarks(any(MarksInfo.class))).thenReturn(1);
			assertThat(service.saveEvaluation(edges, "U2").done()).isTrue();
		}

		@Test
		void commentLengths() {
			MarksInfo marks = scored(5);
			marks.setComments("x".repeat(1001));
			assertThat(service.saveEvaluation(marks, "U2").message())
					.isEqualTo("The overall comment can have at most 1000 characters.");

			marks = scored(5);
			marks.setComment(Criterion.EDUCATION, "x".repeat(256));
			assertThat(service.saveEvaluation(marks, "U2").message())
					.isEqualTo("The comment on Educational Background can have at most 255 characters.");

			verify(marksdao, never()).saveMarks(any(MarksInfo.class));
		}

		@Test
		void refusedByTheDaoWhenDecidedOrUnassigned() {
			when(marksdao.saveMarks(any(MarksInfo.class))).thenReturn(0);

			assertThat(service.saveEvaluation(scored(5), "U2"))
					.isEqualTo(Outcome.refused(MarksServiceImpl.NOT_SAVED));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class Assignments {

		@Mock
		AssignmentDao assignmentdao;

		@Mock
		ActivityService activityservice;

		@InjectMocks
		AssignmentServiceImpl service;

		@Test
		void assignsOnlyAnAssignableInterviewer() {
			when(assignmentdao.getAssignableInterviewers("C1")).thenReturn(List.of(user("U2")));
			when(assignmentdao.assign("C1", "U2", "U3")).thenReturn(true);

			assertThat(service.assign("C1", "U2", "U3")).isEqualTo(Outcome.ok("Interviewer assigned."));
			// HR, an inactive interviewer or a made-up ID isn't in the list (decision F)
			assertThat(service.assign("C1", "U3", "U3"))
					.isEqualTo(Outcome.refused("Please choose an active interviewer who isn't assigned yet."));
			assertThat(service.assign("C1", null, "U3").done()).isFalse();
			verify(assignmentdao, never()).assign("C1", "U3", "U3");
		}

		@Test
		void aDoubleAssignIsRefused() {
			when(assignmentdao.getAssignableInterviewers("C1")).thenReturn(List.of(user("U2")));
			when(assignmentdao.assign("C1", "U2", "U3")).thenReturn(false);

			assertThat(service.assign("C1", "U2", "U3")).isEqualTo(Outcome.refused("That interviewer is assigned already."));
		}

		@Test
		void unassign() {
			when(assignmentdao.unassign("C1", "U2", "U3")).thenReturn(true);
			when(assignmentdao.unassign("C1", "U5", "U3")).thenReturn(false);

			assertThat(service.unassign("C1", "U2", "U3").done()).isTrue();
			assertThat(service.unassign("C1", "U5", "U3"))
					.isEqualTo(Outcome.refused("That interviewer isn't assigned any more."));
			assertThat(service.unassign("C1", null, "U3").done()).isFalse();
		}

		@Test
		void isAssignedNeedsAUser() {
			assertThat(service.isAssigned("C1", null)).isFalse();
			verify(assignmentdao, never()).isAssigned(anyString(), any());
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class Decisions {

		@Mock
		DecisionDao decisiondao;

		@Mock
		ActivityService activityservice;

		@InjectMocks
		DecisionServiceImpl service;

		@Test
		void savesWithTheDeciderAndANote() {
			when(decisiondao.saveDecision(any(DecisionInfo.class))).thenReturn(true);
			DecisionInfo decision = decision(" s ", "  Strong interview  ", LocalDate.now());

			assertThat(service.saveDecision(decision, "U3", 2)).isEqualTo(Outcome.ok(
					"Decision saved: Selected. The candidate's evaluations can no longer be changed."));

			ArgumentCaptor<DecisionInfo> captor = ArgumentCaptor.forClass(DecisionInfo.class);
			verify(decisiondao).saveDecision(captor.capture());
			assertThat(captor.getValue().getStatus()).isEqualTo("S");
			assertThat(captor.getValue().getReason()).isEqualTo("Strong interview");
			assertThat(captor.getValue().getDecidedby()).isEqualTo("U3");
		}

		@Test
		void onHoldDoesntLockAndZeroEvaluationsAreAllowedWithANote() {
			when(decisiondao.saveDecision(any(DecisionInfo.class))).thenReturn(true);

			assertThat(service.saveDecision(decision("H", "Waiting", LocalDate.now().minusDays(1)), "U3", 0))
					.isEqualTo(Outcome.ok("Decision saved: On hold. Note: there are no evaluations yet."));
		}

		@Test
		void refusals() {
			expect(decision("X", "r", LocalDate.now()), "Please choose Selected, Rejected or On hold.");
			expect(decision(null, "r", LocalDate.now()), "Please choose Selected, Rejected or On hold.");
			expect(decision("R", " ", LocalDate.now()), "Please give the reason for the decision.");
			expect(decision("R", "x".repeat(501), LocalDate.now()), "The reason can have at most 500 characters.");
			expect(decision("R", "r", null), "Please enter the date of the decision.");
			expect(decision("R", "r", LocalDate.now().plusDays(1)), "The date of the decision can't be in the future.");
			expect(decision("R", "r", LocalDate.of(1999, 12, 31)), "Please check the date of the decision.");

			verify(decisiondao, never()).saveDecision(any(DecisionInfo.class));
		}

		@Test
		void deletedCandidate() {
			when(decisiondao.saveDecision(any(DecisionInfo.class))).thenReturn(false);

			assertThat(service.saveDecision(decision("R", "r", LocalDate.now()), "U3", 1))
					.isEqualTo(Outcome.refused("The candidate no longer exists."));
		}

		private void expect(DecisionInfo decision, String message) {
			assertThat(service.saveDecision(decision, "U3", 1)).isEqualTo(Outcome.refused(message));
		}
	}

	static MarksInfo scored(int score) {
		MarksInfo marks = new MarksInfo();
		marks.setCandidateid("C1");
		for (Criterion criterion : Criterion.values()) {
			marks.setScore(criterion, score);
		}
		return marks;
	}

	static DecisionInfo decision(String status, String reason, LocalDate date) {
		DecisionInfo decision = new DecisionInfo();
		decision.setCandidateid("C1");
		decision.setStatus(status);
		decision.setReason(reason);
		decision.setDecisiondate(date);
		return decision;
	}

	static UserInfo user(String userid) {
		UserInfo user = new UserInfo();
		user.setUserid(userid);
		return user;
	}
}
