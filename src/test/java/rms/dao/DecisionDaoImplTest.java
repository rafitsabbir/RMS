package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.DecisionInfo;

/** Characterization tests against db/schema.sql + db/test-seed.sql (C1: On hold then Selected; C2: Rejected). */
class DecisionDaoImplTest extends MySqlContainerSupport {

	DecisionDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new DecisionDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void decisionsAreNewestFirstWithTheDecidersName() {
		assertThat(dao.getDecisions("C1"))
				.extracting(DecisionInfo::getStatus, DecisionInfo::getReason, DecisionInfo::getDecisiondate,
						DecisionInfo::getDecidedbyname)
				.containsExactly(
						tuple("S", "Strong technical interview", LocalDate.of(2026, 9, 20), "Hana Recruiter"),
						tuple("H", "Waiting for a second interview", LocalDate.of(2026, 9, 10), "Hana Recruiter"));
		assertThat(dao.getDecisions("C9")).isEmpty();
	}

	@Test
	void saveSetsTheCandidateAndLogsTheDecision() {
		assertThat(dao.saveDecision(decision("C2", "H", "Second interview requested"))).isTrue();

		Map<String, Object> candidate = jdbcTemplate.queryForMap("select * from candidate where candidateid='C2'");
		assertThat(candidate.get("candidatestatus")).isEqualTo("H");
		assertThat(candidate.get("decisionreason")).isEqualTo("Second interview requested");
		assertThat(candidate.get("decisiondate")).isEqualTo(java.sql.Date.valueOf("2026-09-30"));
		assertThat(candidate.get("decidedby")).isEqualTo("U1");
		DecisionInfo latest = dao.getDecisions("C2").get(0);
		assertThat(latest.getStatus()).isEqualTo("H");
		assertThat(latest.getDecidedby()).isEqualTo("U1");
		assertThat(latest.getDecidedat()).isNotNull();
		assertThat(dao.getDecisions("C2")).hasSize(2);
	}

	@Test
	void sameDecisionAgainIsStillLogged() {
		assertThat(dao.saveDecision(decision("C2", "R", "Not enough experience for the role"))).isTrue();
		assertThat(dao.saveDecision(decision("C2", "R", "Not enough experience for the role"))).isTrue();

		assertThat(dao.getDecisions("C2")).hasSize(3);
	}

	@Test
	void deletedOrUnknownCandidateIsRefusedAndNothingIsLogged() {
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C2'");

		assertThat(dao.saveDecision(decision("C2", "S", "x"))).isFalse();
		assertThat(dao.saveDecision(decision("C9", "S", "x"))).isFalse();

		assertThat(jdbcTemplate.queryForObject("select count(*) from candidate_decision", Integer.class)).isEqualTo(3);
		assertThat(jdbcTemplate.queryForObject("select candidatestatus from candidate where candidateid='C2'",
				String.class)).isEqualTo("R");
	}

	private static DecisionInfo decision(String candidateid, String status, String reason) {
		DecisionInfo decision = new DecisionInfo();
		decision.setCandidateid(candidateid);
		decision.setStatus(status);
		decision.setReason(reason);
		decision.setDecisiondate(LocalDate.of(2026, 9, 30));
		decision.setDecidedby("U1");
		return decision;
	}

	@Test
	void selectedMovesACandidateBeforeOfferToOfferWithAHistoryRow() {
		// C2 is at Interview in the seed
		assertThat(dao.saveDecision(decision("C2", "S", "Second look went well"))).isTrue();

		assertThat(jdbcTemplate.queryForObject("select stage from candidate where candidateid='C2'", String.class))
				.isEqualTo("OFFER");
		Map<String, Object> history = jdbcTemplate.queryForMap("select * from candidate_stage where candidateid='C2' "
				+ "order by stagekey desc limit 1");
		assertThat(history.get("fromstage")).isEqualTo("INTERVIEW");
		assertThat(history.get("tostage")).isEqualTo("OFFER");
		assertThat(history.get("changedby")).isEqualTo("U1");
		assertThat(history.get("note")).isEqualTo("Decision: Selected");
	}

	@Test
	void selectedLeavesOfferHiredAndWithdrawnAndOtherDecisionsLeaveTheStage() {
		int before = jdbcTemplate.queryForObject("select count(*) from candidate_stage", Integer.class);
		jdbcTemplate.update("update candidate set stage='HIRED' where candidateid='C1'");
		dao.saveDecision(decision("C1", "S", "Still selected"));
		assertThat(jdbcTemplate.queryForObject("select stage from candidate where candidateid='C1'", String.class))
				.isEqualTo("HIRED");

		dao.saveDecision(decision("C2", "H", "Waiting"));
		dao.saveDecision(decision("C2", "R", "No"));
		assertThat(jdbcTemplate.queryForObject("select stage from candidate where candidateid='C2'", String.class))
				.isEqualTo("INTERVIEW");
		assertThat(jdbcTemplate.queryForObject("select count(*) from candidate_stage", Integer.class)).isEqualTo(before);
	}

	@Test
	void aNullStageReadsAsAppliedAndMovesToOffer() {
		jdbcTemplate.update("update candidate set stage=null where candidateid='C2'");

		dao.saveDecision(decision("C2", "S", "Yes"));

		assertThat(jdbcTemplate.queryForObject("select stage from candidate where candidateid='C2'", String.class))
				.isEqualTo("OFFER");
		assertThat(jdbcTemplate.queryForObject("select fromstage from candidate_stage where candidateid='C2' "
				+ "order by stagekey desc limit 1", String.class)).isNull();
	}
}
