package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import rms.model.DecisionInfo;

@Repository
public class DecisionDaoImpl implements DecisionDao {

	// The latest decision lives in the candidate row (G9: the first code that writes candidatestatus)
	private String updatecandidate = "update candidate set candidatestatus=:status, decisionreason=:reason, "
			+ "decisiondate=:decisiondate, decidedby=:decidedby where candidateid=:candidateid and isactive=1";
	private String savedecision = "insert into candidate_decision (candidateid, status, reason, decisiondate, "
			+ "decidedby, decidedat) values (:candidateid, :status, :reason, :decisiondate, :decidedby, now())";
	private String decisionsbycandidate = "select d.decisionkey, d.candidateid, d.status, d.reason, d.decisiondate, "
			+ "d.decidedby, d.decidedat, (select min(concat_ws(' ', a.firstname, a.lastname)) from admin a "
			+ "where a.userid=d.decidedby) as decidedbyname from candidate_decision d "
			+ "where d.candidateid=:candidateid order by d.decidedat desc, d.decisionkey desc";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	TransactionTemplate transactionTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.transactionTemplate = null;
	}

	/** saveDecision's transaction, on the template's DataSource; made on first use. */
	private synchronized TransactionTemplate transactions() {
		if (transactionTemplate == null) {
			transactionTemplate = new TransactionTemplate(
					new DataSourceTransactionManager(namedParameterJdbcTemplate.getJdbcTemplate().getDataSource()));
		}
		return transactionTemplate;
	}

	private static final class DecisionMapper implements RowMapper<DecisionInfo> {
		@Override
		public DecisionInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			DecisionInfo decision = new DecisionInfo();
			decision.setDecisionkey(rs.getInt("decisionkey"));
			decision.setCandidateid(rs.getString("candidateid"));
			decision.setStatus(rs.getString("status"));
			decision.setReason(rs.getString("reason"));
			decision.setDecisiondate(rs.getObject("decisiondate", LocalDate.class));
			decision.setDecidedby(rs.getString("decidedby"));
			decision.setDecidedat(rs.getObject("decidedat", LocalDateTime.class));
			decision.setDecidedbyname(rs.getString("decidedbyname"));
			return decision;
		}
	}

	@Override
	public boolean saveDecision(DecisionInfo decision) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("candidateid", decision.getCandidateid());
		params.addValue("status", decision.getStatus());
		params.addValue("reason", decision.getReason());
		params.addValue("decisiondate", decision.getDecisiondate());
		params.addValue("decidedby", decision.getDecidedby());

		Boolean saved = transactions().execute(status -> {
			// The update locks the candidate's row, so an evaluation being saved waits (MarksDaoImpl.saveMarks). It counts
			// matched rows (Connector/J's default useAffectedRows=false), so the same decision again still counts as 1
			if (namedParameterJdbcTemplate.update(updatecandidate, params) == 0) {
				return false;
			}
			namedParameterJdbcTemplate.update(savedecision, params);
			return true;
		});
		return Boolean.TRUE.equals(saved);
	}

	@Override
	public List<DecisionInfo> getDecisions(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		return namedParameterJdbcTemplate.query(decisionsbycandidate, paramMap, new DecisionMapper());
	}

}
