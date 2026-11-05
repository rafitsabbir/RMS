package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.HiringGroup;
import rms.model.ReminderInfo;

/**
 * Characterization tests against db/schema.sql + db/test-seed.sql. C1 is Selected and C2 Rejected, so both are
 * decided; U2 is assigned to both and has evaluated both; U5 (inactive) is assigned to C1. Job 1 is open, job 2
 * closed, job 3 deleted. C1 has a current CV, C2 none.
 */
class DashboardDaoImplTest extends MySqlContainerSupport {

	DashboardDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new DashboardDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void candidatesAreCountedByTrimmedUpperCaseStatus() {
		assertThat(dao.countCandidatesByStatus()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("S", 1, "R", 1));

		// CHAR(1) in db/schema.sql (inferred, open question #19): lower case counts as upper case, and a lone space
		// or NULL as no status
		jdbcTemplate.update("update candidate set candidatestatus=' ' where candidateid='C1'");
		jdbcTemplate.update("update candidate set candidatestatus='h' where candidateid='C2'");
		assertThat(dao.countCandidatesByStatus()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("", 1, "H", 1));
		jdbcTemplate.update("update candidate set candidatestatus=null where candidateid='C1'");
		assertThat(dao.countCandidatesByStatus()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("", 1, "H", 1));

		// Deleted candidates aren't counted
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C2'");
		assertThat(dao.countCandidatesByStatus()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("", 1));
	}

	@Test
	void decidedCandidatesHaveNoPendingEvaluations() {
		assertThat(dao.countPendingEvaluations(null)).isZero();
	}

	@Test
	void aPendingEvaluationIsAnActiveAssignedInterviewerWhoHasntEvaluatedAnUndecidedCandidate() {
		jdbcTemplate.update("update candidate set candidatestatus=null where candidateid='C1'");
		// U2 evaluated C1, and U5 (inactive) can't evaluate: nothing pending
		assertThat(dao.countPendingEvaluations(null)).isZero();

		jdbcTemplate.update("update marks set isactive=0 where interviewerid='U2' and candidateid='C1'");
		assertThat(dao.countPendingEvaluations(null)).isEqualTo(1);
		assertThat(dao.countPendingEvaluations("U2")).isEqualTo(1);
		assertThat(dao.countPendingEvaluations("U5")).isZero();
		assertThat(dao.countPendingEvaluations("U3")).isZero();

		// A double-clicked assignment is still one pending evaluation
		jdbcTemplate.update("insert into candidate_interviewer (candidateid, interviewerid, assignedby, assignedat, "
				+ "isactive) values ('C1', 'U2', 'U3', now(), 1)");
		assertThat(dao.countPendingEvaluations(null)).isEqualTo(1);

		// On hold doesn't lock the evaluations; Selected does
		jdbcTemplate.update("update candidate set candidatestatus='h' where candidateid='C1'");
		assertThat(dao.countPendingEvaluations("U2")).isEqualTo(1);
		jdbcTemplate.update("update candidate set candidatestatus='s' where candidateid='C1'");
		assertThat(dao.countPendingEvaluations("U2")).isZero();
	}

	@Test
	void anUnassignedOrDeletedCandidateIsNotPending() {
		jdbcTemplate.update("update candidate set candidatestatus=null");
		jdbcTemplate.update("update marks set isactive=0");
		// U2 has C1 and C2 to do (U5's assignment to C1 is inactive-interviewer, C2's is unassigned)
		assertThat(dao.countPendingEvaluations(null)).isEqualTo(2);

		jdbcTemplate.update("update candidate_interviewer set isactive=0 where assignmentkey=1");
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C2'");
		assertThat(dao.countPendingEvaluations(null)).isZero();
	}

	@Test
	void openJobsAreActiveJobsWithStatusOpen() {
		assertThat(dao.countOpenJobs()).isEqualTo(1);

		jdbcTemplate.update("update job set status='OPEN' where jobkey=2");
		assertThat(dao.countOpenJobs()).isEqualTo(2);
		jdbcTemplate.update("update job set isactive=0 where jobkey=1");
		assertThat(dao.countOpenJobs()).isEqualTo(1);
	}

