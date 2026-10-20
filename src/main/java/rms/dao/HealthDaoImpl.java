package rms.dao;

import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HealthDaoImpl implements HealthDao {

	/** Seconds the ping may take before the database counts as down; the container's check gives up after 5. */
	static final int TIMEOUT = 3;

	private String ping = "select 1";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * A network timeout on the connection, not only a query timeout: a query timeout can't cancel a query on a
	 * connection that doesn't answer, and the RMS_DB_URL may have no socketTimeout. The connection goes back to the
	 * pool, so its own timeout is restored.
	 */
	@Override
	public void ping() {
		namedParameterJdbcTemplate.getJdbcTemplate().execute((ConnectionCallback<Void>) connection -> {
			int previous = connection.getNetworkTimeout();
			connection.setNetworkTimeout(Runnable::run, (int) TimeUnit.SECONDS.toMillis(TIMEOUT));
			try (Statement statement = connection.createStatement()) {
				statement.setQueryTimeout(TIMEOUT);
				statement.execute(ping);
			} finally {
				connection.setNetworkTimeout(Runnable::run, previous);
			}
			return null;
		});
	}
}
