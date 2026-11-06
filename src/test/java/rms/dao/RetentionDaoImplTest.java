package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.RetentionInfo;

/** F11: who is due (not hired, no activity since the cut-off) and what the clean-up removes. */
class RetentionDaoImplTest extends MySqlContainerSupport {

	RetentionDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new RetentionDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void dueAreCandidatesNotHiredWithoutActivitySinceTheCutoff() {
		// Seed: C2's last activity is the decision on 2026-09-22; C1 has an interview in 2099
		assertThat(dao.getDue(LocalDate.of(2026, 10, 1))).extracting(RetentionInfo::getCandidateid,
				RetentionInfo::getLastactivity, RetentionInfo::getName, RetentionInfo::isActive)
				.containsExactly(tuple("C2", LocalDate.of(2026, 9, 22), "Cody Candidate", true));
		assertThat(dao.getDue(LocalDate.of(2026, 9, 22))).isEmpty();

		// A later evaluation is activity too; a deleted candidate is still due; a hired one never is
		jdbcTemplate.update("update marks set updatedat='2026-10-05 10:00:00' where candidateid='C2'");
		assertThat(dao.getDue(LocalDate.of(2026, 10, 1))).isEmpty();
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C2'");
		assertThat(dao.getDue(LocalDate.of(2026, 11, 1))).extracting(RetentionInfo::isActive).containsExactly(false);
		jdbcTemplate.update("update candidate set stage='HIRED' where candidateid='C2'");
		assertThat(dao.getDue(LocalDate.of(2026, 11, 1))).isEmpty();
	}

	@Test
	void theCleanUpRemovesThePersonalDataAndKeepsTheRows() {
		jdbcTemplate.update("update marks set comments='Knows Nora well', stresscomment='Calm' where candidateid='C2'");
		jdbcTemplate.update("insert into candidate_offer (candidateid, offerdate, salaryband, status, note, createdby, "
				+ "createdat) values ('C2', '2026-09-20', 'Band 3', 'DECLINED', 'Wants more', 'U3', now())");
		jdbcTemplate.update("insert into candidate_decision (candidateid, status, reason, decisiondate, decidedby, "
				+ "decidedat) values ('C2', 'R', 'Too junior', '2026-09-22', 'U3', now())");

		assertThat(dao.anonymize("C2", "U1")).isTrue();

		Map<String, Object> c = jdbcTemplate.queryForMap("select * from candidate where candidateid='C2'");
		assertThat(c.get("firstname")).isEqualTo("Removed");
		assertThat(c.get("lastname")).isEqualTo("candidate");
		assertThat(c.get("email")).isNull();
		assertThat(c.get("phone")).isNull();
		assertThat(c.get("decisionreason")).isNull();
		assertThat(c.get("isactive")).isEqualTo(0);
		assertThat(c.get("anonymizedby")).isEqualTo("U1");
		assertThat(c.get("anonymizedat")).isNotNull();
		// The decision and the scores stay for the counts, without their text
		assertThat(c.get("candidatestatus")).isEqualTo("R");
		assertThat(jdbcTemplate.queryForList("select reason from candidate_decision where candidateid='C2'", String.class))
				.containsOnly("Removed");
		assertThat(jdbcTemplate.queryForObject("select count(*) from marks where candidateid='C2' and (comments is not null "
				+ "or stresscomment is not null)", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("select workexp from marks where candidateid='C2' and interviewerid='U2'",
				Integer.class)).isEqualTo(3);
		assertThat(jdbcTemplate.queryForMap("select salaryband, note from candidate_offer where candidateid='C2'"))
				.containsEntry("salaryband", null).containsEntry("note", null);
		assertThat(jdbcTemplate.queryForObject("select count(*) from candidate_stage where candidateid='C2' and note is not null",
				Integer.class)).isZero();

		// Once only; and it is no longer due
		assertThat(dao.anonymize("C2", "U1")).isFalse();
		assertThat(dao.getDue(LocalDate.of(2026, 11, 1))).isEmpty();
	}
}
