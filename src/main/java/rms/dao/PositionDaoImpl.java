package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import rms.model.PositionInfo;

@Repository
public class PositionDaoImpl implements PositionDao {
	private String ifexist = "select positionname from position where positionname=:positionname ";
	private String saveposition = "insert into position  (isActive, positionname) VALUES (:isActive,:positionname)";
	private String updateposition = "update position set positionname=:positionname where positionkey=:positionkey";
	private String allposition = "select positionkey, positionname from position where  isactive=1";
	private String deleteposition = "delete from position where positionkey=:positionkey";
	private String findpositionbyid = "select positionkey, positionname from position where positionkey=:positionkey";

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
	public void updatePosition(PositionInfo positioninfo) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("positionname", positioninfo.getPositionname()
				.toUpperCase().trim());
		paramMap.put("positionkey", positioninfo.getPositionkey());
		namedParameterJdbcTemplate.update(updateposition, paramMap);
	}

	@Override
	public void addPosition(PositionInfo positioninfo) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("positionname", positioninfo.getPositionname()
				.toUpperCase().trim());

		try {
			String positionname = namedParameterJdbcTemplate.queryForObject(
					ifexist, paramMap, String.class);

			System.out.println(positionname + "already exist!");

		} catch (EmptyResultDataAccessException e) {
			paramMap.put("isActive", 1);
			namedParameterJdbcTemplate.update(saveposition, paramMap);
		}
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
		return namedParameterJdbcTemplate.queryForObject(findpositionbyid, paramMap, new PositionMapper());
	}
	

	@Override
	public void deletePosition(int positionkey) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("positionkey", positionkey);
		
		namedParameterJdbcTemplate.update(deleteposition, paramMap);
	}

}
