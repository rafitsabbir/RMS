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
	void addGeneratesTheNextIdAndInsertsTrimmedValuesWithNoStatus() {
		assertThat(dao.addCandidate(candidate(null, " Dana ", " Doe ", 2, 1))).isEqualTo("C3");

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from candidate where candidateid='C3'");
		assertThat(row.get("firstname")).isEqualTo("Dana");
		assertThat(row.get("lastname")).isEqualTo("Doe");
		assertThat(row.get("positionkey")).isEqualTo(2);
		assertThat(row.get("languagekey")).isEqualTo(1);
		// G9: nothing sets the status yet
		assertThat(row.get("candidatestatus")).isNull();
		assertThat(dao.addCandidate(candidate(null, "Eve", "Doe", 1, 1))).isEqualTo("C4");
	}

	@Test
	void addIgnoresAGivenId() {
		assertThat(dao.addCandidate(candidate("C1", "Dana", "Doe", 2, 1))).isEqualTo("C3");

		assertThat(dao.findCandidateById("C1").getFirstname()).isEqualTo("Carla");
	}

	@Test
	void addContinuesFromTheHighestNumberNotTheLatestRow() {
		jdbcTemplate.update("insert into candidate (candidateid, firstname, lastname) values ('C10', 'X', 'Y')");
		jdbcTemplate.update("insert into candidate (candidateid, firstname, lastname) values ('C9', 'X', 'Y')");

		assertThat(dao.addCandidate(candidate(null, "Dana", "Doe", 1, 1))).isEqualTo("C11");
		// Shorter IDs first, so the numbers sort as numbers
		assertThat(dao.getAllCandidate()).extracting(CandidateInfo::getCandidateid)
				.containsExactly("C1", "C2", "C9", "C10", "C11");
	}

	@Test
	void addSkipsIdsOfOtherFormats() {
		// IDs that don't look like C<number> (up to 9 digits) don't count
		for (String id : new String[] { "X99", "C12A", "C-50", "C1234567890", "CAND7" }) {
			jdbcTemplate.update("insert into candidate (candidateid, firstname, lastname) values (?, 'X', 'Y')", id);
		}

		assertThat(dao.addCandidate(candidate(null, "Dana", "Doe", 1, 1))).isEqualTo("C3");
	}

	@Test
	void addStartsAtC1OnAnEmptyTable() {
		jdbcTemplate.update("delete from marks");
		jdbcTemplate.update("delete from candidate");

		assertThat(dao.addCandidate(candidate(null, "Dana", "Doe", 1, 1))).isEqualTo("C1");
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