	@Test
	void candidatesWithoutACurrentCv() {
		// C2 has no document at all
		assertThat(dao.countCandidatesWithoutCv()).isEqualTo(1);

		// C1's CV deleted (document 4 is an older, replaced CV, also inactive)
		jdbcTemplate.update("update candidate_document set isactive=0 where documentkey=1");
		assertThat(dao.countCandidatesWithoutCv()).isEqualTo(2);

		// A purged CV (file removed) is no CV, and another document type isn't a CV
		jdbcTemplate.update("update candidate_document set isactive=1, purgedat=now() where documentkey=1");
		assertThat(dao.countCandidatesWithoutCv()).isEqualTo(2);
		jdbcTemplate.update("update candidate_document set purgedat=null where documentkey=1");
		assertThat(dao.countCandidatesWithoutCv()).isEqualTo(1);

		// Deleted candidates aren't counted
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C2'");
		assertThat(dao.countCandidatesWithoutCv()).isZero();
	}

	@Test
	void candidatesWithAnInactiveInterviewer() {
		// C1 is Selected: nothing left to do for it, so U5's assignment doesn't count
		assertThat(dao.countCandidatesWithInactiveInterviewer()).isZero();

		jdbcTemplate.update("update candidate set candidatestatus='H' where candidateid='C1'");
		assertThat(dao.countCandidatesWithInactiveInterviewer()).isEqualTo(1);

		// An interviewer whose role changed is no longer an interviewer: C1 and C2 (reopened) both count, once each
		jdbcTemplate.update("update candidate set candidatestatus=null where candidateid='C2'");
		jdbcTemplate.update("update admin set role='HR' where userid='U2'");
		assertThat(dao.countCandidatesWithInactiveInterviewer()).isEqualTo(2);

		// Unassigning the inactive interviewers and the changed one clears it
		jdbcTemplate.update("update candidate_interviewer set isactive=0");
		assertThat(dao.countCandidatesWithInactiveInterviewer()).isZero();
	}

	@Test
	void pendingEvaluationsPerInterviewerMatchTheTotal() {
		assertThat(dao.countPendingEvaluationsByInterviewer()).isEmpty();

		jdbcTemplate.update("update candidate set candidatestatus=null");
		jdbcTemplate.update("update marks set isactive=0");
		// The same 2 as countPendingEvaluations: both are U2's, named from admin; a double assignment counts once
		jdbcTemplate.update("insert into candidate_interviewer (candidateid, interviewerid, assignedby, assignedat, "
				+ "isactive) values ('C1', 'U2', 'U3', now(), 1)");
		assertThat(dao.countPendingEvaluationsByInterviewer())
				.extracting(HiringGroup::getLabel, HiringGroup::getTotal)
				.containsExactly(tuple("Ivan Interviewer (U2)", dao.countPendingEvaluations(null)));
	}

	@Test
	void candidatesAreCountedByStage() {
		// Seed: C1 OFFER, C2 INTERVIEW
		assertThat(dao.countCandidatesByStage()).containsOnly(java.util.Map.entry("OFFER", 1), java.util.Map.entry("INTERVIEW", 1));

		jdbcTemplate.update("update candidate set stage=' interview' where candidateid='C1'");
		jdbcTemplate.update("update candidate set stage=null where candidateid='C2'");
		assertThat(dao.countCandidatesByStage()).containsOnly(java.util.Map.entry("INTERVIEW", 1), java.util.Map.entry("", 1));
	}

	@Test
	void theWorkloadReadsActiveInterviewersAndTheirInterviewsPerDay() {
		// F9: the seed's Interviewer is U2; an HR user counts once they also interview
		assertThat(dao.getActiveInterviewers()).containsKey("U2").doesNotContainKey("U3");
		assertThat(dao.getActiveInterviewers().get("U2")).isEqualTo("Ivan Interviewer (U2)");
		jdbcTemplate.update("update admin set alsointerviewer=1 where userid='U3'");
		assertThat(dao.getActiveInterviewers()).containsKeys("U2", "U3");

		// Seed: U2 on 2099-01-15 (scheduled); U5's 2099-01-16 interview is cancelled
		Map<String, Map<LocalDate, Integer>> counts = dao.countInterviewsByDay(LocalDate.of(2099, 1, 12),
				LocalDate.of(2099, 1, 19));
		assertThat(counts).containsOnlyKeys("U2");
		assertThat(counts.get("U2")).containsExactly(Map.entry(LocalDate.of(2099, 1, 15), 1));
		// The end is exclusive, and a deleted candidate's interviews don't count
		assertThat(dao.countInterviewsByDay(LocalDate.of(2099, 1, 12), LocalDate.of(2099, 1, 15))).isEmpty();
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C1'");
		assertThat(dao.countInterviewsByDay(LocalDate.of(2099, 1, 12), LocalDate.of(2099, 1, 19))).isEmpty();
	}

