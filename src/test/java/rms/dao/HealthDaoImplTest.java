package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The health check's ping against the test MySQL, with its query timeout. */
class HealthDaoImplTest extends MySqlContainerSupport {

	HealthDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new HealthDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void pingAnswers() {
		assertThatCode(dao::ping).doesNotThrowAnyException();
		assertThat(dao.jdbcTemplate().getQueryTimeout()).isEqualTo(HealthDaoImpl.QUERY_TIMEOUT);
	}
}
