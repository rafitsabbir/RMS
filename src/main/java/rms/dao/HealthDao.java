package rms.dao;

/** Whether the database answers, for the health check (GET /health). */
public interface HealthDao {

	/** Runs a trivial query; throws a DataAccessException when the database can't be reached. */
	public void ping();
}
