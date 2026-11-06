package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.dao.DocumentDao;
import rms.dao.RetentionDao;
import rms.model.DocumentInfo;
import rms.model.RetentionInfo;

/** F11: off by default; only due candidates are cleaned up, documents first, and a failed purge skips one. */
class RetentionServiceImplTest {

	RetentionDao retentiondao = mock(RetentionDao.class);
	DocumentDao documentdao = mock(DocumentDao.class);
	DocumentService documentservice = mock(DocumentService.class);
	ActivityService activityservice = mock(ActivityService.class);

	RetentionServiceImpl service;

	@BeforeEach
	void setUp() {
		service = wire(new RetentionServiceImpl("24"));
		lenient().when(retentiondao.getDue(any(LocalDate.class))).thenReturn(List.of(due("C2"), due("C5"), due("C6")));
		lenient().when(retentiondao.anonymize(anyString(), anyString())).thenReturn(true);
	}

	@Test
	void theSettingIsAWholeNumberOfMonthsOrOff() {
		assertThat(RetentionServiceImpl.months(null)).isZero();
		assertThat(RetentionServiceImpl.months(" ")).isZero();
		assertThat(RetentionServiceImpl.months("abc")).isZero();
		assertThat(RetentionServiceImpl.months("0")).isZero();
		assertThat(RetentionServiceImpl.months("241")).isZero();
		assertThat(RetentionServiceImpl.months(" 24 ")).isEqualTo(24);
		assertThat(service.getMonths()).isEqualTo(24);
		assertThat(service.cutoff()).isEqualTo(LocalDate.now().minusMonths(24));
	}

	@Test
	void offMeansNothingIsDueAndNothingIsCleanedUp() {
		RetentionServiceImpl off = wire(new RetentionServiceImpl(null));

		assertThat(off.getDue()).isEmpty();
		assertThat(off.cleanUp(List.of("C2"), "U1")).isEqualTo(Outcome.refused("Data retention is off."));
		verifyNoInteractions(retentiondao, documentservice);
	}

	@Test
	void dueCandidatesAreCleanedUpWithTheirDocumentsFirst() {
		when(documentdao.getUnpurgedDocuments("C2")).thenReturn(List.of(new DocumentInfo()));
		when(documentservice.purge("C2", "Retention period ended", "U1")).thenReturn(Outcome.ok("1 file deleted."));

		assertThat(service.cleanUp(List.of("C2", "C5"), "U1")).isEqualTo(Outcome.ok("2 candidates' personal data removed."));

		verify(retentiondao).anonymize("C2", "U1");
		verify(retentiondao).anonymize("C5", "U1");
		// No files: no purge (it would refuse without a document folder)
		verify(documentservice, never()).purge(eq("C5"), any(), any());
		verify(activityservice).record("U1", "CANDIDATE_ANONYMIZED", "CANDIDATE", "C2", "24 months");
	}

	@Test
	void notDueOrFilesKeptMeansSkipped() {
		when(documentdao.getUnpurgedDocuments("C6")).thenReturn(List.of(new DocumentInfo()));
		when(documentservice.purge(any(), any(), any())).thenReturn(Outcome.refused("couldn't be removed"));

		assertThat(service.cleanUp(List.of("C2", "C6", "C9"), "U1").message())
				.isEqualTo("1 candidate's personal data removed. 2 skipped: no longer due, or documents couldn't be deleted.");
		verify(retentiondao, never()).anonymize(eq("C6"), any());
		verify(retentiondao, never()).anonymize(eq("C9"), any());

		assertThat(service.cleanUp(List.of("C9"), "U1").done()).isFalse();
		assertThat(service.cleanUp(List.of(), "U1").message()).isEqualTo("Please choose the candidates to clean up.");
	}

	private RetentionServiceImpl wire(RetentionServiceImpl retention) {
		retention.setRetentionDao(retentiondao);
		retention.setDocumentDao(documentdao);
		retention.setDocumentService(documentservice);
		retention.setActivityService(activityservice);
		return retention;
	}

	private static RetentionInfo due(String candidateid) {
		RetentionInfo info = new RetentionInfo();
		info.setCandidateid(candidateid);
		return info;
	}
}
