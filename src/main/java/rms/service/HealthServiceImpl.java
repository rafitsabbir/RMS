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
 * a host that can't be resolved) would otherwise hold the request for minutes, as the JDBC query timeout can't cancel
 * a query on a connection that doesn't answer. While one check still runs, the next request waits on the same one, so
 * a hung database ties up one thread, not one per request.
 */
@Service
public class HealthServiceImpl implements HealthService, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(HealthServiceImpl.class);

	/** Milliseconds a check may take; the container's HEALTHCHECK gives up after 5 seconds. */
	long timeoutMillis = 4000;

	private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "rms-health-check");
		thread.setDaemon(true);
		return thread;
	});

	private Future<?> running;

	HealthDao healthdao;

	@Autowired
	public void setHealthDao(HealthDao healthdao) {
		this.healthdao = healthdao;
	}

	@Override
	public synchronized boolean isDatabaseUp() {
		if (running == null || running.isDone()) {
			running = executor.submit(() -> healthdao.ping());
		}
		try {
			running.get(timeoutMillis, TimeUnit.MILLISECONDS);
			return true;
		} catch (TimeoutException e) {
			log.warn("Health check: the database didn't answer within {} ms", timeoutMillis);
			return false;
		} catch (ExecutionException e) {
			// The cause names the problem (refused, unknown host, access denied) but never the password
			log.warn("Health check: the database doesn't answer: {}", e.getCause().toString());
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/** Stops the check's thread when the application stops. */
	@Override
	public void destroy() {
		executor.shutdownNow();
	}
}
