package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.JobInfo;

/** Characterization tests against db/schema.sql + db/test-seed.sql (jobs 1 open, 2 closed, 3 deleted). */
class JobDaoImplTest extends MySqlContainerSupport {

	JobDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new JobDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void listReturnsActiveJobsWithTheirPosition() {
		assertThat(dao.getAllJob())
				.extracting(JobInfo::getJobkey, JobInfo::getPositionname, JobInfo::getVacancies,
						JobInfo::getClosingdate, JobInfo::getStatus)
				.containsExactly(
						tuple(1, "SOFTWARE ENGINEER", 2, LocalDate.of(2030, 12, 31), "OPEN"),
						tuple(2, "QA ENGINEER", 1, LocalDate.of(2026, 1, 31), "CLOSED"));
	}

	@Test
	void openJobsAreTheActiveOpenOnes() {
		assertThat(dao.getOpenJob()).extracting(JobInfo::getJobkey).containsExactly(1);
	}

	@Test
	void findReturnsActiveJobsOpenOrClosed() {
		assertThat(dao.findJobById(2).getStatus()).isEqualTo("CLOSED");
		assertThat(dao.findJobById(3)).isNull();
		assertThat(dao.findJobById(99)).isNull();
	}

	@Test
	void aJobKeepsADeletedPosition() {
		jdbcTemplate.update("update job set positionkey=3 where jobkey=1");

		assertThat(dao.findJobById(1).getPositionname()).isEqualTo("RETIRED ROLE");
	}

	@Test
	void addSavesAnActiveJob() {
		dao.addJob(job(0, 2, 4, LocalDate.of(2031, 1, 15), "OPEN"));
		dao.addJob(job(0, 1, 1, null, "CLOSED"));

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from job where jobkey=4");
		assertThat(row.get("positionkey")).isEqualTo(2);
		assertThat(row.get("vacancies")).isEqualTo(4);
		assertThat(row.get("closingdate")).isEqualTo(java.sql.Date.valueOf("2031-01-15"));
		assertThat(row.get("status")).isEqualTo("OPEN");
		assertThat(row.get("isactive")).isEqualTo(1);
		assertThat(dao.findJobById(5).getClosingdate()).isNull();
	}

	@Test
	void updateChangesAnActiveJobOnly() {
		dao.updateJob(job(1, 2, 5, null, "CLOSED"));
		dao.updateJob(job(3, 2, 5, null, "CLOSED"));

		JobInfo job = dao.findJobById(1);
		assertThat(job.getPositionkey()).isEqualTo(2);
		assertThat(job.getVacancies()).isEqualTo(5);
		assertThat(job.getClosingdate()).isNull();
		assertThat(job.getStatus()).isEqualTo("CLOSED");
		assertThat(jdbcTemplate.queryForObject("select vacancies from job where jobkey=3", Integer.class)).isEqualTo(1);
	}

	@Test
	void deleteIsSoftAndCandidatesKeepTheLink() {
		dao.deleteJob(1);

		assertThat(dao.findJobById(1)).isNull();
		assertThat(jdbcTemplate.queryForObject("select isactive from job where jobkey=1", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("select jobkey from candidate where candidateid='C1'", Integer.class))
				.isEqualTo(1);
	}

	private static JobInfo job(int jobkey, int positionkey, int vacancies, LocalDate closingdate, String status) {
		JobInfo job = new JobInfo();
		job.setJobkey(jobkey);
		job.setPositionkey(positionkey);
		job.setVacancies(vacancies);
		job.setClosingdate(closingdate);
		job.setStatus(status);
		return job;
	}
}
