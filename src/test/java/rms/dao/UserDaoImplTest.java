package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import rms.model.UserInfo;

/** Users and Roles against db/schema.sql + db/test-seed.sql (U1-U5, one per role plus an inactive one). */
class UserDaoImplTest extends MySqlContainerSupport {

	UserDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new UserDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void listReturnsEveryUserActiveOrNot() {
		assertThat(dao.getAllUsers())
				.extracting(UserInfo::getUserid, UserInfo::getUsername, UserInfo::getRole, UserInfo::getIsactive)
				.containsExactly(
						tuple("U1", "test.admin", "SUPER_ADMIN", 1),
						tuple("U2", "test.interviewer", "INTERVIEWER", 1),
						tuple("U3", "test.hr", "HR", 1),
						tuple("U4", "test.manager", "HIRING_MANAGER", 1),
						tuple("U5", "test.former", "INTERVIEWER", 0));
	}

	@Test
	void listKeepsAProfileWithoutLoginAndSortsIdsAsNumbers() {
		jdbcTemplate.update("insert into admin (userid, isactive, firstname, lastname, isinterviewer) "
				+ "values ('U10', 1, 'No', 'Login', 'Y')");
		jdbcTemplate.update("insert into admin (userid, isactive, firstname, lastname, isinterviewer) "
				+ "values ('U6', 1, 'Legacy', 'Row', 'N')");

		assertThat(dao.getAllUsers()).extracting(UserInfo::getUserid)
				.containsExactly("U1", "U2", "U3", "U4", "U5", "U6", "U10");
		UserInfo nologin = dao.findUserById("U10");
		assertThat(nologin.getUsername()).isNull();
		assertThat(nologin.getMustchangepassword()).isEqualTo(0);
		// role NULL: the isinterviewer fallback
		assertThat(nologin.getRole()).isEqualTo("INTERVIEWER");
		assertThat(dao.findUserById("U6").getRole()).isEqualTo("SUPER_ADMIN");
	}

	@Test
	void findReturnsInactiveUsersAndNullForUnknown() {
		UserInfo former = dao.findUserById("U5");

		assertThat(former.getIsactive()).isEqualTo(0);
		assertThat(former.getFirstname()).isEqualTo("Fay");
		assertThat(former.getEmail()).isEqualTo("former@example.test");
		assertThat(former.getDesignation()).isEqualTo("Engineer");
		assertThat(dao.findUserById("U9")).isNull();
	}

	@Test
	void addWritesBothRowsWithTheNextIdAndAForcedChange() {
		String userid = dao.addUser(user("new.user", "HR"), "{bcrypt}hash-placeholder");

		assertThat(userid).isEqualTo("U6");
		Map<String, Object> login = jdbcTemplate.queryForMap("select * from users where userid='U6'");
		assertThat(login.get("username")).isEqualTo("new.user");
		assertThat(login.get("password")).isEqualTo("{bcrypt}hash-placeholder");
		assertThat(login.get("mustchangepassword")).isEqualTo(1);
		Map<String, Object> profile = jdbcTemplate.queryForMap("select * from admin where userid='U6'");
		assertThat(profile.get("username")).isEqualTo("new.user");
		assertThat(profile.get("isactive")).isEqualTo(1);
		assertThat(profile.get("firstname")).isEqualTo("Nina");
		assertThat(profile.get("lastname")).isEqualTo("New");
		assertThat(profile.get("email")).isEqualTo("nina@example.test");
		assertThat(profile.get("role")).isEqualTo("HR");
		// For a rollback to a WAR from before the roles (rms.model.Role)
		assertThat(profile.get("isinterviewer")).isEqualTo("Y");
	}

	@Test
	void superAdminIsWrittenAsIsinterviewerN() {
		dao.addUser(user("new.admin", "SUPER_ADMIN"), "{bcrypt}x");

		assertThat(jdbcTemplate.queryForObject("select isinterviewer from admin where userid='U6'", String.class))
				.isEqualTo("N");
	}

	@Test
	void addContinuesAfterTheHighestIdInUsersAdminAndMarks() {
		jdbcTemplate.update("insert into marks (isactive, interviewerid, candidateid) values (1, 'U20', 'C1')");
		assertThat(dao.addUser(user("first.user", "INTERVIEWER"), "{bcrypt}x")).isEqualTo("U21");

		jdbcTemplate.update("insert into admin (userid, isactive, firstname, lastname) values ('U30', 1, 'A', 'B')");
		assertThat(dao.addUser(user("second.user", "INTERVIEWER"), "{bcrypt}x")).isEqualTo("U31");

		jdbcTemplate.update("insert into users (userid, username, password) values ('U40', 'orphan', 'x')");
		assertThat(dao.addUser(user("third.user", "INTERVIEWER"), "{bcrypt}x")).isEqualTo("U41");
	}

	@Test
	void addSkipsIdsOfOtherFormats() {
		int n = 0;
		for (String id : new String[] { "X99", "U12A", "U-50", "U1234567890", "ADMIN7" }) {
			jdbcTemplate.update("insert into users (userid, username, password) values (?, ?, 'x')", id, "other" + n++);
		}

		assertThat(dao.addUser(user("new.user", "HR"), "{bcrypt}x")).isEqualTo("U6");
	}

