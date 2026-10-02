package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.CandidateInfo;

/** Characterization tests against db/schema.sql + db/test-seed.sql. */
class CandidateDaoImplTest extends MySqlContainerSupport {

	CandidateDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new CandidateDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void listReturnsEveryCandidateWithNamesAndStatus() {
		assertThat(dao.getAllCandidate())
				.extracting(CandidateInfo::getCandidateid, CandidateInfo::getPositionname,
						CandidateInfo::getLanguagename, CandidateInfo::getCandidatestatus)
				.containsExactly(
						tuple("C1", "SOFTWARE ENGINEER", "JAVA", "S"),
						tuple("C2", "QA ENGINEER", "PYTHON", "R"));
	}

	@Test
	void listKeepsCandidatesWhosePositionIsDeletedOrMissing() {
		jdbcTemplate.update("update candidate set positionkey=3 where candidateid='C1'");
		jdbcTemplate.update("update candidate set languagekey=99 where candidateid='C2'");

		assertThat(dao.getAllCandidate()).hasSize(2);
		assertThat(dao.findCandidateById("C1").getPositionname()).isEqualTo("RETIRED ROLE");
		assertThat(dao.findCandidateById("C2").getLanguagename()).isNull();
	}

	@Test
	void findReturnsTheCandidateOrNull() {
		CandidateInfo candidate = dao.findCandidateById("C1");

		assertThat(candidate.getFirstname()).isEqualTo("Carla");
		assertThat(candidate.getLastname()).isEqualTo("Candidate");
		assertThat(candidate.getPositionkey()).isEqualTo(1);
		assertThat(candidate.getLanguagekey()).isEqualTo(1);
		assertThat(dao.findCandidateById("C9")).isNull();
	}

	@Test
	void addInsertsTrimmedValuesWithNoStatus() {
		assertThat(dao.addCandidate(candidate(" C3 ", " Dana ", " Doe ", 2, 1))).isTrue();

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from candidate where candidateid='C3'");
		assertThat(row.get("firstname")).isEqualTo("Dana");
		assertThat(row.get("lastname")).isEqualTo("Doe");
		assertThat(row.get("positionkey")).isEqualTo(2);
		assertThat(row.get("languagekey")).isEqualTo(1);
		// G9: nothing sets the status yet
		assertThat(row.get("candidatestatus")).isNull();
	}

	@Test
	void addRefusesATakenId() {
		assertThat(dao.addCandidate(candidate("C1", "Dana", "Doe", 2, 1))).isFalse();

		assertThat(jdbcTemplate.queryForObject("select count(*) from candidate", Integer.class)).isEqualTo(2);
		assertThat(dao.findCandidateById("C1").getFirstname()).isEqualTo("Carla");
	}

	@Test
	void updateChangesNamesAndChoicesButNotTheStatus() {
		dao.updateCandidate(candidate("C2", " Cody ", " Smith ", 1, 1));

		CandidateInfo candidate = dao.findCandidateById("C2");
		assertThat(candidate.getFirstname()).isEqualTo("Cody");
		assertThat(candidate.getLastname()).isEqualTo("Smith");
		assertThat(candidate.getPositionkey()).isEqualTo(1);
		assertThat(candidate.getLanguagekey()).isEqualTo(1);
		assertThat(candidate.getCandidatestatus()).isEqualTo("R");
		assertThat(dao.findCandidateById("C1").getLastname()).isEqualTo("Candidate");
	}

	private static CandidateInfo candidate(String id, String first, String last, int positionkey, int languagekey) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid(id);
		candidate.setFirstname(first);
		candidate.setLastname(last);
		candidate.setPositionkey(positionkey);
		candidate.setLanguagekey(languagekey);
		return candidate;
	}
}
