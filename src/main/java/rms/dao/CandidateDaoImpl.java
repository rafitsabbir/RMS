package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
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
import rms.model.DocumentType;

@Repository
public class CandidateDaoImpl implements CandidateDao {
	private static final Logger log = LoggerFactory.getLogger(CandidateDaoImpl.class);

	// The single-slot kinds' names, the :slottypes list of the slotcount subquery
	private static final List<String> SLOT_TYPES = Arrays.stream(DocumentType.values())
			.filter(DocumentType::isSingleSlot).map(DocumentType::name).toList();

	// Left joins: a candidate still lists when its position, language or job is deleted (soft delete) or missing.
	// The job's status only when the job is active. The document counts are of active documents: CV, how many
	// of the six single-slot types (CV included), and professional certificates
	private String selectcandidate = "select c.candidateid, c.firstname, c.lastname, c.positionkey, c.languagekey, "
			+ "c.candidatestatus, c.email, c.phone, c.source, c.applieddate, c.jobkey, p.positionname, l.languagename, "
			+ "(case when j.isactive=1 then j.status end) as jobstatus, "
			+ "(select count(*) from candidate_document d where d.candidateid=c.candidateid and d.isactive=1 "
			+ "and d.doctype='CV') as cvcount, "
			+ "(select count(distinct d.doctype) from candidate_document d where d.candidateid=c.candidateid "
			+ "and d.isactive=1 and d.doctype in (:slottypes)) as slotcount, "
			+ "(select count(*) from candidate_document d where d.candidateid=c.candidateid and d.isactive=1 "
			+ "and d.doctype='PROFESSIONAL') as professionalcount "
			+ "from candidate c left join position p on p.positionkey=c.positionkey "
			+ "left join language l on l.languagekey=c.languagekey left join job j on j.jobkey=c.jobkey";
	// Active candidates only (deleted ones keep isactive=0); shorter IDs first, so C2 comes before C10
	private String allcandidate = selectcandidate + " where c.isactive=1 order by length(c.candidateid), c.candidateid";
	private String findcandidatebyid = selectcandidate + " where c.candidateid=:candidateid and c.isactive=1";
	// The highest number among IDs of the form C<number> (up to 9 digits), deleted ones included so an ID is
	// never reused; 0 when there is none
	private String lastnumber = "select coalesce(max(cast(substring(candidateid, 2) as unsigned)), 0) from candidate "
			+ "where candidateid regexp '^C[0-9]{1,9}$'";
	private String savecandidate = "insert into candidate "
			+ "(isactive, candidateid, firstname, lastname, positionkey, languagekey, email, phone, source, applieddate, "
			+ "jobkey) VALUES (1, :candidateid, :firstname, :lastname, :positionkey, :languagekey, :email, :phone, "
			+ ":source, :applieddate, :jobkey)";
	private String updatecandidate = "update candidate set firstname=:firstname, lastname=:lastname, "
			+ "positionkey=:positionkey, languagekey=:languagekey, email=:email, phone=:phone, source=:source, "
			+ "applieddate=:applieddate, jobkey=:jobkey where candidateid=:candidateid and isactive=1";
	// Soft delete (owner decision 2026-10-02): the row and its marks stay
	private String deletecandidate = "update candidate set isactive=0 where candidateid=:candidateid";

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
			candidate.setEmail(rs.getString("email"));
			candidate.setPhone(rs.getString("phone"));
			candidate.setSource(rs.getString("source"));
			candidate.setApplieddate(rs.getObject("applieddate", LocalDate.class));
			// NULL (no job) reads as 0
			candidate.setJobkey(rs.getInt("jobkey"));
			candidate.setJobstatus(rs.getString("jobstatus"));
			candidate.setCvcount(rs.getInt("cvcount"));
			candidate.setSlotcount(rs.getInt("slotcount"));
			candidate.setProfessionalcount(rs.getInt("professionalcount"));
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
		paramMap.put("slottypes", SLOT_TYPES);
		return namedParameterJdbcTemplate.query(allcandidate, paramMap, new CandidateMapper());
	}

	@Override
	public CandidateInfo findCandidateById(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		paramMap.put("slottypes", SLOT_TYPES);
		// A list, not queryForObject: whether candidateid is unique in production is unknown (G22)
		List<CandidateInfo> list = namedParameterJdbcTemplate.query(findcandidatebyid, paramMap,
				new CandidateMapper());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public void deleteCandidate(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);

		namedParameterJdbcTemplate.update(deletecandidate, paramMap);
	}

	/**
	 * The id (null for a new candidate) and names trimmed; the names are stored as typed, not upper-cased. Empty
	 * optional fields and no job (0) are stored as NULL.
	 */
	private static Map<String, Object> paramMap(CandidateInfo candidateinfo) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateinfo.getCandidateid() == null ? null : candidateinfo.getCandidateid().trim());
		paramMap.put("firstname", candidateinfo.getFirstname().trim());
		paramMap.put("lastname", candidateinfo.getLastname().trim());
		paramMap.put("positionkey", candidateinfo.getPositionkey());
		paramMap.put("languagekey", candidateinfo.getLanguagekey());
		paramMap.put("email", blankToNull(candidateinfo.getEmail()));
		paramMap.put("phone", blankToNull(candidateinfo.getPhone()));
		paramMap.put("source", blankToNull(candidateinfo.getSource()));
		paramMap.put("applieddate", candidateinfo.getApplieddate());
		paramMap.put("jobkey", candidateinfo.getJobkey() > 0 ? candidateinfo.getJobkey() : null);
		return paramMap;
	}

	private static String blankToNull(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}

}
