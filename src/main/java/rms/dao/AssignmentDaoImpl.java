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

import rms.model.AssignmentInfo;
import rms.model.UserInfo;

@Repository
public class AssignmentDaoImpl implements AssignmentDao {

	// An interviewer counts as active only while their account is active and has the Interviewer role (role, or the
	// isinterviewer fallback of rms.model.Role.of); otherwise the profile shows "interviewer inactive"
	private String assignmentsbycandidate = "select ci.assignmentkey, ci.candidateid, ci.interviewerid, "
			+ "ci.assignedat, (select min(concat_ws(' ', a.firstname, a.lastname)) from admin a "
			+ "where a.userid=ci.interviewerid) as interviewername, coalesce((select max(case when a.isactive=1 "
			+ "and (upper(trim(a.role))='INTERVIEWER' or (a.role is null and upper(a.isinterviewer)='Y')) then 1 else 0 end) "
			+ "from admin a where a.userid=ci.interviewerid), 0) as intervieweractive, (select count(*) from marks m "
			+ "where m.candidateid=ci.candidateid and m.interviewerid=ci.interviewerid and m.isactive=1) as evaluated "
			+ "from candidate_interviewer ci where ci.candidateid=:candidateid and ci.isactive=1 "
			+ "order by ci.assignmentkey";
	private String isassigned = "select count(*) from candidate_interviewer where candidateid=:candidateid "
			+ "and interviewerid=:interviewerid and isactive=1";
	// Only Interviewer-role users (decision F), active, and not assigned already
	private String assignable = "select a.userid, a.firstname, a.lastname from admin a where a.isactive=1 "
			+ "and (upper(trim(a.role))='INTERVIEWER' or (a.role is null and upper(a.isinterviewer)='Y')) "
			+ "and not exists (select 1 from candidate_interviewer ci where ci.candidateid=:candidateid "
			+ "and ci.interviewerid=a.userid and ci.isactive=1) order by a.firstname, a.lastname, a.userid";
	private String assign = "insert into candidate_interviewer (candidateid, interviewerid, assignedby, assignedat, "
			+ "isactive) values (:candidateid, :interviewerid, :assignedby, now(), 1)";
	private String unassign = "update candidate_interviewer set isactive=0, unassignedby=:userid, unassignedat=now() "
			+ "where candidateid=:candidateid and interviewerid=:interviewerid and isactive=1";
	private String mycandidates = "select c.candidateid, c.firstname, c.lastname, c.candidatestatus, p.positionname, "
			+ "(select max(ci.assignedat) from candidate_interviewer ci where ci.candidateid=c.candidateid "
			+ "and ci.interviewerid=:interviewerid and ci.isactive=1) as assignedat, "
			+ "(select count(*) from marks m where m.candidateid=c.candidateid and m.interviewerid=:interviewerid "
			+ "and m.isactive=1) as evaluated from candidate c left join position p on p.positionkey=c.positionkey "
			+ "where c.isactive=1 and (exists (select 1 from candidate_interviewer ci where ci.candidateid=c.candidateid "
			+ "and ci.interviewerid=:interviewerid and ci.isactive=1) or exists (select 1 from marks m "
			+ "where m.candidateid=c.candidateid and m.interviewerid=:interviewerid and m.isactive=1)) "
			+ "order by length(c.candidateid), c.candidateid";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class AssignmentMapper implements RowMapper<AssignmentInfo> {
		@Override
		public AssignmentInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			AssignmentInfo assignment = new AssignmentInfo();
			assignment.setAssignmentkey(rs.getInt("assignmentkey"));
			assignment.setCandidateid(rs.getString("candidateid"));
			assignment.setInterviewerid(rs.getString("interviewerid"));
			assignment.setAssignedat(rs.getObject("assignedat", LocalDateTime.class));
			assignment.setInterviewername(rs.getString("interviewername"));
			assignment.setIntervieweractive(rs.getInt("intervieweractive") == 1);
			assignment.setEvaluated(rs.getInt("evaluated") > 0);
			assignment.setAssigned(true);
			return assignment;
		}
	}

	private static final class MyCandidateMapper implements RowMapper<AssignmentInfo> {
		@Override
		public AssignmentInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			AssignmentInfo assignment = new AssignmentInfo();
			assignment.setCandidateid(rs.getString("candidateid"));
			assignment.setCandidatefirstname(rs.getString("firstname"));
			assignment.setCandidatelastname(rs.getString("lastname"));
			assignment.setCandidatestatus(rs.getString("candidatestatus"));
			assignment.setPositionname(rs.getString("positionname"));
			LocalDateTime assignedat = rs.getObject("assignedat", LocalDateTime.class);
			assignment.setAssignedat(assignedat);
			// No active assignment: evaluated earlier, unassigned since
			assignment.setAssigned(assignedat != null);
			assignment.setEvaluated(rs.getInt("evaluated") > 0);
			return assignment;
		}
	}

	private static final class InterviewerMapper implements RowMapper<UserInfo> {
		@Override
		public UserInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			UserInfo user = new UserInfo();
			user.setUserid(rs.getString("userid"));
			user.setFirstname(rs.getString("firstname"));
			user.setLastname(rs.getString("lastname"));
			return user;
		}
	}

	@Override
	public List<AssignmentInfo> getAssignments(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		return namedParameterJdbcTemplate.query(assignmentsbycandidate, paramMap, new AssignmentMapper());
	}

	@Override
	public boolean isAssigned(String candidateid, String interviewerid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		paramMap.put("interviewerid", interviewerid);
		return namedParameterJdbcTemplate.queryForObject(isassigned, paramMap, Integer.class) > 0;
	}

	@Override
	public List<UserInfo> getAssignableInterviewers(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		return namedParameterJdbcTemplate.query(assignable, paramMap, new InterviewerMapper());
	}

	@Override
	// synchronized: the check and the insert must not interleave with another assignment. One Tomcat only; on two
	// nodes a double click could add the assignment twice, which shows twice and is ended by one unassign
	public synchronized boolean assign(String candidateid, String interviewerid, String assignedby) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		paramMap.put("interviewerid", interviewerid);
		paramMap.put("assignedby", assignedby);
		if (isAssigned(candidateid, interviewerid)) {
			return false;
		}
		return namedParameterJdbcTemplate.update(assign, paramMap) == 1;
	}

	@Override
	public boolean unassign(String candidateid, String interviewerid, String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		paramMap.put("interviewerid", interviewerid);
		paramMap.put("userid", userid);
		return namedParameterJdbcTemplate.update(unassign, paramMap) > 0;
	}

	@Override
	public List<AssignmentInfo> getMyCandidates(String interviewerid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("interviewerid", interviewerid);
		return namedParameterJdbcTemplate.query(mycandidates, paramMap, new MyCandidateMapper());
	}

}
