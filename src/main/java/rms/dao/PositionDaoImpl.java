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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import rms.model.PositionInfo;

@Repository
public class PositionDaoImpl implements PositionDao {
	private static final Logger log = LoggerFactory.getLogger(PositionDaoImpl.class);

	private String ifexist = "select count(*) from position where positionname=:positionname and isactive=1";
	private String ifexistother = "select count(*) from position where positionname=:positionname and positionkey<>:positionkey and isactive=1";
	private String reactivateposition = "update position set isactive=1 where positionname=:positionname and isactive=0 limit 1";
	private String saveposition = "insert into position  (isActive, positionname) VALUES (:isActive,:positionname)";
	private String updateposition = "update position set positionname=:positionname where positionkey=:positionkey";
	private String allposition = "select positionkey, positionname from position where  isactive=1";
	private String deleteposition = "update position set isactive=0 where positionkey=:positionkey";
	private String findpositionbyid = "select positionkey, positionname from position where positionkey=:positionkey and isactive=1";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class PositionMapper implements RowMapper<PositionInfo> {
		
		public PositionInfo mapRow(ResultSet rs, int rowNum)throws SQLException {
			PositionInfo position = new PositionInfo();
			position.setPositionkey(rs.getInt("positionkey"));
			position.setPositionname(rs.getString("positionname"));
			return position;
		}
	}

	@Override
	public boolean updatePosition(PositionInfo positioninfo) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("positionname", positioninfo.getPositionname()
				.toUpperCase().trim());
		paramMap.put("positionkey", positioninfo.getPositionkey());

		if (namedParameterJdbcTemplate.queryForObject(ifexistother, paramMap, Integer.class) > 0) {
			log.warn("Position {} already exists; not renamed", paramMap.get("positionname"));
			return false;
		}
		namedParameterJdbcTemplate.update(updateposition, paramMap);
		return true;
	}

	@Override
	public boolean addPosition(PositionInfo positioninfo) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("positionname", positioninfo.getPositionname()
				.toUpperCase().trim());

		if (namedParameterJdbcTemplate.queryForObject(ifexist, paramMap, Integer.class) > 0) {
			log.warn("Position {} already exists; not added", paramMap.get("positionname"));
			return false;
		}
		// A deleted position of the same name comes back rather than a second row being added
		if (namedParameterJdbcTemplate.update(reactivateposition, paramMap) == 0) {
			paramMap.put("isActive", 1);
			namedParameterJdbcTemplate.update(saveposition, paramMap);
		}
		return true;
	}

	@Override
	public List<PositionInfo> getAllPosition() {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		List<PositionInfo> list = namedParameterJdbcTemplate.query(allposition,
				paramMap, new PositionMapper());
		return list;
	}
	
	@Override
	public PositionInfo findPositionById(int positionkey) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("positionkey", positionkey);
		try {
			return namedParameterJdbcTemplate.queryForObject(findpositionbyid, paramMap, new PositionMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	

	@Override
	public void deletePosition(int positionkey) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("positionkey", positionkey);
		
		namedParameterJdbcTemplate.update(deleteposition, paramMap);
	}

}
