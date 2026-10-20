package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import rms.dao.HealthDao;

/**
 * Up when the ping answers; down when it fails, or within the time limit when it doesn't answer at all, also for
 * callers at the same moment; a result is reused for a few seconds.
 */
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
	void aPingThatAnswersIsUpAndTheResultIsReused() {
		assertThat(service.isDatabaseUp()).isTrue();
		assertThat(service.isDatabaseUp()).isTrue();

		verify(healthdao, times(1)).ping();
	}

	@Test
	void aPingThatFailsIsDownAndIsTriedAgainAfterTheReuseTime() {
		doThrow(new DataAccessResourceFailureException("Communications link failure")).when(healthdao).ping();
		service.reuseMillis = 0;

		assertThat(service.isDatabaseUp()).isFalse();
		assertThat(service.isDatabaseUp()).isFalse();
		verify(healthdao, times(2)).ping();
	}

	@Test
	void callersAtTheSameMomentAllAnswerWithinTheLimitAndShareOnePing() throws Exception {
		CountDownLatch release = new CountDownLatch(1);
		doAnswer(invocation -> {
			release.await();
			return null;
		}).when(healthdao).ping();
		service.timeoutMillis = 500;

		long start = System.nanoTime();
		CompletableFuture<Boolean> first = CompletableFuture.supplyAsync(service::isDatabaseUp);
		CompletableFuture<Boolean> second = CompletableFuture.supplyAsync(service::isDatabaseUp);
		CompletableFuture<Boolean> third = CompletableFuture.supplyAsync(service::isDatabaseUp);

		assertThat(first.get(5, TimeUnit.SECONDS)).isFalse();
		assertThat(second.get(5, TimeUnit.SECONDS)).isFalse();
		assertThat(third.get(5, TimeUnit.SECONDS)).isFalse();
		// Waiting happens outside the lock: about one time limit for all, not one per caller
		assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(1400);
		verify(healthdao, times(1)).ping();

		release.countDown();
	}
}
