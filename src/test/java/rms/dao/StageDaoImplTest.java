package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.StageInfo;

/** Characterization tests against db/schema.sql + db/test-seed.sql (C1 at Offer, C2 at Interview, 3 history rows). */
class StageDaoImplTest extends MySqlContainerSupport {

	StageDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new StageDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void theStageOfAnActiveCandidateOrNull() {
		assertThat(dao.getStage("C1")).isEqualTo("OFFER");
		assertThat(dao.getStage("C9")).isNull();
		// A NULL stage is "" so it differs from "no candidate"
		jdbcTemplate.update("update candidate set stage=null where candidateid='C2'");
		assertThat(dao.getStage("C2")).isEmpty();
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C1'");
		assertThat(dao.getStage("C1")).isNull();
	}

	@Test
	void aMoveUpdatesTheCandidateAndKeepsTheHistory() {
		assertThat(dao.moveStage("C1", "OFFER", "HIRED", "  Signed on Monday ", "U3")).isTrue();

		assertThat(dao.getStage("C1")).isEqualTo("HIRED");
		StageInfo latest = dao.getStages("C1").get(0);
		assertThat(latest.getFromstage()).isEqualTo("OFFER");
		assertThat(latest.getTostage()).isEqualTo("HIRED");
		assertThat(latest.getNote()).isEqualTo("Signed on Monday");
		assertThat(latest.getChangedbyname()).isEqualTo("Hana Recruiter");
		assertThat(latest.getChangedat()).isNotNull();
	}

	@Test
	void aStaleOrMissingCandidateIsNotMoved() {
		// The stored stage is OFFER, not INTERVIEW: someone moved the candidate meanwhile
		assertThat(dao.moveStage("C1", "INTERVIEW", "HIRED", null, "U3")).isFalse();
		assertThat(dao.getStage("C1")).isEqualTo("OFFER");
		assertThat(dao.moveStage("C9", null, "SCREENING", null, "U3")).isFalse();
		assertThat(dao.getStages("C1")).hasSize(2);

		// A NULL stage matches an expected null (the service passes the stored value on unchanged)
		jdbcTemplate.update("update candidate set stage=null, candidatestatus=null where candidateid='C2'");
		assertThat(dao.moveStage("C2", null, "SCREENING", "", "U3")).isTrue();
		assertThat(dao.getStages("C2").get(0).getNote()).isNull();
	}

	@Test
	void theDecisionIsCheckedAgainOnTheLockedRow() {
		// C2 is Rejected: no move at all, even with the right stage
		assertThat(dao.moveStage("C2", "INTERVIEW", "WITHDRAWN", null, "U3")).isFalse();
		// On hold: a stage before Offer may change, Offer and Hired may not
		jdbcTemplate.update("update candidate set candidatestatus='h' where candidateid='C2'");
		assertThat(dao.moveStage("C2", "INTERVIEW", "OFFER", null, "U3")).isFalse();
		assertThat(dao.moveStage("C2", "INTERVIEW", "SCREENING", null, "U3")).isTrue();
		// Selected (any case): Offer and Hired may
		jdbcTemplate.update("update candidate set candidatestatus='s' where candidateid='C2'");
		assertThat(dao.moveStage("C2", "SCREENING", "OFFER", null, "U3")).isTrue();
	}

	@Test
	void theHistoryIsNewestFirst() {
		assertThat(dao.getStages("C1")).extracting(StageInfo::getFromstage, StageInfo::getTostage, StageInfo::getNote)
				.containsExactly(tuple("INTERVIEW", "OFFER", "Decision: Selected"), tuple("APPLIED", "INTERVIEW", null));
		assertThat(dao.getStages("C9")).isEmpty();
	}
}
