package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.DocumentInfo;

/**
 * Characterization tests against db/schema.sql + db/test-seed.sql. The seed: C1 has documents 1 (CV), 2 (SSC) and
 * 3 (PROFESSIONAL, PMP) active, and 4 (an old CV, replaced); C2 has none. Metadata only: no files.
 */
class DocumentDaoImplTest extends MySqlContainerSupport {

	DocumentDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new DocumentDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void documentsAreTheActiveOnesOldestFirstWithTheUploadersName() {
		assertThat(dao.getDocuments("C1"))
				.extracting(DocumentInfo::getDocumentkey, DocumentInfo::getDoctype, DocumentInfo::getOriginalname,
						DocumentInfo::getUploadedbyname)
				.containsExactly(
						tuple(1, "CV", "carla-cv.pdf", "Hana Recruiter"),
						tuple(2, "SSC", "ssc.jpg", "Hana Recruiter"),
						tuple(3, "PROFESSIONAL", "pmp.png", "Ada Admin"));
		assertThat(dao.getDocuments("C2")).isEmpty();
	}

	@Test
	void documentRowIsReadInFull() {
		DocumentInfo document = dao.findDocument(3);

		assertThat(document.getCandidateid()).isEqualTo("C1");
		assertThat(document.getStoredname()).isEqualTo("a0000000000000000000000000000003");
		assertThat(document.getContenttype()).isEqualTo("image/png");
		assertThat(document.getFilesize()).isEqualTo(512);
		assertThat(document.getTitle()).isEqualTo("PMP");
		assertThat(document.getIssuer()).isEqualTo("Example Institute");
		assertThat(document.getIssueyear()).isEqualTo(2024);
		assertThat(document.getUploadedby()).isEqualTo("U1");
		assertThat(document.getUploadedlabel()).isEqualTo("2026-09-03 10:10");
	}

	@Test
	void findSkipsInactiveDocumentsAndDeletedCandidates() {
		assertThat(dao.findDocument(4)).isNull();
		assertThat(dao.findDocument(99)).isNull();

		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C1'");
		assertThat(dao.findDocument(1)).isNull();
	}

	@Test
	void uploaderWithoutAnAdminRowShowsNoName() {
		jdbcTemplate.update("update candidate_document set uploadedby='U99' where documentkey=1");

		assertThat(dao.findDocument(1).getUploadedbyname()).isNull();
		assertThat(dao.findDocument(1).getUploadedby()).isEqualTo("U99");
	}

	@Test
	void countIsOfActiveDocumentsOfTheKind() {
		assertThat(dao.countDocuments("C1", "CV")).isEqualTo(1);
		assertThat(dao.countDocuments("C1", "PROFESSIONAL")).isEqualTo(1);
		assertThat(dao.countDocuments("C1", "PHD")).isZero();
		assertThat(dao.countDocuments("C2", "CV")).isZero();
	}

	@Test
	void addSavesAnActiveRowAndReturnsItsKey() {
		int key = dao.addDocument(document("C2", "CV", null), 1);

		assertThat(key).isEqualTo(5);
		Map<String, Object> row = jdbcTemplate.queryForMap("select * from candidate_document where documentkey=5");
		assertThat(row.get("candidateid")).isEqualTo("C2");
		assertThat(row.get("doctype")).isEqualTo("CV");
		assertThat(row.get("originalname")).isEqualTo("new.pdf");
		assertThat(row.get("storedname")).isEqualTo("b0000000000000000000000000000001");
		assertThat(row.get("contenttype")).isEqualTo("application/pdf");
		assertThat(row.get("filesize")).isEqualTo(100);
		assertThat(row.get("uploadedby")).isEqualTo("U3");
		assertThat(row.get("uploadedat")).isNotNull();
		assertThat(row.get("isactive")).isEqualTo(1);
		assertThat(row.get("purgedat")).isNull();
	}

	@Test
	void addingASingleSlotKindAgainReplacesTheActiveOne() {
		int key = dao.addDocument(document("C1", "CV", null), 1);

		assertThat(dao.getDocuments("C1")).extracting(DocumentInfo::getDocumentkey).containsExactly(2, 3, key);
		Map<String, Object> old = jdbcTemplate.queryForMap("select * from candidate_document where documentkey=1");
		assertThat(old.get("isactive")).isEqualTo(0);
		assertThat(old.get("deletedby")).isEqualTo("U3");
		assertThat(old.get("deletedat")).isNotNull();
		// The earlier replaced CV keeps its own record
		assertThat(jdbcTemplate.queryForObject("select deletedby from candidate_document where documentkey=4",
				String.class)).isEqualTo("U3");
		assertThat(dao.countDocuments("C1", "CV")).isEqualTo(1);
	}

