package rms.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.HealthDao;

/**
 * The database check runs on its own thread with a time limit: a database that doesn't answer at all (frozen, or
 * a host that can't be resolved) would otherwise hold the request for minutes. Every caller waits on the same check,
 * outside the lock, so a hung database ties up one check thread and each caller for at most the time limit. /health
 * is public: a result is reused for a few seconds, so callers can't make RMS query the database on every request, and
 * only a change (up to down, down to up) is logged.
 */
@Service
public class HealthServiceImpl implements HealthService, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(HealthServiceImpl.class);

	/** Milliseconds a check may take; the container's HEALTHCHECK gives up after 5 seconds. */
	long timeoutMillis = 4000;

	/** Milliseconds a result is reused. */
	long reuseMillis = 5000;

	private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "rms-health-check");
		thread.setDaemon(true);
		return thread;
	});

	private Future<?> running;
	private boolean checked;
	private long checkedAt;
	private boolean lastUp = true;

	HealthDao healthdao;

	@Autowired
	public void setHealthDao(HealthDao healthdao) {
		this.healthdao = healthdao;
	}

	@Override
	public boolean isDatabaseUp() {
		Future<?> check;
		synchronized (this) {
			if (checked && System.nanoTime() - checkedAt < TimeUnit.MILLISECONDS.toNanos(reuseMillis)) {
				return lastUp;
			}
			if (running == null || running.isDone()) {
				running = executor.submit(() -> healthdao.ping());
			}
			check = running;
		}
		try {
			check.get(timeoutMillis, TimeUnit.MILLISECONDS);
			record(true, null);
			return true;
		} catch (TimeoutException e) {
			record(false, "no answer within " + timeoutMillis + " ms");
			return false;
		} catch (ExecutionException e) {
			// The exception's type only: its messages can name the database user and host
			record(false, e.getCause().getClass().getSimpleName());
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private synchronized void record(boolean up, String why) {
		if (up && !lastUp) {
			log.info("Health check: the database answers again");
		} else if (!up && lastUp) {
			log.warn("Health check: the database doesn't answer ({})", why);
		} else if (!up) {
			log.debug("Health check: the database still doesn't answer ({})", why);
		}
		lastUp = up;
		checked = true;
		checkedAt = System.nanoTime();
	}

	/** Stops the check's thread when the application stops. */
	@Override
	public void destroy() {
		executor.shutdownNow();
	}
}
