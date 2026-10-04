package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

		// The column is CHAR(1): lower case counts as upper case, and a lone space or NULL as no status
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
}