	@Test
	void addRefusesATakenUsernameInAnyCase() {
		// The column's collation decides; MySQL's defaults ignore case, as the login does
		assertThat(dao.addUser(user("TEST.ADMIN", "HR"), "{bcrypt}x")).isNull();
		// A deactivated user's name stays taken
		assertThat(dao.addUser(user("test.former", "HR"), "{bcrypt}x")).isNull();

		assertThat(jdbcTemplate.queryForObject("select count(*) from users", Integer.class)).isEqualTo(5);
		assertThat(jdbcTemplate.queryForObject("select count(*) from admin", Integer.class)).isEqualTo(5);
	}

	@Test
	void addIsAllOrNothing() {
		// The admin insert fails (first name longer than the column, under MySQL's strict mode): no users row is left
		UserInfo tooLong = user("new.user", "HR");
		tooLong.setFirstname("x".repeat(101));

		assertThatThrownBy(() -> dao.addUser(tooLong, "{bcrypt}x")).isInstanceOf(DataAccessException.class);

		assertThat(jdbcTemplate.queryForObject("select count(*) from users", Integer.class)).isEqualTo(5);
		assertThat(jdbcTemplate.queryForObject("select count(*) from admin", Integer.class)).isEqualTo(5);
	}

	@Test
	void updateChangesProfileAndRoleAndKeepsIsinterviewerInStep() {
		UserInfo changed = user("ignored", "SUPER_ADMIN");
		changed.setUserid("U2");
		changed.setPhone(null);

		dao.updateUser(changed);

		Map<String, Object> profile = jdbcTemplate.queryForMap("select * from admin where userid='U2'");
		assertThat(profile.get("firstname")).isEqualTo("Nina");
		assertThat(profile.get("phone")).isNull();
		assertThat(profile.get("role")).isEqualTo("SUPER_ADMIN");
		assertThat(profile.get("isinterviewer")).isEqualTo("N");
		// Not changed here
		assertThat(profile.get("username")).isEqualTo("test.interviewer");
		assertThat(profile.get("isactive")).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("select username from users where userid='U2'", String.class))
				.isEqualTo("test.interviewer");

		changed.setRole("HIRING_MANAGER");
		dao.updateUser(changed);
		assertThat(jdbcTemplate.queryForObject("select isinterviewer from admin where userid='U2'", String.class))
				.isEqualTo("Y");
	}

	@Test
	void deactivateAndReactivateKeepTheRowsAndHistory() {
		dao.setActive("U2", false);

		assertThat(dao.findUserById("U2").getIsactive()).isEqualTo(0);
		assertThat(jdbcTemplate.queryForObject("select count(*) from users where userid='U2'", Integer.class))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("select count(*) from marks where interviewerid='U2'", Integer.class))
				.isEqualTo(2);

		dao.setActive("U2", true);
		dao.setActive("U5", true);
		assertThat(dao.findUserById("U2").getIsactive()).isEqualTo(1);
		assertThat(dao.findUserById("U5").getIsactive()).isEqualTo(1);
	}

	@Test
	void setPasswordStoresTheHashAndTheForcedChange() {
		dao.setPassword("U2", "{bcrypt}reset-hash", true);

		Map<String, Object> login = jdbcTemplate.queryForMap("select * from users where userid='U2'");
		assertThat(login.get("password")).isEqualTo("{bcrypt}reset-hash");
		assertThat(login.get("mustchangepassword")).isEqualTo(1);

		dao.setPassword("U2", "{bcrypt}own-hash", false);
		assertThat(jdbcTemplate.queryForObject("select mustchangepassword from users where userid='U2'", Integer.class))
				.isEqualTo(0);
		// Nobody else's
		assertThat(dao.findPassword("U1")).isEqualTo("test-only-1");
	}

	@Test
	void findPasswordReturnsTheStoredValueOrNull() {
		assertThat(dao.findPassword("U1")).isEqualTo("test-only-1");
		assertThat(dao.findPassword("U3")).startsWith("{bcrypt}$2a$10$");
		assertThat(dao.findPassword("U9")).isNull();
	}

	@Test
	void countsActiveSuperAdminsIncludingTheFallback() {
		assertThat(dao.countActiveSuperAdmins()).isEqualTo(1);

		// role NULL with isinterviewer n (any case) counts; an inactive one doesn't
		jdbcTemplate.update("insert into admin (userid, isactive, isinterviewer) values ('U7', 1, 'n')");
		jdbcTemplate.update("insert into admin (userid, isactive, isinterviewer, role) values ('U8', 0, 'N', 'SUPER_ADMIN')");
		// A stored role wins over isinterviewer
		jdbcTemplate.update("insert into admin (userid, isactive, isinterviewer, role) values ('U9', 1, 'N', 'HR')");

		assertThat(dao.countActiveSuperAdmins()).isEqualTo(2);
	}

	private static UserInfo user(String username, String role) {
		UserInfo user = new UserInfo();
		user.setUsername(username);
		user.setFirstname("Nina");
		user.setLastname("New");
		user.setEmail("nina@example.test");
		user.setPhone("000-0006");
		user.setDesignation("Recruiter");
		user.setRole(role);
		return user;
	}
}
