package rms.dao;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import rms.model.RetentionInfo;

/** The retention clean-up (F11). Never logs names: the callers log candidate IDs only. */
@Repository
public class RetentionDaoImpl implements RetentionDao {

	/** What a cleaned-up candidate is called; English letters, so every page shows it as is. */
	public static final String REMOVED_FIRSTNAME = "Removed";
	public static final String REMOVED_LASTNAME = "candidate";

	// The newest of every date RMS keeps about the candidate; '1000-01-01' stands for "none"
	private String due = "select * from (select c.candidateid, concat_ws(' ', c.firstname, c.lastname) as name, c.stage, "
			+ "c.candidatestatus, c.isactive, greatest(coalesce(c.applieddate, '1000-01-01'), "
			+ "coalesce(c.decisiondate, '1000-01-01'), "
			+ "coalesce((select date(max(cs.changedat)) from candidate_stage cs where cs.candidateid=c.candidateid), '1000-01-01'), "
			+ "coalesce((select date(max(s.startat)) from interview_schedule s where s.candidateid=c.candidateid), '1000-01-01'), "
			+ "coalesce((select date(max(coalesce(m.updatedat, m.createdat))) from marks m where m.candidateid=c.candidateid), '1000-01-01'), "
			+ "coalesce((select date(max(d.uploadedat)) from candidate_document d where d.candidateid=c.candidateid), '1000-01-01'), "
			+ "coalesce((select date(max(o.createdat)) from candidate_offer o where o.candidateid=c.candidateid), '1000-01-01')) "
			+ "as lastactivity from candidate c where c.anonymizedat is null "
			+ "and coalesce(upper(trim(c.stage)), 'APPLIED')<>'HIRED') t "
			+ "where t.lastactivity>'1000-01-01' and t.lastactivity<:before "
			+ "order by t.lastactivity, length(t.candidateid), t.candidateid";
	private String candidate = "update candidate set firstname='" + REMOVED_FIRSTNAME + "', lastname='" + REMOVED_LASTNAME
			+ "', email=null, phone=null, decisionreason=null, isactive=0, anonymizedat=now(), anonymizedby=:userid "
			+ "where candidateid=:candidateid and anonymizedat is null";
	private String decisions = "update candidate_decision set reason='Removed' where candidateid=:candidateid";
	private String stages = "update candidate_stage set note=null where candidateid=:candidateid";
	private String marks = "update marks set comments=null, workexpcomment=null, techknowledgecomment=null, "
			+ "leadershipcomment=null, decisioncomment=null, probsolvingcomment=null, stresscomment=null, "
			+ "educationcomment=null, comskillcomment=null, attitudecomment=null, personalitycomment=null "
			+ "where candidateid=:candidateid";
	private String offers = "update candidate_offer set salaryband=null, note=null where candidateid=:candidateid";
	private String documents = "update candidate_document set originalname='removed', title=null, issuer=null "
			+ "where candidateid=:candidateid";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	TransactionTemplate transactionTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.transactionTemplate = null;
	}

	private synchronized TransactionTemplate transactions() {
		if (transactionTemplate == null) {
			transactionTemplate = new TransactionTemplate(
					new DataSourceTransactionManager(namedParameterJdbcTemplate.getJdbcTemplate().getDataSource()));
		}
		return transactionTemplate;
	}

	@Override
	public List<RetentionInfo> getDue(LocalDate before) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("before", before);
		return namedParameterJdbcTemplate.query(due, paramMap, (rs, rowNum) -> {
			RetentionInfo info = new RetentionInfo();
			info.setCandidateid(rs.getString("candidateid"));
			info.setName(rs.getString("name"));
			info.setStage(rs.getString("stage"));
			info.setCandidatestatus(rs.getString("candidatestatus"));
			info.setActive(rs.getInt("isactive") == 1);
			info.setLastactivity(rs.getObject("lastactivity", LocalDate.class));
			return info;
		});
	}

	@Override
	public boolean anonymize(String candidateid, String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		paramMap.put("userid", userid);
		Boolean done = transactions().execute(status -> {
			if (namedParameterJdbcTemplate.update(candidate, paramMap) != 1) {
				return false;
			}
			for (String sql : new String[] { decisions, stages, marks, offers, documents }) {
				namedParameterJdbcTemplate.update(sql, paramMap);
			}
			return true;
		});
		return Boolean.TRUE.equals(done);
	}
}