	@Test
	void anEvaluationIsOverdueAfterAnInterviewInItsRound() {
		LocalDateTime now = LocalDateTime.of(2026, 11, 5, 9, 0);
		// Seed: U2 interviewed C2 on 2026-09-18 and evaluated; C2 is Rejected (decided): no reminder
		assertThat(dao.getOverdueEvaluations(null, now)).isEmpty();

		jdbcTemplate.update("update candidate set candidatestatus=null where candidateid='C2'");
		assertThat(dao.getOverdueEvaluations(null, now)).isEmpty();
		jdbcTemplate.update("update marks set isactive=0 where candidateid='C2' and interviewerid='U2'");
		assertThat(dao.getOverdueEvaluations(null, now)).extracting(ReminderInfo::getCandidateid,
				ReminderInfo::getInterviewerid, ReminderInfo::getSince, ReminderInfo::getCandidatename,
				ReminderInfo::getInterviewername).containsExactly(tuple("C2", "U2", LocalDate.of(2026, 9, 18),
						"Cody Candidate", "Ivan Interviewer"));
		// Of one interviewer only; not before the interview
		assertThat(dao.getOverdueEvaluations("U5", now)).isEmpty();
		assertThat(dao.getOverdueEvaluations("U2", LocalDateTime.of(2026, 9, 18, 9, 0))).isEmpty();

		// With a round, only an evaluation in that round counts
		jdbcTemplate.update("update interview_schedule set interviewround='TECHNICAL' where schedulekey=2");
		jdbcTemplate.update("update marks set isactive=1 where candidateid='C2' and interviewerid='U2'");
		assertThat(dao.getOverdueEvaluations("U2", now)).extracting(ReminderInfo::getInterviewround)
				.containsExactly("TECHNICAL");
		jdbcTemplate.update("update marks set interviewround='TECHNICAL' where candidateid='C2' and interviewerid='U2'");
		assertThat(dao.getOverdueEvaluations("U2", now)).isEmpty();

		// A cancelled interview, or an interviewer no longer assigned, is no reminder
		jdbcTemplate.update("update marks set isactive=0 where candidateid='C2'");
		jdbcTemplate.update("update candidate_interviewer set isactive=0 where candidateid='C2' and interviewerid='U2'");
		assertThat(dao.getOverdueEvaluations(null, now)).isEmpty();
	}

	@Test
	void aCandidateIsLongAtAStageFromTheLastMoveIntoIt() {
		// Seed: C1 (Selected) reached Offer on 2026-09-20; C2 is Rejected
		assertThat(dao.getStuckCandidates(LocalDate.of(2026, 11, 1))).extracting(ReminderInfo::getCandidateid,
				ReminderInfo::getStage, ReminderInfo::getSince, ReminderInfo::getCandidatename)
				.containsExactly(tuple("C1", "OFFER", LocalDate.of(2026, 9, 20), "Carla Candidate"));
		assertThat(dao.getStuckCandidates(LocalDate.of(2026, 9, 20))).isEmpty();

		// Hired and Withdrawn are done; a candidate at Applied without history counts from the applied date
		jdbcTemplate.update("update candidate set stage='HIRED' where candidateid='C1'");
		jdbcTemplate.update("update candidate set candidatestatus=null, stage=null where candidateid='C2'");
		assertThat(dao.getStuckCandidates(LocalDate.of(2026, 11, 1))).extracting(ReminderInfo::getCandidateid,
				ReminderInfo::getSince).containsExactly(tuple("C2", LocalDate.of(2026, 9, 15)));
	}
}
