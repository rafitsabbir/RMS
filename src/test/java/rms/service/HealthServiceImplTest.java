package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import rms.dao.HealthDao;

/** Up when the ping answers; down when it fails, and down within the time limit when it doesn't answer at all. */
@ExtendWith(MockitoExtension.class)
class HealthServiceImplTest {

	@Mock
	HealthDao healthdao;

	@InjectMocks
	HealthServiceImpl service;

	@AfterEach
	void stop() {
		service.destroy();
	}

	@Test
	void aPingThatAnswersIsUp() {
		assertThat(service.isDatabaseUp()).isTrue();
	}

	@Test
	void aPingThatFailsIsDown() {
		doThrow(new DataAccessResourceFailureException("Communications link failure")).when(healthdao).ping();

		assertThat(service.isDatabaseUp()).isFalse();
	}

	@Test
	void aPingThatHangsIsDownWithinTheLimitAndIsNotStartedTwice() throws Exception {
		CountDownLatch release = new CountDownLatch(1);
		doAnswer(invocation -> {
			release.await();
			return null;
		}).when(healthdao).ping();
		service.timeoutMillis = 200;

		long start = System.nanoTime();
		assertThat(service.isDatabaseUp()).isFalse();
		assertThat(service.isDatabaseUp()).isFalse();
		assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(2000);
		// The second request waited on the same hung ping instead of starting another
		verify(healthdao, times(1)).ping();

		release.countDown();
	}
}
