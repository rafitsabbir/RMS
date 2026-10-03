package rms.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardDaoImpl implements DashboardDao {

	// The same conditions as AssignmentDaoImpl: an interviewer is active while their account is active and has the
	// Interviewer role (role, or the isinterviewer fallback of rms.model.Role.of)
	private String activeinterviewer = "exists (select 1 from admin a where a.userid=ci.interviewerid and a.isactive=1 "
			+ "and (upper(trim(a.role))='INTERVIEWER' or (a.role is null and upper(a.isinterviewer)='Y')))";
	// Not Selected or Rejected (rms.model.DecisionStatus.locks): On hold, no decision and unknown texts stay open
	private String undecided = "(c.candidatestatus is null or upper(trim(c.candidatestatus)) not in ('S','R'))";
	private String bystatus = "select upper(trim(candidatestatus)) as code, count(*) as total from candidate "
			+ "where isactive=1 group by upper(trim(candidatestatus))";
	// count(distinct ...): a double-clicked assignment is two rows, but one pending evaluation
	private String pendingevaluations = "select count(distinct ci.candidateid, ci.interviewerid) from "
			+ "candidate_interviewer ci join candidate c on c.candidateid=ci.candidateid and c.isactive=1 "
			+ "where ci.isactive=1 and " + undecided + " and " + activeinterviewer + " and not exists (select 1 from "
			+ "marks m where m.candidateid=ci.candidateid and m.interviewerid=ci.interviewerid and m.isactive=1)";
	private String openjobs = "select count(*) from job where isactive=1 and status='OPEN'";
	private String withoutcv = "select count(*) from candidate c where c.isactive=1 and not exists (select 1 from "
			+ "candidate_document d where d.candidateid=c.candidateid and d.doctype='CV' and d.isactive=1 "
			+ "and d.purgedat is null)";
	private String withinactiveinterviewer = "select count(distinct c.candidateid) from candidate c join "
			+ "candidate_interviewer ci on ci.candidateid=c.candidateid and ci.isactive=1 where c.isactive=1 and "
			+ undecided + " and not " + activeinterviewer;

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Override
	public Map<String, Integer> countCandidatesByStatus() {
		Map<String, Integer> counts = new HashMap<String, Integer>();
		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(bystatus,
				new HashMap<String, Object>());
		for (Map<String, Object> row : rows) {
			Object code = row.get("code");
			counts.merge(code == null ? "" : code.toString(), ((Number) row.get("total")).intValue(), Integer::sum);
		}
		return counts;
	}

	@Override
	public int countPendingEvaluations(String interviewerid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		String sql = pendingevaluations;
		if (interviewerid != null) {
			sql += " and ci.interviewerid=:interviewerid";
			paramMap.put("interviewerid", interviewerid);
		}
		return namedParameterJdbcTemplate.queryForObject(sql, paramMap, Integer.class);
	}

	@Override
	public int countOpenJobs() {
		return count(openjobs);
	}

	@Override
	public int countCandidatesWithoutCv() {
		return count(withoutcv);
	}

	@Override
	public int countCandidatesWithInactiveInterviewer() {
		return count(withinactiveinterviewer);
	}

	private int count(String sql) {
		return namedParameterJdbcTemplate.queryForObject(sql, new HashMap<String, Object>(), Integer.class);
	}

}
