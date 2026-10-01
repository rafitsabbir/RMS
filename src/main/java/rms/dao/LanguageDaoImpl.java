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


import rms.model.LanguageInfo;

@Repository
public class LanguageDaoImpl implements LanguageDao {
	private static final Logger log = LoggerFactory.getLogger(LanguageDaoImpl.class);

	private String savelanguage = "insert into language  (isActive, languagename) VALUES (:isActive,:languagename)";
	private String ifexist = "select count(*) from language where languagename=:languagename and isactive=1";
	private String ifexistother = "select count(*) from language where languagename=:languagename and languagekey<>:languagekey and isactive=1";
	private String reactivatelanguage = "update language set isactive=1 where languagename=:languagename and isactive=0 limit 1";
	private String alllanguage = "select languagekey, languagename from language where  isactive=1";
	private String findlanguagebyid = "select languagekey, languagename from language where languagekey=:languagekey and isactive=1";
	private String updatelanguage = "update language set languagename=:languagename where languagekey=:languagekey";
	private String deletelanguage = "update language set isactive=0 where languagekey=:languagekey";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class LanguageMapper implements
			RowMapper<LanguageInfo> {
		public LanguageInfo mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			LanguageInfo language = new LanguageInfo();
			language.setLanguagekey(rs.getInt("languagekey"));
			language.setLanguagename(rs.getString("languagename"));
			return language;
		}
	}

	@Override
	public List<LanguageInfo> getAllLanguage() {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		List<LanguageInfo> list = namedParameterJdbcTemplate.query(alllanguage,
				paramMap, new LanguageMapper());
		return list;
	}

	@Override
	public boolean addLanguage(LanguageInfo languageinfo) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("languagename", languageinfo.getLanguagename()
				.toUpperCase().trim());

		if (namedParameterJdbcTemplate.queryForObject(ifexist, paramMap, Integer.class) > 0) {
			log.warn("Language {} already exists; not added", paramMap.get("languagename"));
			return false;
		}
		// A deleted language of the same name comes back rather than a second row being added
		if (namedParameterJdbcTemplate.update(reactivatelanguage, paramMap) == 0) {
			paramMap.put("isActive", 1);
			namedParameterJdbcTemplate.update(savelanguage, paramMap);
		}
		return true;
	}

	@Override
	public LanguageInfo findLanguageById(int languagekey) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("languagekey", languagekey);
		try {
			return namedParameterJdbcTemplate.queryForObject(findlanguagebyid, paramMap, new LanguageMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public boolean updateLanguage(LanguageInfo languageinfo) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("languagename", languageinfo.getLanguagename()
				.toUpperCase().trim());
		paramMap.put("languagekey", languageinfo.getLanguagekey());

		if (namedParameterJdbcTemplate.queryForObject(ifexistother, paramMap, Integer.class) > 0) {
			log.warn("Language {} already exists; not renamed", paramMap.get("languagename"));
			return false;
		}
		namedParameterJdbcTemplate.update(updatelanguage, paramMap);
		return true;
	}

	@Override
	public void deleteLanguage(int languagekey) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("languagekey", languagekey);
		
		namedParameterJdbcTemplate.update(deletelanguage, paramMap);
	}

}
