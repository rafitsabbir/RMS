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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import rms.model.OfferInfo;

/** Offers (F8). The salary band is read and written here only; it is never logged. */
@Repository
public class OfferDaoImpl implements OfferDao {

	// The maker's name by subquery, so a missing or duplicated admin row doesn't drop or repeat an offer
	private String selectoffer = "select o.offerkey, o.candidateid, o.offerdate, o.salaryband, o.startdate, o.status, "
			+ "o.respondeddate, o.note, o.createdby, o.createdat, (select min(concat_ws(' ', a.firstname, a.lastname)) "
			+ "from admin a where a.userid=o.createdby) as createdbyname from candidate_offer o where o.isactive=1";
	private String offersbycandidate = selectoffer + " and o.candidateid=:candidateid order by o.offerkey desc";
	private String findoffer = selectoffer + " and o.offerkey=:offerkey";
	private String addoffer = "insert into candidate_offer (candidateid, offerdate, salaryband, startdate, status, "
			+ "respondeddate, note, createdby, createdat, isactive) values (:candidateid, :offerdate, :salaryband, "
			+ ":startdate, :status, :respondeddate, :note, :userid, now(), 1)";
	// Only a pending offer changes; an answered one stays as it was
	private String updateoffer = "update candidate_offer set offerdate=:offerdate, salaryband=:salaryband, "
			+ "startdate=:startdate, status=:status, respondeddate=:respondeddate, note=:note, updatedby=:userid, "
			+ "updatedat=now() where offerkey=:offerkey and candidateid=:candidateid and isactive=1 and status='PENDING'";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class OfferMapper implements RowMapper<OfferInfo> {
		@Override
		public OfferInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			OfferInfo offer = new OfferInfo();
			offer.setOfferkey(rs.getInt("offerkey"));
			offer.setCandidateid(rs.getString("candidateid"));
			offer.setOfferdate(rs.getObject("offerdate", LocalDate.class));
			offer.setSalaryband(rs.getString("salaryband"));
			offer.setStartdate(rs.getObject("startdate", LocalDate.class));
			offer.setStatus(rs.getString("status"));
			offer.setRespondeddate(rs.getObject("respondeddate", LocalDate.class));
			offer.setNote(rs.getString("note"));
			offer.setCreatedby(rs.getString("createdby"));
			offer.setCreatedat(rs.getObject("createdat", LocalDateTime.class));
			offer.setCreatedbyname(rs.getString("createdbyname"));
			return offer;
		}
	}

	@Override
	public List<OfferInfo> getOffers(String candidateid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", candidateid);
		return namedParameterJdbcTemplate.query(offersbycandidate, paramMap, new OfferMapper());
	}

	@Override
	public OfferInfo findOffer(int offerkey) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("offerkey", offerkey);
		List<OfferInfo> list = namedParameterJdbcTemplate.query(findoffer, paramMap, new OfferMapper());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public int addOffer(OfferInfo offer, String userid) {
		KeyHolder keys = new GeneratedKeyHolder();
		namedParameterJdbcTemplate.update(addoffer, new MapSqlParameterSource(paramMap(offer, userid)), keys,
				new String[] {"offerkey"});
		Number key = keys.getKey();
		return key == null ? 0 : key.intValue();
	}

	@Override
	public boolean updateOffer(OfferInfo offer, String userid) {
		Map<String, Object> paramMap = paramMap(offer, userid);
		paramMap.put("offerkey", offer.getOfferkey());
		return namedParameterJdbcTemplate.update(updateoffer, paramMap) == 1;
	}

	private static Map<String, Object> paramMap(OfferInfo offer, String userid) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("candidateid", offer.getCandidateid());
		paramMap.put("offerdate", offer.getOfferdate());
		paramMap.put("salaryband", offer.getSalaryband());
		paramMap.put("startdate", offer.getStartdate());
		paramMap.put("status", offer.getStatus());
		paramMap.put("respondeddate", offer.getRespondeddate());
		paramMap.put("note", offer.getNote());
		paramMap.put("userid", userid);
		return paramMap;
	}
}
