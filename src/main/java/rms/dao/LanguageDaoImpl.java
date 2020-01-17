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


import rms.model.LanguageInfo;

@Repository
public class LanguageDaoImpl implements LanguageDao {
	private String savelanguage = "insert into language  (isActive, languagename) VALUES (:isActive,:languagename)";
	private String ifexist = "select languagename from language where languagename=:languagename ";
	private String alllanguage = "select languagekey, languagename from language where  isactive=1";
	private String findlanguagebyid = "select languagekey, languagename from language where languagekey=:languagekey";
	private String updatelanguage = "update language set languagename=:languagename where languagekey=:languagekey";
	private String deletelanguage = "delete from language where languagekey=:languagekey";

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
	public void addLanguage(LanguageInfo languageinfo) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("languagename", languageinfo.getLanguagename()
				.toUpperCase().trim());

		try {
			String languagename = namedParameterJdbcTemplate.queryForObject(
					ifexist, paramMap, String.class);

			System.out.println(languagename + "already exist!");

		} catch (EmptyResultDataAccessException e) {
			paramMap.put("isActive", 1);
			namedParameterJdbcTemplate.update(savelanguage, paramMap);
		}

	}

	@Override
	public LanguageInfo findLanguageById(int languagekey) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("languagekey", languagekey);
		return namedParameterJdbcTemplate.queryForObject(findlanguagebyid, paramMap, new LanguageMapper());
	}

	@Override
	public void updateLanguage(LanguageInfo languageinfo) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();

		paramMap.put("languagename", languageinfo.getLanguagename()
				.toUpperCase().trim());
		paramMap.put("languagekey", languageinfo.getLanguagekey());
		namedParameterJdbcTemplate.update(updatelanguage, paramMap);
	}

	@Override
	public void deleteLanguage(int languagekey) {
		// TODO Auto-generated method stub
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("languagekey", languagekey);
		
		namedParameterJdbcTemplate.update(deletelanguage, paramMap);
	}

}