	@Test
	void professionalCertificatesStopAtFive() {
		for (int i = 0; i < 4; i++) {
			assertThat(dao.addDocument(document("C1", "PROFESSIONAL", "Cert " + i), 5)).isPositive();
		}

		assertThat(dao.addDocument(document("C1", "PROFESSIONAL", "Sixth"), 5)).isZero();

		assertThat(dao.countDocuments("C1", "PROFESSIONAL")).isEqualTo(5);
		assertThat(jdbcTemplate.queryForObject("select count(*) from candidate_document where title='Sixth'",
				Integer.class)).isZero();
		// Another candidate isn't affected
		assertThat(dao.addDocument(document("C2", "PROFESSIONAL", "PMP"), 5)).isPositive();
	}

	@Test
	void aFailedInsertRollsBackTheReplace() {
		DocumentInfo tooLong = document("C1", "CV", null);
		tooLong.setOriginalname("x".repeat(300));

		// Data too long for originalname (strict mode, the MySQL 8 default)
		assertThatThrownBy(() -> dao.addDocument(tooLong, 1)).isInstanceOf(RuntimeException.class);

		assertThat(jdbcTemplate.queryForObject("select isactive from candidate_document where documentkey=1",
				Integer.class)).isEqualTo(1);
	}

	@Test
	void deleteIsSoftAndOnlyOnce() {
		assertThat(dao.deleteDocument(2, "U1")).isTrue();
		assertThat(dao.deleteDocument(2, "U3")).isFalse();

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from candidate_document where documentkey=2");
		assertThat(row.get("isactive")).isEqualTo(0);
		assertThat(row.get("deletedby")).isEqualTo("U1");
		assertThat(row.get("storedname")).isEqualTo("a0000000000000000000000000000002");
	}

	@Test
	void unpurgedDocumentsIncludeReplacedAndDeletedOnes() {
		dao.deleteDocument(2, "U1");

		assertThat(dao.getUnpurgedDocuments("C1")).extracting(DocumentInfo::getDocumentkey).containsExactly(1, 2, 3, 4);
		assertThat(dao.getUnpurgedDocuments("C2")).isEmpty();
	}

	@Test
	void markPurgedRecordsWhoWhenAndWhyOnce() {
		dao.markPurged(1, "U1", "Retention period ended");
		dao.markPurged(4, "U1", "Retention period ended");
		dao.markPurged(4, "U9", "Again");

		Map<String, Object> active = jdbcTemplate.queryForMap("select * from candidate_document where documentkey=1");
		assertThat(active.get("isactive")).isEqualTo(0);
		assertThat(active.get("deletedby")).isEqualTo("U1");
		assertThat(active.get("purgedby")).isEqualTo("U1");
		assertThat(active.get("purgedat")).isNotNull();
		assertThat(active.get("purgereason")).isEqualTo("Retention period ended");
		Map<String, Object> replaced = jdbcTemplate.queryForMap("select * from candidate_document where documentkey=4");
		// Deleted earlier: keeps who deleted it; purged once
		assertThat(replaced.get("deletedby")).isEqualTo("U3");
		assertThat(replaced.get("purgedby")).isEqualTo("U1");
		assertThat(dao.getUnpurgedDocuments("C1")).extracting(DocumentInfo::getDocumentkey).containsExactly(2, 3);
		assertThat(dao.getDocuments("C1")).extracting(DocumentInfo::getDocumentkey).containsExactly(2, 3);
	}

	private static DocumentInfo document(String candidateid, String doctype, String title) {
		DocumentInfo document = new DocumentInfo();
		document.setCandidateid(candidateid);
		document.setDoctype(doctype);
		document.setOriginalname("new.pdf");
		document.setStoredname("b0000000000000000000000000000001");
		document.setContenttype("application/pdf");
		document.setFilesize(100);
		document.setTitle(title);
		document.setUploadedby("U3");
		return document;
	}
}
