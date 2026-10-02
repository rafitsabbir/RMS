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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import rms.model.DocumentInfo;

@Repository
public class DocumentDaoImpl implements DocumentDao {

	// The uploader's name by subquery, so a missing or duplicated admin row doesn't drop or repeat a document
	private String selectdocument = "select d.documentkey, d.candidateid, d.doctype, d.originalname, d.storedname, "
			+ "d.contenttype, d.filesize, d.title, d.issuer, d.issueyear, d.uploadedby, d.uploadedat, "
			+ "(select min(concat_ws(' ', a.firstname, a.lastname)) from admin a where a.userid=d.uploadedby) "
			+ "as uploadedbyname from candidate_document d";
	private String documentsbycandidate = selectdocument
			+ " where d.candidateid=:candidateid and d.isactive=1 order by d.uploadedat, d.documentkey";
	// Not for a deleted candidate: their documents are out of reach, like their profile
	private String finddocument = selectdocument + " where d.documentkey=:documentkey and d.isactive=1 "
			+ "and exists (select 1 from candidate c where c.candidateid=d.candidateid and c.isactive=1)";
	// Locks the candidate's row until the upload's transaction ends, so two uploads for one candidate (also on two
	// Tomcat nodes) count and insert one after the other. InnoDB; no row (a candidate deleted meanwhile) locks nothing
	private String lockcandidate = "select candidateid from candidate where candidateid=:candidateid for update";
	private String countdocuments ="select count(*) from candidate_document "
			+ "where candidateid=:candidateid and doctype=:doctype and isactive=1";
	private String savedocument = "insert into candidate_document (candidateid, doctype, originalname, storedname, "
			+ "contenttype, filesize, title, issuer, issueyear, uploadedby, uploadedat, isactive) values (:candidateid, "
			+ ":doctype, :originalname, :storedname, :contenttype, :filesize, :title, :issuer, :issueyear, "
			+ ":uploadedby, now(), 1)";
	// The replaced document of a single-slot type; its file stays until a purge
	private String replacedocuments = "update candidate_document set isactive=0, deletedby=:uploadedby, "
			+ "deletedat=now() where candidateid=:candidateid and doctype=:doctype and isactive=1";
	private String deletedocument = "update candidate_document set isactive=0, deletedby=:userid, deletedat=now() "
			+ "where documentkey=:documentkey and isactive=1";
	private String unpurgeddocuments = selectdocument
			+ " where d.candidateid=:candidateid and d.purgedat is null order by d.documentkey";
	// A document deleted earlier keeps its own deletedby and deletedat
	private String markpurged = "update candidate_document set isactive=0, deletedby=coalesce(deletedby, :userid), "
			+ "deletedat=coalesce(deletedat, now()), purgedby=:userid, purgedat=now(), purgereason=:reason "
			+ "where documentkey=:documentkey and purgedat is null";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	TransactionTemplate transactionTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.transactionTemplate = null;
	}

	/** addDocument's transaction, on the template's DataSource; made on first use. */
	private synchronized TransactionTemplate transactions() {
		if (transactionTemplate == null) {
			transactionTemplate = new TransactionTemplate(
					new DataSourceTransactionManager(namedParameterJdbcTemplate.getJdbcTemplate().getDataSource()));
		}
		return transactionTemplate;
	}

	private static final class DocumentMapper implements RowMapper<DocumentInfo> {

		public DocumentInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			DocumentInfo document = new DocumentInfo();
			document.setDocumentkey(rs.getInt("documentkey"));
			document.setCandidateid(rs.getString("candidateid"));
			document.setDoctype(rs.getString("doctype"));
			document.setOriginalname(rs.getString("originalname"));
			document.setStoredname(rs.getString("storedname"));
			document.setContenttype(rs.getString("contenttype"));
			document.setFilesize(rs.getLong("filesize"));
			document.setTitle(rs.getString("title"));
			document.setIssuer(rs.getString("issuer"));
			document.setIssueyear(rs.getObject("issueyear", Integer.class));
			document.setUploadedby(rs.getString("uploadedby"));
			document.setUploadedat(rs.getObject("uploadedat", LocalDateTime.class));
			document.setUploadedbyname(rs.getString("uploadedbyname"));
			return document;
		}
	}

	@Override
	public List<DocumentInfo> getDocuments(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		return namedParameterJdbcTemplate.query(documentsbycandidate, paramMap, new DocumentMapper());
	}

	@Override
	public DocumentInfo findDocument(int documentkey) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("documentkey", documentkey);
		List<DocumentInfo> list = namedParameterJdbcTemplate.query(finddocument, paramMap, new DocumentMapper());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public int countDocuments(String candidateid, String doctype) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		paramMap.put("doctype", doctype);
		return namedParameterJdbcTemplate.queryForObject(countdocuments, paramMap, Integer.class);
	}

	@Override
	// Counting (or replacing) and inserting run in one transaction behind a lock on the candidate's row, as nothing
	// in the table enforces the limits. synchronized too, for one Tomcat even if candidate isn't InnoDB (G22)
	public synchronized int addDocument(DocumentInfo documentinfo, int maxActive) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("candidateid", documentinfo.getCandidateid());
		params.addValue("doctype", documentinfo.getDoctype());
		params.addValue("originalname", documentinfo.getOriginalname());
		params.addValue("storedname", documentinfo.getStoredname());
		params.addValue("contenttype", documentinfo.getContenttype());
		params.addValue("filesize", documentinfo.getFilesize());
		params.addValue("title", documentinfo.getTitle());
		params.addValue("issuer", documentinfo.getIssuer());
		params.addValue("issueyear", documentinfo.getIssueyear());
		params.addValue("uploadedby", documentinfo.getUploadedby());

		Integer documentkey = transactions().execute(status -> {
			namedParameterJdbcTemplate.queryForList(lockcandidate, params, String.class);
			if (maxActive == 1) {
				namedParameterJdbcTemplate.update(replacedocuments, params);
			} else if (namedParameterJdbcTemplate.queryForObject(countdocuments, params, Integer.class) >= maxActive) {
				return 0;
			}
			KeyHolder keys = new GeneratedKeyHolder();
			namedParameterJdbcTemplate.update(savedocument, params, keys, new String[] { "documentkey" });
			return keys.getKey().intValue();
		});
		return documentkey;
	}

	@Override
	public boolean deleteDocument(int documentkey, String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("documentkey", documentkey);
		paramMap.put("userid", userid);
		return namedParameterJdbcTemplate.update(deletedocument, paramMap) == 1;
	}

	@Override
	public List<DocumentInfo> getUnpurgedDocuments(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		return namedParameterJdbcTemplate.query(unpurgeddocuments, paramMap, new DocumentMapper());
	}

	@Override
	public void markPurged(int documentkey, String userid, String reason) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("documentkey", documentkey);
		paramMap.put("userid", userid);
		paramMap.put("reason", reason);
		namedParameterJdbcTemplate.update(markpurged, paramMap);
	}

}
