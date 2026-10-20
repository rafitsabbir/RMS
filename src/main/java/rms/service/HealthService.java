package rms.service;

/** The application's health for monitoring (GET /health). */
public interface HealthService {

	/** True when the database answers; false (logged at WARN) when it doesn't. */
	public boolean isDatabaseUp();
}
