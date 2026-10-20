package rms.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HealthDaoImpl implements HealthDao {

	/** Seconds the query may take before the database counts as down; the container's check gives up after 5. */
	static final int QUERY_TIMEOUT = 3;

	private String ping = "select 1";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	// Its own template with a query timeout, on the same DataSource, so the shared one keeps its settings
	private volatile JdbcTemplate jdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	JdbcTemplate jdbcTemplate() {
		if (jdbcTemplate == null) {
			JdbcTemplate timed = new JdbcTemplate(namedParameterJdbcTemplate.getJdbcTemplate().getDataSource());
			timed.setQueryTimeout(QUERY_TIMEOUT);
			jdbcTemplate = timed;
		}
		return jdbcTemplate;
	}

	@Override
	public void ping() {
		jdbcTemplate().queryForObject(ping, Integer.class);
	}
}
