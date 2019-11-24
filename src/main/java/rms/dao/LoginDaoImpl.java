package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import rms.model.UserInfo;

@Repository
public class LoginDaoImpl implements LoginDao {
	final String listallusers = "select userid from users where username= :username and password= :password";
	final String userinfo = "select * from admin where userid= :userid";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class UserMapper implements RowMapper<UserInfo> {
		public UserInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			UserInfo userinfo = new UserInfo();
			userinfo = new UserInfo();
			userinfo.setIsactive(rs.getInt("isactive"));
			userinfo.setUserid(rs.getString("userid"));
			userinfo.setUsername(rs.getString("username"));
			userinfo.setFirstname(rs.getString("firstname"));
			userinfo.setLastname(rs.getString("lastname"));
			userinfo.setEmail(rs.getString("email"));
			userinfo.setPhone(rs.getString("phone"));
			userinfo.setDesignation(rs.getString("designation"));
			userinfo.setIsinterviewer(rs.getString("isinterviewer"));
			return userinfo;
		}
	}

	public String checkUser(String username, String password) {
		// TODO Auto-generated method stub
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("username", username);
		paramMap.put("password", password);
		return namedParameterJdbcTemplate.queryForObject(listallusers, paramMap, String.class);
	}

	public UserInfo getUserInfo(String userid) {
		// TODO Auto-generated method stub
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("userid", userid);
		return namedParameterJdbcTemplate.queryForObject(userinfo, paramMap, new UserMapper());

	}

}
