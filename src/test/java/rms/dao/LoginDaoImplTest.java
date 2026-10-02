package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.LoginInfo;
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
	void findLoginsReturnsUseridAndStoredPassword() {
		List<LoginInfo> logins = dao.findLogins("test.admin");

		assertThat(logins).hasSize(1);
		assertThat(logins.get(0).getUserid()).isEqualTo("U1");
		assertThat(logins.get(0).getUsername()).isEqualTo("test.admin");
		// Plain text today (G13); Spring Security compares it in Java
		assertThat(logins.get(0).getPassword()).isEqualTo("test-only-1");
	}

	@Test
	void findLoginsReturnsNothingForUnknownUser() {
		assertThat(dao.findLogins("nobody")).isEmpty();
	}

	@Test
	void findLoginsReturnsEveryRowOfADuplicateUsername() {
		// characterizes G22: the inferred schema has no unique index on users.username
		jdbcTemplate.update("insert into users (userid, username, password) values ('U8', 'test.admin', 'test-only-8')");

		assertThat(dao.findLogins("test.admin")).extracting(LoginInfo::getUserid).containsExactlyInAnyOrder("U1", "U8");
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
		assertThat(user.getRole()).isEqualTo("INTERVIEWER");
		assertThat(user.getMustchangepassword()).isEqualTo(0);
	}

	@Test
	void getUserInfoReadsEachRole() {
		assertThat(dao.getUserInfo("U1").getRole()).isEqualTo("SUPER_ADMIN");
		assertThat(dao.getUserInfo("U3").getRole()).isEqualTo("HR");
		assertThat(dao.getUserInfo("U4").getRole()).isEqualTo("HIRING_MANAGER");
		// The inactive seed user still has a profile; SecurityConfig refuses the login
		assertThat(dao.getUserInfo("U5").getIsactive()).isEqualTo(0);
	}

	@Test
	void nullRoleFallsBackToIsinterviewer() {
		// Rows that migration 002 left without a role, or rows added outside RMS
		jdbcTemplate.update("update admin set role=null where userid in ('U1', 'U2', 'U3')");
		jdbcTemplate.update("update admin set isinterviewer=null where userid='U3'");

		assertThat(dao.getUserInfo("U1").getRole()).isEqualTo("SUPER_ADMIN");
		assertThat(dao.getUserInfo("U2").getRole()).isEqualTo("INTERVIEWER");
		assertThat(dao.getUserInfo("U3").getRole()).isNull();
	}

	@Test
	void unknownRoleIsNoRole() {
		jdbcTemplate.update("update admin set role='ADMIN' where userid='U1'");

		assertThat(dao.getUserInfo("U1").getRole()).isNull();
	}

	@Test
	void getUserInfoReadsTheForcedPasswordChange() {
		jdbcTemplate.update("update users set mustchangepassword=1 where userid='U2'");

		assertThat(dao.getUserInfo("U2").getMustchangepassword()).isEqualTo(1);
	}

	@Test
	void adminRowWithoutUsersRowHasNoForcedChange() {
		jdbcTemplate.update("insert into admin (userid, username, isactive, firstname, lastname, isinterviewer, role) "
				+ "values ('U9', 'nologin', 1, 'No', 'Login', 'Y', 'INTERVIEWER')");

		assertThat(dao.getUserInfo("U9").getMustchangepassword()).isEqualTo(0);
		assertThat(dao.findLogins("nologin")).isEmpty();
	}

	@Test
	void bcryptRowsAreReadAsStored() {
		assertThat(dao.findLogins("test.hr").get(0).getPassword()).startsWith("{bcrypt}$2a$10$").hasSize(68);
	}

	@Test
	void getUserInfoReturnsNullWhenAdminRowIsMissing() {
		jdbcTemplate.update("insert into users (userid, username, password) values ('U9', 'orphan', 'test-only-9')");

		assertThat(dao.findLogins("orphan")).extracting(LoginInfo::getUserid).containsExactly("U9");
		assertThat(dao.getUserInfo("U9")).isNull();
	}

}
