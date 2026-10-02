package rms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** The document kinds, sources and the labels the pages show. */
class DocumentModelTest {

	@Test
	void sixSingleSlotKindsAndFiveProfessionalCertificates() {
		assertThat(DocumentType.values()).filteredOn(DocumentType::isSingleSlot).hasSize(DocumentType.SLOT_TYPES);
		assertThat(DocumentType.PROFESSIONAL.getMaxActive()).isEqualTo(5);
		assertThat(DocumentType.CV.isSingleSlot()).isTrue();
	}

	@Test
	void parseTakesExactNamesOnly() {
		assertThat(DocumentType.parse("PHD")).isEqualTo(DocumentType.PHD);
		assertThat(DocumentType.parse("phd")).isNull();
		assertThat(DocumentType.parse("")).isNull();
		assertThat(DocumentType.parse(null)).isNull();
		assertThat(CandidateSource.parse("WALK_IN")).isEqualTo(CandidateSource.WALK_IN);
		assertThat(CandidateSource.parse("Walk-in")).isNull();
	}

	@Test
	void candidateLabels() {
		CandidateInfo candidate = new CandidateInfo();
		assertThat(candidate.getSourcelabel()).isNull();
		assertThat(candidate.isKnownsource()).isTrue();
		candidate.setSource("JOB_BOARD");
		assertThat(candidate.getSourcelabel()).isEqualTo("Job board");
		candidate.setSource("NEWSPAPER");
		assertThat(candidate.getSourcelabel()).isEqualTo("NEWSPAPER");
		assertThat(candidate.isKnownsource()).isFalse();

		assertThat(candidate.isHascv()).isFalse();
		candidate.setCvcount(1);
		assertThat(candidate.isHascv()).isTrue();
		assertThat(candidate.getSlottypes()).isEqualTo(6);
		// No personal data in logs
		candidate.setEmail("someone@example.test");
		candidate.setFirstname("Carla");
		assertThat(candidate.toString()).doesNotContain("someone").doesNotContain("Carla");
	}

	@Test
	void documentLabels() {
		DocumentInfo document = new DocumentInfo();
		document.setDoctype("BSC");
		document.setFilesize(512);
		assertThat(document.getDoctypelabel()).isEqualTo("BSc / Honours certificate");
		assertThat(document.getSizelabel()).isEqualTo("512 bytes");
		document.setFilesize(2048);
		assertThat(document.getSizelabel()).isEqualTo("2.0 KB");
		document.setFilesize(1536 * 1024);
		assertThat(document.getSizelabel()).isEqualTo("1.5 MB");
		assertThat(document.getUploadedlabel()).isEmpty();
		document.setUploadedat(LocalDateTime.of(2026, 9, 3, 10, 5, 59));
		assertThat(document.getUploadedlabel()).isEqualTo("2026-09-03 10:05");
		document.setDoctype("FUTURE_KIND");
		assertThat(document.getDoctypelabel()).isEqualTo("FUTURE_KIND");
		document.setOriginalname("carla-cv.pdf");
		assertThat(document.toString()).doesNotContain("carla");
	}

	@Test
	void jobLabels() {
		JobInfo job = new JobInfo();
		job.setJobkey(1);
		job.setPositionkey(4);
		assertThat(job.getLabel()).isEqualTo("Job 1: Position 4");
		job.setPositionname("SOFTWARE ENGINEER");
		job.setClosingdate(LocalDate.of(2030, 12, 31));
		assertThat(job.getLabel()).isEqualTo("Job 1: SOFTWARE ENGINEER (closes 2030-12-31)");
		assertThat(job.isOpen()).isTrue();
		job.setStatus(JobInfo.CLOSED);
		assertThat(job.getLabel()).isEqualTo("Job 1: SOFTWARE ENGINEER (closed)");
		assertThat(job.isOpen()).isFalse();
		// A deleted job a candidate still has: no status
		job.setStatus(null);
		assertThat(job.getLabel()).isEqualTo("Job 1 (deleted)");
	}
}
