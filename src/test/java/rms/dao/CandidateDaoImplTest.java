package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
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

	@Test
	void deleteHidesTheCandidateButKeepsTheRow() {
		dao.deleteCandidate("C2");

		assertThat(dao.getAllCandidate()).extracting(CandidateInfo::getCandidateid).containsExactly("C1");
		assertThat(dao.findCandidateById("C2")).isNull();
		assertThat(jdbcTemplate.queryForObject("select isactive from candidate where candidateid='C2'", Integer.class))
				.isEqualTo(0);
		assertThat(jdbcTemplate.queryForObject("select count(*) from marks where candidateid='C2'", Integer.class))
				.isEqualTo(1);
	}

	@Test
	void deletedIdsAreNotReused() {
		dao.addCandidate(candidate(null, "Dana", "Doe", 1, 1));
		dao.deleteCandidate("C3");

		assertThat(dao.addCandidate(candidate(null, "Eve", "Doe", 1, 1))).isEqualTo("C4");
	}

	@Test
	void updateLeavesADeletedCandidate() {
		dao.deleteCandidate("C2");

		dao.updateCandidate(candidate("C2", "New", "Name", 1, 1));

		assertThat(jdbcTemplate.queryForObject("select firstname from candidate where candidateid='C2'", String.class))
				.isEqualTo("Cody");
	}

	@Test
	void newCandidatesAreActive() {
		dao.addCandidate(candidate(null, "Dana", "Doe", 1, 1));

		assertThat(jdbcTemplate.queryForObject("select isactive from candidate where candidateid='C3'", Integer.class))
				.isEqualTo(1);
	}

	// --- Phase 2: contact details, source, applied date, job, document counts ---

	@Test
	void profileFieldsJobAndDocumentCountsAreRead() {
		CandidateInfo carla = dao.findCandidateById("C1");

		assertThat(carla.getEmail()).isEqualTo("carla@example.test");
		assertThat(carla.getPhone()).isEqualTo("000-1001");
		assertThat(carla.getSource()).isEqualTo("REFERRAL");
		assertThat(carla.getApplieddate()).isEqualTo(LocalDate.of(2026, 9, 1));
		assertThat(carla.getJobkey()).isEqualTo(1);
		assertThat(carla.getJobstatus()).isEqualTo("OPEN");
		// CV and SSC active (the replaced CV doesn't count), one professional certificate
		assertThat(carla.getCvcount()).isEqualTo(1);
		assertThat(carla.getSlotcount()).isEqualTo(2);
		assertThat(carla.getProfessionalcount()).isEqualTo(1);

		CandidateInfo cody = dao.findCandidateById("C2");
		assertThat(cody.getEmail()).isNull();
		assertThat(cody.getJobkey()).isZero();
		assertThat(cody.getJobstatus()).isNull();
		assertThat(cody.getCvcount()).isZero();
		assertThat(cody.getSlotcount()).isZero();
	}

	@Test
	void aDeletedJobHasNoStatusAndTheLinkStays() {
		jdbcTemplate.update("update job set isactive=0 where jobkey=1");

		CandidateInfo carla = dao.findCandidateById("C1");
		assertThat(carla.getJobkey()).isEqualTo(1);
		assertThat(carla.getJobstatus()).isNull();
	}

	@Test
	void documentCountsFollowDeletesAndKinds() {
		jdbcTemplate.update("update candidate_document set isactive=0 where documentkey=1");
		jdbcTemplate.update("insert into candidate_document (candidateid, doctype, originalname, storedname, "
				+ "contenttype, filesize, uploadedby, uploadedat) values ('C1', 'SSC', 'x.pdf', "
				+ "'b0000000000000000000000000000001', 'application/pdf', 1, 'U1', now())");

		CandidateInfo carla = dao.getAllCandidate().get(0);
		assertThat(carla.isHascv()).isFalse();
		// Two active SSC rows still count as one kind
		assertThat(carla.getSlotcount()).isEqualTo(1);
	}

	@Test
	void addAndUpdateSaveTheNewFields() {
		CandidateInfo dana = candidate(null, "Dana", "Doe", 1, 1);
		dana.setEmail(" dana@example.test ");
		dana.setPhone("000-2001");
		dana.setSource("AGENCY");
		dana.setApplieddate(LocalDate.of(2026, 9, 20));
		dana.setJobkey(1);
		String id = dao.addCandidate(dana);

		CandidateInfo saved = dao.findCandidateById(id);
		assertThat(saved.getEmail()).isEqualTo("dana@example.test");
		assertThat(saved.getSource()).isEqualTo("AGENCY");
		assertThat(saved.getApplieddate()).isEqualTo(LocalDate.of(2026, 9, 20));
		assertThat(saved.getJobkey()).isEqualTo(1);

		saved.setEmail("");
		saved.setPhone(" ");
		saved.setSource("");
		saved.setApplieddate(null);
		saved.setJobkey(0);
		dao.updateCandidate(saved);

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from candidate where candidateid=?", id);
		assertThat(row.get("email")).isNull();
		assertThat(row.get("phone")).isNull();
		assertThat(row.get("source")).isNull();
		assertThat(row.get("applieddate")).isNull();
		assertThat(row.get("jobkey")).isNull();
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

	@Test
	void deletedCandidatesListWithTheirStoredFiles() {
		assertThat(dao.getDeletedCandidates()).isEmpty();

		int stored = jdbcTemplate.queryForObject("select count(*) from candidate_document where candidateid='C1' and purgedat is null", Integer.class);
		// C1 has documents in the seed (one of them replaced, its file still stored); C2 has none
		jdbcTemplate.update("update candidate set isactive=0");
		assertThat(dao.getDeletedCandidates()).extracting("candidateid", "unpurgedcount")
				.containsExactly(tuple("C1", stored), tuple("C2", 0));

		// A purged file no longer counts
		jdbcTemplate.update("update candidate_document set purgedat=now() where documentkey=1");
		assertThat(dao.getDeletedCandidates().get(0).getUnpurgedcount()).isEqualTo(stored - 1);
		// Active candidates never list
		jdbcTemplate.update("update candidate set isactive=1 where candidateid='C1'");
		assertThat(dao.getDeletedCandidates()).extracting("candidateid").containsExactly("C2");
	}
}
