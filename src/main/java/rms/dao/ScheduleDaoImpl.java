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

import rms.model.ScheduleInfo;

@Repository
public class ScheduleDaoImpl implements ScheduleDao {

	// Names by subquery: an interview still lists when its interviewer was removed from admin. Interviews of a
	// deleted (inactive) candidate are hidden, like the candidate
	private String visible = "s.isactive=1 and exists (select 1 from candidate c where c.candidateid=s.candidateid "
			+ "and c.isactive=1)";
	private String selectschedule = "select s.schedulekey, s.candidateid, s.interviewerid, s.startat, s.location, "
			+ "s.status, (select min(concat_ws(' ', c.firstname, c.lastname)) from candidate c "
			+ "where c.candidateid=s.candidateid) as candidatename, (select min(concat_ws(' ', a.firstname, "
			+ "a.lastname)) from admin a where a.userid=s.interviewerid) as interviewername from interview_schedule s "
			+ "where " + visible;
	private String order = " order by s.startat, s.schedulekey";
	private String allschedule = selectschedule + order;
	private String findschedulebyid = selectschedule + " and s.schedulekey=:schedulekey";
	private String upcomingfilter = " and s.status='SCHEDULED' and s.startat>=now()";
	// An interviewer sees an interview only while still assigned to the candidate (an unassigned or deactivated
	// interviewer's scheduled interviews drop out of their own pages; staff still see them and can cancel them)
	private String ofinterviewer = " and s.interviewerid=:interviewerid and exists (select 1 from candidate_interviewer ci "
			+ "where ci.candidateid=s.candidateid and ci.interviewerid=s.interviewerid and ci.isactive=1)";
	private String scheduleof = selectschedule + ofinterviewer + order;
	private String countupcoming = "select count(*) from interview_schedule s where " + visible + upcomingfilter;
	private String conflict = "select count(*) from interview_schedule where isactive=1 and status='SCHEDULED' "
			+ "and interviewerid=:interviewerid and startat=:startat and schedulekey<>:schedulekey";
	private String addschedule = "insert into interview_schedule (candidateid, interviewerid, startat, location, "
			+ "status, createdby, createdat, isactive) values (:candidateid, :interviewerid, :startat, :location, "
			+ "'SCHEDULED', :userid, now(), 1)";
	private String updateschedule = "update interview_schedule set interviewerid=:interviewerid, startat=:startat, "
			+ "location=:location, status=:status, updatedby=:userid, updatedat=now() "
			+ "where schedulekey=:schedulekey and isactive=1 and status<>'CANCELLED'";
	private String cancelschedule = "update interview_schedule set status='CANCELLED', updatedby=:userid, "
			+ "updatedat=now() where schedulekey=:schedulekey and isactive=1 and status='SCHEDULED'";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class ScheduleMapper implements RowMapper<ScheduleInfo> {
		@Override
		public ScheduleInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			ScheduleInfo schedule = new ScheduleInfo();
			schedule.setSchedulekey(rs.getInt("schedulekey"));
			schedule.setCandidateid(rs.getString("candidateid"));
			schedule.setCandidatename(rs.getString("candidatename"));
			schedule.setInterviewerid(rs.getString("interviewerid"));
			schedule.setInterviewername(rs.getString("interviewername"));
			schedule.setStartat(rs.getObject("startat", LocalDateTime.class));
			schedule.setLocation(rs.getString("location"));
			schedule.setStatus(rs.getString("status"));
			return schedule;
		}
	}

	@Override
	public List<ScheduleInfo> getAllSchedule() {
		return namedParameterJdbcTemplate.query(allschedule, new HashMap<String, Object>(), new ScheduleMapper());
	}

	@Override
	public List<ScheduleInfo> getScheduleOf(String interviewerid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("interviewerid", interviewerid);
		return namedParameterJdbcTemplate.query(scheduleof, paramMap, new ScheduleMapper());
	}

	@Override
	public ScheduleInfo findScheduleById(int schedulekey) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("schedulekey", schedulekey);
		List<ScheduleInfo> list = namedParameterJdbcTemplate.query(findschedulebyid, paramMap, new ScheduleMapper());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public List<ScheduleInfo> getUpcoming(String interviewerid, int limit) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		String sql = selectschedule + upcomingfilter;
		if (interviewerid != null) {
			sql += ofinterviewer;
			paramMap.put("interviewerid", interviewerid);
		}
		// The limit is an int, not user text, so it goes into the SQL as a number
		sql += order + " limit " + Math.max(limit, 0);
		return namedParameterJdbcTemplate.query(sql, paramMap, new ScheduleMapper());
	}

	@Override
	public int countUpcoming(String interviewerid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		String sql = countupcoming;
		if (interviewerid != null) {
			sql += ofinterviewer;
			paramMap.put("interviewerid", interviewerid);
		}
		return namedParameterJdbcTemplate.queryForObject(sql, paramMap, Integer.class);
	}

	@Override
	public boolean hasConflict(String interviewerid, LocalDateTime startat, int excludekey) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("interviewerid", interviewerid);
		paramMap.put("startat", startat);
		paramMap.put("schedulekey", excludekey);
		return namedParameterJdbcTemplate.queryForObject(conflict, paramMap, Integer.class) > 0;
	}

	@Override
	public void addSchedule(ScheduleInfo scheduleinfo, String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", scheduleinfo.getCandidateid());
		paramMap.put("interviewerid", scheduleinfo.getInterviewerid());
		paramMap.put("startat", scheduleinfo.getStartat());
		paramMap.put("location", scheduleinfo.getLocation());
		paramMap.put("userid", userid);
		namedParameterJdbcTemplate.update(addschedule, paramMap);
	}

	@Override
	public boolean updateSchedule(ScheduleInfo scheduleinfo, String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("schedulekey", scheduleinfo.getSchedulekey());
		paramMap.put("interviewerid", scheduleinfo.getInterviewerid());
		paramMap.put("startat", scheduleinfo.getStartat());
		paramMap.put("location", scheduleinfo.getLocation());
		paramMap.put("status", scheduleinfo.getStatus());
		paramMap.put("userid", userid);
		return namedParameterJdbcTemplate.update(updateschedule, paramMap) > 0;
	}

	@Override
	public boolean cancelSchedule(int schedulekey, String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("schedulekey", schedulekey);
		paramMap.put("userid", userid);
		return namedParameterJdbcTemplate.update(cancelschedule, paramMap) > 0;
	}

}
