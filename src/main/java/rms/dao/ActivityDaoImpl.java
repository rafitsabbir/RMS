package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import rms.model.ActivityInfo;

@Repository
public class ActivityDaoImpl implements ActivityDao {

	private String addactivity = "insert into activity_log (userid, action, entitytype, entityid, detail, createdat) "
			+ "values (:userid, :action, :entitytype, :entityid, :detail, now())";
	// The name by subquery: an entry still lists when its user was removed from admin
	private String selectrecent = "select l.activitykey, l.userid, l.action, l.entitytype, l.entityid, l.detail, "
			+ "l.createdat, (select min(concat_ws(' ', a.firstname, a.lastname)) from admin a "
			+ "where a.userid=l.userid) as username from activity_log l";
	private String recent = selectrecent + " order by l.activitykey desc limit ";
	private String recentofaction = selectrecent + " where l.action=:action order by l.activitykey desc limit ";
	private String actions = "select distinct action from activity_log order by action";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class ActivityMapper implements RowMapper<ActivityInfo> {
		@Override
		public ActivityInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			ActivityInfo activity = new ActivityInfo();
			activity.setActivitykey(rs.getLong("activitykey"));
			activity.setUserid(rs.getString("userid"));
			activity.setUsername(rs.getString("username"));
			activity.setAction(rs.getString("action"));
			activity.setEntitytype(rs.getString("entitytype"));
			activity.setEntityid(rs.getString("entityid"));
			activity.setDetail(rs.getString("detail"));
			activity.setCreatedat(rs.getObject("createdat", LocalDateTime.class));
			return activity;
		}
	}

	@Override
	public void add(ActivityInfo activity) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("userid", activity.getUserid());
		params.put("action", activity.getAction());
		params.put("entitytype", activity.getEntitytype());
		params.put("entityid", activity.getEntityid());
		params.put("detail", activity.getDetail());
		namedParameterJdbcTemplate.update(addactivity, params);
	}

	@Override
	public List<ActivityInfo> getRecent(String action, int limit) {
		// limit is an int, so it is safe to put in the SQL
		Map<String, Object> params = new HashMap<String, Object>();
		if (action == null) {
			return namedParameterJdbcTemplate.query(recent + limit, params, new ActivityMapper());
		}
		params.put("action", action);
		return namedParameterJdbcTemplate.query(recentofaction + limit, params, new ActivityMapper());
	}

	@Override
	public List<String> getActions() {
		return namedParameterJdbcTemplate.queryForList(actions, new HashMap<String, Object>(), String.class);
	}
}
