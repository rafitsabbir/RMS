package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import rms.model.UserInfo;

/** Characterization tests against db/schema.sql + db/test-seed.sql. */
class LoginDaoImplTest extends MySqlContainerSupport {

	LoginDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new LoginDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void checkUserReturnsUseridForMatchingCredentials() {
		assertThat(dao.checkUser("test.admin", "test-only-1")).isEqualTo("U1");
	}

	@Test
	void checkUserReturnsNullForWrongPassword() {
		assertThat(dao.checkUser("test.admin", "wrong")).isNull();
	}

	@Test
	void getUserInfoMapsAdminRow() {
		UserInfo user = dao.getUserInfo("U2");

		assertThat(user.getUserid()).isEqualTo("U2");
		assertThat(user.getUsername()).isEqualTo("test.interviewer");
		assertThat(user.getIsactive()).isEqualTo(1);
		assertThat(user.getFirstname()).isEqualTo("Ivan");
		assertThat(user.getLastname()).isEqualTo("Interviewer");
		assertThat(user.getEmail()).isEqualTo("interviewer@example.test");
		assertThat(user.getPhone()).isEqualTo("000-0002");
		assertThat(user.getDesignation()).isEqualTo("Engineer");
		assertThat(user.getIsinterviewer()).isEqualTo("Y");
		assertThat(user.getPassword()).isNull();
	}

	@Test
	void getUserInfoThrowsWhenAdminRowIsMissing() {
		// characterizes G26: a users row without an admin row is not handled
		jdbcTemplate.update("insert into users (userid, username, password) values ('U9', 'orphan', 'test-only-9')");

		assertThat(dao.checkUser("orphan", "test-only-9")).isEqualTo("U9");
		assertThatThrownBy(() -> dao.getUserInfo("U9")).isInstanceOf(EmptyResultDataAccessException.class);
	}

}
