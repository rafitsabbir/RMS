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
}
