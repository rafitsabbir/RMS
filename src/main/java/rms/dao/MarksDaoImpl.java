package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import rms.model.MarksInfo;

@Repository
public class MarksDaoImpl implements MarksDao {
	final String getallmarksbyadmin = "select (select concat(a.firstname,' ',a.lastname) from admin a where "
			+ "a.userid=m.interviewerid )as interviewername, (select concat(cc.firstname,' ',"
			+ "cc.lastname) from candidate cc where cc.candidateid=m.candidateid ) as "
			+ "candidatename, p.positionname, l.languagename,m.*,c.candidatestatus from marks m, position p,"
			+ " language l , candidate c where m.candidateid=c.candidateid "
			+ "and c.positionkey=p.positionkey and c.languagekey=l.languagekey "
			+ "order by m.candidateid asc";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class MarksMapper implements RowMapper<MarksInfo> {
		@Override
		public MarksInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			MarksInfo marksinfo = new MarksInfo();
			marksinfo.setInterviewerid(rs.getString(1));
			marksinfo.setCandidateid(rs.getString(2));
			marksinfo.setPosition(rs.getString(3));
			marksinfo.setLanguage(rs.getString(4));
			marksinfo.setIsactive(rs.getInt(5));
			marksinfo.setWorkexp(rs.getInt(8));
			marksinfo.setTechknowledge(rs.getInt(9));
			marksinfo.setLeadership(rs.getInt(10));
			marksinfo.setDecision(rs.getInt(11));
			marksinfo.setProbsolving(rs.getInt(12));
			marksinfo.setStress(rs.getInt(13));
			marksinfo.setEducation(rs.getInt(14));
			marksinfo.setComskill(rs.getInt(15));
			marksinfo.setAttitude(rs.getInt(16));
			marksinfo.setPersonality(rs.getInt(17));
			marksinfo.setCandidateStatus(rs.getString(18));
			return marksinfo;
		}

	}

	@Override
	public void saveMarks(MarksInfo marksinfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getFullmarks(MarksInfo marksinfo) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<MarksInfo>  getAllMarksByInterviewer(String userid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MarksInfo> getAllMarksByAdmin() {
		// TODO Auto-generated method stub
		Map<String, String> paramMap = new HashMap<String, String>();
		return namedParameterJdbcTemplate.query(getallmarksbyadmin, paramMap,
				new MarksMapper());
	}

}
