package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import rms.model.Role;
import rms.model.UserInfo;

@Repository
public class UserDaoImpl implements UserDao {
	private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

	// Every admin row once. An admin row without a users row (no login, G26) has a NULL username. Subqueries, not
	// a join, so two users rows for one userid (no known key, G22) don't list the user twice
	private String selectuser = "select a.userid, (select min(u.username) from users u where u.userid=a.userid) "
			+ "as username, a.isactive, a.firstname, a.lastname, a.email, a.phone, a.designation, a.isinterviewer, "
			+ "a.role, (select max(u.mustchangepassword) from users u where u.userid=a.userid) as mustchangepassword "
			+ "from admin a";
	// Shorter IDs first, so U2 comes before U10
	private String allusers = selectuser + " order by length(a.userid), a.userid";
	private String finduserbyid = selectuser + " where a.userid=:userid";
	// Every row with the name: users has no isactive, and a username on two rows can't log in (LoginServiceImpl)
	private String usernamecount = "select count(*) from users where username=:username";
	// The highest number among IDs of the form U<number> (up to 9 digits) in users, admin and marks, so a new user
	// never takes an ID that already has a login, a profile or evaluations; 0 when there is none
	private String lastnumber = "select coalesce(max(n), 0) from ("
			+ "select cast(substring(userid, 2) as unsigned) n from users where userid regexp '^U[0-9]{1,9}$' "
			+ "union all select cast(substring(userid, 2) as unsigned) from admin where userid regexp '^U[0-9]{1,9}$' "
			+ "union all select cast(substring(interviewerid, 2) as unsigned) from marks "
			+ "where interviewerid regexp '^U[0-9]{1,9}$') ids";
	private String savelogin = "insert into users (userid, username, password, mustchangepassword) "
			+ "values (:userid, :username, :password, 1)";
	private String saveprofile = "insert into admin "
			+ "(userid, username, isactive, firstname, lastname, email, phone, designation, isinterviewer, role) "
			+ "values (:userid, :username, 1, :firstname, :lastname, :email, :phone, :designation, :isinterviewer, :role)";
	// isinterviewer follows the role, for a rollback to a WAR from before the roles (Role.getIsinterviewer)
	private String updateuser = "update admin set firstname=:firstname, lastname=:lastname, email=:email, "
			+ "phone=:phone, designation=:designation, isinterviewer=:isinterviewer, role=:role where userid=:userid";
	// Soft: the rows, and the user's evaluations and history, stay
	private String setactive = "update admin set isactive=:isactive where userid=:userid";
	private String setpassword = "update users set password=:password, mustchangepassword=:mustchangepassword "
			+ "where userid=:userid";
	private String passwordbyid = "select password from users where userid=:userid";
	private String activesuperadmins = "select count(*) from admin where isactive=1 "
			+ "and (role='SUPER_ADMIN' or (role is null and upper(isinterviewer)='N'))";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	TransactionTemplate transactionTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.transactionTemplate = null;
	}

	/** addUser's transaction, on the template's DataSource; made on first use (addUser is synchronized). */
	private TransactionTemplate transactions() {
		if (transactionTemplate == null) {
			transactionTemplate = new TransactionTemplate(
					new DataSourceTransactionManager(namedParameterJdbcTemplate.getJdbcTemplate().getDataSource()));
		}
		return transactionTemplate;
	}

	private static final class UserMapper implements RowMapper<UserInfo> {
		public UserInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			UserInfo userinfo = new UserInfo();
			userinfo.setUserid(rs.getString("userid"));
			userinfo.setUsername(rs.getString("username"));
			userinfo.setIsactive(rs.getInt("isactive"));
			userinfo.setFirstname(rs.getString("firstname"));
			userinfo.setLastname(rs.getString("lastname"));
			userinfo.setEmail(rs.getString("email"));
			userinfo.setPhone(rs.getString("phone"));
			userinfo.setDesignation(rs.getString("designation"));
			userinfo.setIsinterviewer(rs.getString("isinterviewer"));
			Role role = Role.of(rs.getString("role"), userinfo.getIsinterviewer());
			userinfo.setRole(role == null ? null : role.name());
			userinfo.setMustchangepassword(rs.getInt("mustchangepassword"));
			return userinfo;
		}
	}

	@Override
	public List<UserInfo> getAllUsers() {
		return namedParameterJdbcTemplate.query(allusers, new HashMap<String, Object>(), new UserMapper());
	}

	@Override
	public UserInfo findUserById(String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("userid", userid);
		// A list, not queryForObject: whether admin.userid is unique in production is unknown (G22)
		List<UserInfo> list = namedParameterJdbcTemplate.query(finduserbyid, paramMap, new UserMapper());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	// synchronized: the username check, reading the highest number and inserting must not interleave with another
	// add. This covers one Tomcat only; on several nodes a clash on the key is retried (when userid is a key, G22).
	// Each attempt is its own transaction, so the next one reads the highest number afresh.
	public synchronized String addUser(UserInfo userinfo, String encodedPassword) {
		Map<String, Object> paramMap = paramMap(userinfo);
		paramMap.put("username", userinfo.getUsername());
		paramMap.put("password", encodedPassword);

		if (namedParameterJdbcTemplate.queryForObject(usernamecount, paramMap, Integer.class) > 0) {
			return null;
		}
		for (int attempt = 1; ; attempt++) {
			int last = namedParameterJdbcTemplate.queryForObject(lastnumber, new HashMap<String, Object>(),
					Integer.class);
			String userid = "U" + (last + 1);
			paramMap.put("userid", userid);
			try {
				transactions().executeWithoutResult(status -> {
					namedParameterJdbcTemplate.update(savelogin, paramMap);
					namedParameterJdbcTemplate.update(saveprofile, paramMap);
				});
				return userid;
			} catch (DuplicateKeyException e) {
				if (attempt == 3) {
					throw e;
				}
				log.warn("User {} was added meanwhile; trying the next number", userid);
			}
		}
	}

	@Override
	public void updateUser(UserInfo userinfo) {
		Map<String, Object> paramMap = paramMap(userinfo);
		paramMap.put("userid", userinfo.getUserid());
		namedParameterJdbcTemplate.update(updateuser, paramMap);
	}

	@Override
	public void setActive(String userid, boolean active) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("userid", userid);
		paramMap.put("isactive", active ? 1 : 0);
		namedParameterJdbcTemplate.update(setactive, paramMap);
	}

	@Override
	public void setPassword(String userid, String encodedPassword, boolean mustChange) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("userid", userid);
		paramMap.put("password", encodedPassword);
		paramMap.put("mustchangepassword", mustChange ? 1 : 0);
		namedParameterJdbcTemplate.update(setpassword, paramMap);
	}

	@Override
	public String findPassword(String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("userid", userid);
		List<String> list = namedParameterJdbcTemplate.queryForList(passwordbyid, paramMap, String.class);
		// No row, or (unknown schema, G22) several: nothing to check a password against
		return list.size() == 1 ? list.get(0) : null;
	}

	@Override
	public int countActiveSuperAdmins() {
		return namedParameterJdbcTemplate.queryForObject(activesuperadmins, new HashMap<String, Object>(),
				Integer.class);
	}

	/** The profile columns, with isinterviewer derived from the role. */
	private static Map<String, Object> paramMap(UserInfo userinfo) {
		Role role = Role.parse(userinfo.getRole());
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("firstname", userinfo.getFirstname());
		paramMap.put("lastname", userinfo.getLastname());
		paramMap.put("email", userinfo.getEmail());
		paramMap.put("phone", userinfo.getPhone());
		paramMap.put("designation", userinfo.getDesignation());
		paramMap.put("role", role == null ? null : role.name());
		paramMap.put("isinterviewer", role == null ? null : role.getIsinterviewer());
		return paramMap;
	}

}
