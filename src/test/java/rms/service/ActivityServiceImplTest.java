package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;

import rms.dao.ActivityDao;
import rms.model.ActivityInfo;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

	@Mock
	ActivityDao activitydao;

	ActivityServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ActivityServiceImpl();
		service.setActivityDao(activitydao);
	}

	@Test
	void anEntryCarriesWhoWhatAndWhich() {
		service.record("U3", "SELECTED", "CANDIDATE", "C1", "detail");

		ArgumentCaptor<ActivityInfo> captor = ArgumentCaptor.forClass(ActivityInfo.class);
		verify(activitydao).add(captor.capture());
		ActivityInfo entry = captor.getValue();
		assertThat(entry.getUserid()).isEqualTo("U3");
		assertThat(entry.getAction()).isEqualTo("SELECTED");
		assertThat(entry.getEntitytype()).isEqualTo("CANDIDATE");
		assertThat(entry.getEntityid()).isEqualTo("C1");
		assertThat(entry.getDetail()).isEqualTo("detail");
	}

	@Test
	void aMissingUserAndALongDetailAreHandled() {
		service.record(null, "MARKED", "CANDIDATE", "C1", "x".repeat(400));

		ArgumentCaptor<ActivityInfo> captor = ArgumentCaptor.forClass(ActivityInfo.class);
		verify(activitydao).add(captor.capture());
		assertThat(captor.getValue().getUserid()).isEmpty();
		assertThat(captor.getValue().getDetail()).hasSize(ActivityServiceImpl.MAX_DETAIL);
	}

	@Test
	void aFailedWriteNeverBreaksTheUsersAction() {
		// For example migration 007 not run yet: the table doesn't exist
		doThrow(new BadSqlGrammarException("log", "insert", new SQLException("no table")))
				.when(activitydao).add(any(ActivityInfo.class));

		service.record("U3", "SELECTED", "CANDIDATE", "C1", null);
	}

	@Test
	void theRecentListCanBeFilteredByAction() {
		service.getRecent(null);
		service.getRecent("  ");
		service.getRecent(" SELECTED ");

		verify(activitydao, org.mockito.Mockito.times(2)).getRecent(eq((String) null), eq(ActivityServiceImpl.MAX_ROWS));
		verify(activitydao).getRecent("SELECTED", ActivityServiceImpl.MAX_ROWS);
	}

	@Test
	void theLabelsAreReadable() {
		ActivityInfo entry = new ActivityInfo();
		entry.setAction("INTERVIEW_SCHEDULED");
		entry.setCreatedat(java.time.LocalDateTime.of(2026, 10, 3, 9, 5, 7));

		assertThat(entry.getActionlabel()).isEqualTo("Interview scheduled");
		assertThat(entry.getCreatedlabel()).isEqualTo("2026-10-03 09:05:07");
		assertThat(new ActivityInfo().getActionlabel()).isEmpty();
		assertThat(new ActivityInfo().getCreatedlabel()).isEmpty();
	}
}
