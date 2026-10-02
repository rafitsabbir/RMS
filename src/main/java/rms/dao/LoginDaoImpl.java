package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import rms.model.LoginInfo;
import rms.model.Role;
import rms.model.UserInfo;

@Repository
public class LoginDaoImpl implements LoginDao {
	// The password is checked in Java by Spring Security (SecurityConfig.passwordEncoder), not in SQL (G13)
	final String loginsbyusername = "select userid, username, password from users where username= :username";
	// The admin row (the profile) plus the users row's forced-change flag; NULL (0) when there is no users row.
	// A subquery, not a join: two users rows for one userid (no known key, G22) must not make two profile rows
	final String userinfo = "select a.*, (select max(u.mustchangepassword) from users u where u.userid=a.userid) "
			+ "as mustchangepassword from admin a where a.userid= :userid";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class UserMapper implements RowMapper<UserInfo> {
		public UserInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			UserInfo userinfo = new UserInfo();
			userinfo.setIsactive(rs.getInt("isactive"));
			userinfo.setUserid(rs.getString("userid"));
			userinfo.setUsername(rs.getString("username"));
			userinfo.setFirstname(rs.getString("firstname"));
			userinfo.setLastname(rs.getString("lastname"));
			userinfo.setEmail(rs.getString("email"));
			userinfo.setPhone(rs.getString("phone"));
			userinfo.setDesignation(rs.getString("designation"));
			userinfo.setIsinterviewer(rs.getString("isinterviewer"));
			// admin.role, or the role the legacy isinterviewer implies when it is NULL (migration 002)
			Role role = Role.of(rs.getString("role"), userinfo.getIsinterviewer());
			userinfo.setRole(role == null ? null : role.name());
			userinfo.setMustchangepassword(rs.getInt("mustchangepassword"));
			return userinfo;
		}
	}

	private static final class LoginMapper implements RowMapper<LoginInfo> {
		public LoginInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new LoginInfo(rs.getString("userid"), rs.getString("username"), rs.getString("password"));
		}
	}

	public List<LoginInfo> findLogins(String username) {
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("username", username);
		return namedParameterJdbcTemplate.query(loginsbyusername, paramMap, new LoginMapper());
	}

	public UserInfo getUserInfo(String userid) {
		// TODO Auto-generated method stub
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("userid", userid);
		try {
			return namedParameterJdbcTemplate.queryForObject(userinfo, paramMap, new UserMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}

	}

}
