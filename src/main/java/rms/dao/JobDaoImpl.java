package rms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import rms.model.JobInfo;

@Repository
public class JobDaoImpl implements JobDao {

	// Left join: a job still lists when its position is deleted (soft delete) or missing
	private String selectjob = "select j.jobkey, j.positionkey, j.vacancies, j.closingdate, j.status, p.positionname "
			+ "from job j left join position p on p.positionkey=j.positionkey";
	private String alljob = selectjob + " where j.isactive=1 order by j.jobkey";
	private String openjob = selectjob + " where j.isactive=1 and j.status='OPEN' order by j.jobkey";
	private String findjobbyid = selectjob + " where j.jobkey=:jobkey and j.isactive=1";
	private String savejob = "insert into job (positionkey, vacancies, closingdate, status, isactive) "
			+ "values (:positionkey, :vacancies, :closingdate, :status, 1)";
	private String updatejob = "update job set positionkey=:positionkey, vacancies=:vacancies, "
			+ "closingdate=:closingdate, status=:status where jobkey=:jobkey and isactive=1";
	// Soft delete: candidates keep candidate.jobkey, and their profile shows the job as deleted
	private String deletejob = "update job set isactive=0 where jobkey=:jobkey";

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate)
			throws DataAccessException {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static final class JobMapper implements RowMapper<JobInfo> {

		public JobInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			JobInfo job = new JobInfo();
			job.setJobkey(rs.getInt("jobkey"));
			job.setPositionkey(rs.getInt("positionkey"));
			job.setPositionname(rs.getString("positionname"));
			job.setVacancies(rs.getInt("vacancies"));
			job.setClosingdate(rs.getObject("closingdate", LocalDate.class));
			job.setStatus(rs.getString("status"));
			return job;
		}
	}

	@Override
	public void addJob(JobInfo jobinfo) {
		namedParameterJdbcTemplate.update(savejob, paramMap(jobinfo));
	}

	@Override
	public void updateJob(JobInfo jobinfo) {
		namedParameterJdbcTemplate.update(updatejob, paramMap(jobinfo));
	}

	@Override
	public List<JobInfo> getAllJob() {
		return namedParameterJdbcTemplate.query(alljob, new HashMap<String, Object>(), new JobMapper());
	}

	@Override
	public List<JobInfo> getOpenJob() {
		return namedParameterJdbcTemplate.query(openjob, new HashMap<String, Object>(), new JobMapper());
	}

	@Override
	public JobInfo findJobById(int jobkey) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("jobkey", jobkey);
		List<JobInfo> list = namedParameterJdbcTemplate.query(findjobbyid, paramMap, new JobMapper());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public void deleteJob(int jobkey) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("jobkey", jobkey);
		namedParameterJdbcTemplate.update(deletejob, paramMap);
	}

	private static Map<String, Object> paramMap(JobInfo jobinfo) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("jobkey", jobinfo.getJobkey());
		paramMap.put("positionkey", jobinfo.getPositionkey());
		paramMap.put("vacancies", jobinfo.getVacancies());
		paramMap.put("closingdate", jobinfo.getClosingdate());
		paramMap.put("status", jobinfo.getStatus());
		return paramMap;
	}

}
