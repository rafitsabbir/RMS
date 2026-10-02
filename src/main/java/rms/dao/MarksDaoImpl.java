package rms.dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import rms.model.Criterion;
import rms.model.DecisionStatus;
import rms.model.MarksInfo;
import rms.model.ResultInfo;

/**
 * Evaluations (marks) and the Candidate Status averages. Since Phase 3 every column is named and read by name, so
 * the real table's column order no longer matters (G10). Only rows with isactive = 1 count.
 */
@Repository
public class MarksDaoImpl implements MarksDao {

	// Names and flags by subquery, so a missing or duplicated admin row doesn't drop or repeat an evaluation. An
	// interviewer counts as active only while their account is active and has the Interviewer role
	private String selectmarks = "select m.markkey, m.isactive, m.interviewerid, m.candidateid, m.workexp, "
			+ "m.techknowledge, m.leadership, m.decision, m.probsolving, m.stress, m.education, m.comskill, "
			+ "m.attitude, m.personality, m.comments, m.workexpcomment, m.techknowledgecomment, m.leadershipcomment, "
			+ "m.decisioncomment, m.probsolvingcomment, m.stresscomment, m.educationcomment, m.comskillcomment, "
			+ "m.attitudecomment, m.personalitycomment, m.createdat, m.updatedat, "
			+ "(select min(concat_ws(' ', a.firstname, a.lastname)) from admin a where a.userid=m.interviewerid) "
			+ "as interviewername, coalesce((select max(case when a.isactive=1 and (upper(trim(a.role))='INTERVIEWER' "
			+ "or (a.role is null and upper(a.isinterviewer)='Y')) then 1 else 0 end) from admin a "
			+ "where a.userid=m.interviewerid), 0) as intervieweractive, "
			+ "(select count(*) from candidate_interviewer ci where ci.candidateid=m.candidateid "
			+ "and ci.interviewerid=m.interviewerid and ci.isactive=1) as assigned from marks m";
	private String evaluationsbycandidate = selectmarks
			+ " where m.candidateid=:candidateid and m.isactive=1 order by m.markkey";
	// The oldest, if older data has several for one interviewer and candidate
	private String findevaluation = selectmarks + " where m.candidateid=:candidateid "
			+ "and m.interviewerid=:interviewerid and m.isactive=1 order by m.markkey limit 1";
	// Every active candidate (deleted ones are hidden, decision N), with or without evaluations; the averages of
	// their active evaluations, NULL without any
	private String results = "select c.candidateid, c.firstname, c.lastname, c.candidatestatus, p.positionname, "
			+ "l.languagename, count(m.markkey) as evaluations, avg(m.workexp) as workexp, "
			+ "avg(m.techknowledge) as techknowledge, avg(m.leadership) as leadership, avg(m.decision) as decision, "
			+ "avg(m.probsolving) as probsolving, avg(m.stress) as stress, avg(m.education) as education, "
			+ "avg(m.comskill) as comskill, avg(m.attitude) as attitude, avg(m.personality) as personality "
			+ "from candidate c left join position p on p.positionkey=c.positionkey "
			+ "left join language l on l.languagekey=c.languagekey "
			+ "left join marks m on m.candidateid=c.candidateid and m.isactive=1 where c.isactive=1 "
			+ "group by c.candidateid, c.firstname, c.lastname, c.candidatestatus, p.positionname, l.languagename "
			+ "order by length(c.candidateid), c.candidateid";
	// Saving: lock the candidate's row and re-check under the lock that it is active and not decided (Selected or
	// Rejected) and that the interviewer is still assigned (a shared lock on the assignment), so a decision or an
	// unassignment can't slip in between
	private String lockcandidate = "select candidatestatus from candidate where candidateid=:candidateid "
			+ "and isactive=1 for update";
	// A shared lock on the assignment row, so an unassignment committing meanwhile waits for the save (LOCK IN SHARE
	// MODE works on MySQL 5.7 and 8)
	private String isassigned = "select count(*) from candidate_interviewer where candidateid=:candidateid "
			+ "and interviewerid=:interviewerid and isactive=1 lock in share mode";
	private String existingevaluation = "select min(markkey) from marks where candidateid=:candidateid "
			+ "and interviewerid=:interviewerid and isactive=1";
	private String insertmarks = "insert into marks (isactive, interviewerid, candidateid, workexp, techknowledge, "
			+ "leadership, decision, probsolving, stress, education, comskill, attitude, personality, comments, "
			+ "workexpcomment, techknowledgecomment, leadershipcomment, decisioncomment, probsolvingcomment, "
			+ "stresscomment, educationcomment, comskillcomment, attitudecomment, personalitycomment, createdat, "
			+ "updatedat) values (1, :interviewerid, :candidateid, :workexp, :techknowledge, :leadership, :decision, "
			+ ":probsolving, :stress, :education, :comskill, :attitude, :personality, :comments, :workexpcomment, "
			+ ":techknowledgecomment, :leadershipcomment, :decisioncomment, :probsolvingcomment, :stresscomment, "
			+ ":educationcomment, :comskillcomment, :attitudecomment, :personalitycomment, now(), now())";
	private String updatemarks = "update marks set workexp=:workexp, techknowledge=:techknowledge, "
			+ "leadership=:leadership, decision=:decision, probsolving=:probsolving, stress=:stress, "
			+ "education=:education, comskill=:comskill, attitude=:attitude, personality=:personality, "
			+ "comments=:comments, workexpcomment=:workexpcomment, techknowledgecomment=:techknowledgecomment, "
			+ "leadershipcomment=:leadershipcomment, decisioncomment=:decisioncomment, "
			+ "probsolvingcomment=:probsolvingcomment, stresscomment=:stresscomment, "
			+ "educationcomment=:educationcomment, comskillcomment=:comskillcomment, "
			+ "attitudecomment=:attitudecomment, personalitycomment=:personalitycomment, updatedat=now() "
			+ "where markkey=:markkey";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	TransactionTemplate transactionTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.transactionTemplate = null;
	}

	/** saveMarks's transaction, on the template's DataSource; made on first use. */
	private synchronized TransactionTemplate transactions() {
		if (transactionTemplate == null) {
			transactionTemplate = new TransactionTemplate(
					new DataSourceTransactionManager(namedParameterJdbcTemplate.getJdbcTemplate().getDataSource()));
		}
		return transactionTemplate;
	}

	private static final class MarksMapper implements RowMapper<MarksInfo> {
		@Override
		public MarksInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			MarksInfo marks = new MarksInfo();
			marks.setMarkkey(rs.getInt("markkey"));
			marks.setIsactive(rs.getInt("isactive"));
			marks.setInterviewerid(rs.getString("interviewerid"));
			marks.setCandidateid(rs.getString("candidateid"));
			for (Criterion criterion : Criterion.values()) {
				marks.setScore(criterion, rs.getInt(criterion.getField()));
				marks.setComment(criterion, rs.getString(criterion.getCommentfield()));
			}
			marks.setComments(rs.getString("comments"));
			marks.setCreatedat(rs.getObject("createdat", LocalDateTime.class));
			marks.setUpdatedat(rs.getObject("updatedat", LocalDateTime.class));
			marks.setInterviewername(rs.getString("interviewername"));
			marks.setIntervieweractive(rs.getInt("intervieweractive") == 1);
			marks.setAssigned(rs.getInt("assigned") > 0);
			return marks;
		}
	}

	private static final class ResultMapper implements RowMapper<ResultInfo> {
		@Override
		public ResultInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			ResultInfo result = new ResultInfo();
			result.setCandidateid(rs.getString("candidateid"));
			result.setFirstname(rs.getString("firstname"));
			result.setLastname(rs.getString("lastname"));
			result.setCandidatestatus(rs.getString("candidatestatus"));
			result.setPositionname(rs.getString("positionname"));
			result.setLanguagename(rs.getString("languagename"));
			result.setEvaluations(rs.getInt("evaluations"));
			List<BigDecimal> averages = new ArrayList<BigDecimal>();
			for (Criterion criterion : Criterion.values()) {
				averages.add(rs.getBigDecimal(criterion.getField()));
			}
			result.setAverages(averages);
			return result;
		}
	}

	@Override
	public List<ResultInfo> getResults() {
		return namedParameterJdbcTemplate.query(results, new HashMap<String, Object>(), new ResultMapper());
	}

	@Override
	public List<MarksInfo> getEvaluations(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		return namedParameterJdbcTemplate.query(evaluationsbycandidate, paramMap, new MarksMapper());
	}

	@Override
	public MarksInfo findEvaluation(String candidateid, String interviewerid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		paramMap.put("interviewerid", interviewerid);
		List<MarksInfo> list = namedParameterJdbcTemplate.query(findevaluation, paramMap, new MarksMapper());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	// One evaluation per interviewer and candidate: the existing one is updated, otherwise one is inserted, in a
	// transaction behind a lock on the candidate's row. synchronized too, for one Tomcat if candidate isn't InnoDB
	public synchronized int saveMarks(MarksInfo marksinfo) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("candidateid", marksinfo.getCandidateid());
		params.addValue("interviewerid", marksinfo.getInterviewerid());
		for (Criterion criterion : Criterion.values()) {
			params.addValue(criterion.getField(), marksinfo.getScore(criterion));
			params.addValue(criterion.getCommentfield(), marksinfo.getComment(criterion));
		}
		params.addValue("comments", marksinfo.getComments());

		Integer markkey = transactions().execute(status -> {
			List<String> candidate = namedParameterJdbcTemplate.queryForList(lockcandidate, params, String.class);
			if (candidate.isEmpty() || DecisionStatus.locks(candidate.get(0))
					|| namedParameterJdbcTemplate.queryForObject(isassigned, params, Integer.class) == 0) {
				return 0;
			}
			Integer existing = namedParameterJdbcTemplate.queryForObject(existingevaluation, params, Integer.class);
			if (existing != null) {
				params.addValue("markkey", existing);
				namedParameterJdbcTemplate.update(updatemarks, params);
				return existing;
			}
			KeyHolder keys = new GeneratedKeyHolder();
			namedParameterJdbcTemplate.update(insertmarks, params, keys, new String[] { "markkey" });
			return keys.getKey().intValue();
		});
		return markkey;
	}

}
