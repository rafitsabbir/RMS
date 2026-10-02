package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import rms.model.CandidateInfo;

@Repository
public class CandidateDaoImpl implements CandidateDao {
	private static final Logger log = LoggerFactory.getLogger(CandidateDaoImpl.class);

	// Left joins: a candidate still lists when its position or language is deleted (soft delete) or missing
	private String selectcandidate = "select c.candidateid, c.firstname, c.lastname, c.positionkey, c.languagekey, "
			+ "c.candidatestatus, p.positionname, l.languagename from candidate c "
			+ "left join position p on p.positionkey=c.positionkey left join language l on l.languagekey=c.languagekey";
	// Shorter IDs first, so C2 comes before C10
	private String allcandidate = selectcandidate + " order by length(c.candidateid), c.candidateid";
	private String findcandidatebyid = selectcandidate + " where c.candidateid=:candidateid";
	// The highest number among IDs of the form C<number> (up to 9 digits); 0 when there is none
	private String lastnumber = "select coalesce(max(cast(substring(candidateid, 2) as unsigned)), 0) from candidate "
			+ "where candidateid regexp '^C[0-9]{1,9}$'";
	private String savecandidate = "insert into candidate (candidateid, firstname, lastname, positionkey, languagekey) "
			+ "VALUES (:candidateid, :firstname, :lastname, :positionkey, :languagekey)";
	private String updatecandidate = "update candidate set firstname=:firstname, lastname=:lastname, "
			+ "positionkey=:positionkey, languagekey=:languagekey where candidateid=:candidateid";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class CandidateMapper implements RowMapper<CandidateInfo> {

		public CandidateInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			CandidateInfo candidate = new CandidateInfo();
			candidate.setCandidateid(rs.getString("candidateid"));
			candidate.setFirstname(rs.getString("firstname"));
			candidate.setLastname(rs.getString("lastname"));
			candidate.setPositionkey(rs.getInt("positionkey"));
			candidate.setLanguagekey(rs.getInt("languagekey"));
			candidate.setCandidatestatus(rs.getString("candidatestatus"));
			candidate.setPositionname(rs.getString("positionname"));
			candidate.setLanguagename(rs.getString("languagename"));
			return candidate;
		}
	}

	@Override
	// synchronized: reading the highest number and inserting the next one must not interleave with another
	// add. This covers one Tomcat only; on several nodes a clash on the key is retried (when candidateid is a
	// key, G22).
	public synchronized String addCandidate(CandidateInfo candidateinfo) {
		Map<String, Object> paramMap = paramMap(candidateinfo);

		for (int attempt = 1; ; attempt++) {
			int last = namedParameterJdbcTemplate.queryForObject(lastnumber, new HashMap<String, Object>(),
					Integer.class);
			String candidateid = "C" + (last + 1);
			paramMap.put("candidateid", candidateid);
			try {
				namedParameterJdbcTemplate.update(savecandidate, paramMap);
				return candidateid;
			} catch (DuplicateKeyException e) {
				if (attempt == 3) {
					throw e;
				}
				log.warn("Candidate {} was added meanwhile; trying the next number", candidateid);
			}
		}
	}

	@Override
	public void updateCandidate(CandidateInfo candidateinfo) {
		namedParameterJdbcTemplate.update(updatecandidate, paramMap(candidateinfo));
	}

	@Override
	public List<CandidateInfo> getAllCandidate() {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		return namedParameterJdbcTemplate.query(allcandidate, paramMap, new CandidateMapper());
	}

	@Override
	public CandidateInfo findCandidateById(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		// A list, not queryForObject: whether candidateid is unique in production is unknown (G22)
		List<CandidateInfo> list = namedParameterJdbcTemplate.query(findcandidatebyid, paramMap,
				new CandidateMapper());
		return list.isEmpty() ? null : list.get(0);
	}

	/** The id (null for a new candidate) and names trimmed; the names are stored as typed, not upper-cased. */
	private static Map<String, Object> paramMap(CandidateInfo candidateinfo) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateinfo.getCandidateid() == null ? null : candidateinfo.getCandidateid().trim());
		paramMap.put("firstname", candidateinfo.getFirstname().trim());
		paramMap.put("lastname", candidateinfo.getLastname().trim());
		paramMap.put("positionkey", candidateinfo.getPositionkey());
		paramMap.put("languagekey", candidateinfo.getLanguagekey());
		return paramMap;
	}

}
