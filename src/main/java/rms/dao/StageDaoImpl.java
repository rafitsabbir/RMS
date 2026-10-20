package rms.dao;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import rms.model.HiringStage;
import rms.model.StageInfo;

@Repository
public class StageDaoImpl implements StageDao {

	private String getstage = "select c.stage from candidate c where c.candidateid=:candidateid and c.isactive=1";
	// <=> is NULL-safe: a candidate from before migration 008's backfill may still be NULL. The update also locks the
	// row, so two moves of one candidate can't both pass the check. The decision rules are checked again here, on the
	// locked row: no move while Rejected, and Offer or Hired only while Selected
	private String updatestage = "update candidate set stage=:tostage where candidateid=:candidateid and isactive=1 "
			+ "and stage <=> :expected and (candidatestatus is null or upper(trim(candidatestatus))<>'R') "
			+ "and (:needsselected=0 or upper(trim(candidatestatus))='S')";
	static final String SAVESTAGE = "insert into candidate_stage (candidateid, fromstage, tostage, note, changedby, "
			+ "changedat) values (:candidateid, :expected, :tostage, :note, :userid, now())";
	private String stagesbycandidate = "select s.stagekey, s.candidateid, s.fromstage, s.tostage, s.note, s.changedby, "
			+ "s.changedat, (select min(concat_ws(' ', a.firstname, a.lastname)) from admin a "
			+ "where a.userid=s.changedby) as changedbyname from candidate_stage s "
			+ "where s.candidateid=:candidateid order by s.changedat desc, s.stagekey desc";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	TransactionTemplate transactionTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.transactionTemplate = null;
	}

	/** moveStage's transaction, on the template's DataSource; made on first use. */
	private synchronized TransactionTemplate transactions() {
		if (transactionTemplate == null) {
			transactionTemplate = new TransactionTemplate(
					new DataSourceTransactionManager(namedParameterJdbcTemplate.getJdbcTemplate().getDataSource()));
		}
		return transactionTemplate;
	}

	@Override
	public String getStage(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		List<String> stages = namedParameterJdbcTemplate.query(getstage, paramMap, (rs, rowNum) -> {
			String stage = rs.getString("stage");
			// An empty text, not null, so "no such candidate" stays apart from a NULL stage
			return stage == null ? "" : stage;
		});
		return stages.isEmpty() ? null : stages.get(0);
	}

	@Override
	public boolean moveStage(String candidateid, String expected, String tostage, String note, String userid) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("candidateid", candidateid);
		params.addValue("expected", expected);
		params.addValue("tostage", tostage);
		params.addValue("needsselected", HiringStage.of(tostage).needsSelected() ? 1 : 0);
		params.addValue("note", note == null || note.isBlank() ? null : note.trim());
		params.addValue("userid", userid);
		Boolean moved = transactions().execute(status -> {
			if (namedParameterJdbcTemplate.update(updatestage, params) == 0) {
				return false;
			}
			namedParameterJdbcTemplate.update(SAVESTAGE, params);
			return true;
		});
		return Boolean.TRUE.equals(moved);
	}

	@Override
	public List<StageInfo> getStages(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		return namedParameterJdbcTemplate.query(stagesbycandidate, paramMap, (rs, rowNum) -> {
			StageInfo stage = new StageInfo();
			stage.setStagekey(rs.getInt("stagekey"));
			stage.setCandidateid(rs.getString("candidateid"));
			stage.setFromstage(rs.getString("fromstage"));
			stage.setTostage(rs.getString("tostage"));
			stage.setNote(rs.getString("note"));
			stage.setChangedby(rs.getString("changedby"));
			stage.setChangedbyname(rs.getString("changedbyname"));
			stage.setChangedat(rs.getObject("changedat", LocalDateTime.class));
			return stage;
		});
	}
}
