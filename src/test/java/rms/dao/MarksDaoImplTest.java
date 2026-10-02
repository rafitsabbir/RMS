package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.Criterion;
import rms.model.MarksInfo;
import rms.model.ResultInfo;

/**
 * Characterization tests against db/schema.sql + db/test-seed.sql. The seed's marks: 1 (U2, C1), 2 (U2, C2), 3 (U5,
 * C1, with comments; U5 is inactive) and 4 (U5, C2, isactive 0). U2 is assigned to C1 and C2, U5 to C1 only. C1 is
 * Selected and C2 Rejected, so both are locked for saving; the save tests set a status first.
 */
class MarksDaoImplTest extends MySqlContainerSupport {

	MarksDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new MarksDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void resultsAverageTheActiveEvaluationsPerCandidate() {
		List<ResultInfo> results = dao.getResults();

		assertThat(results).extracting(ResultInfo::getCandidateid, ResultInfo::getEvaluations,
				ResultInfo::getPositionname, ResultInfo::getCandidatestatus)
				.containsExactly(tuple("C1", 2, "SOFTWARE ENGINEER", "S"), tuple("C2", 1, "QA ENGINEER", "R"));
		ResultInfo carla = results.get(0);
		assertThat(carla.getRoundedaverages()).extracting(BigDecimal::toPlainString)
				.containsExactly("7.0", "8.0", "7.5", "7.0", "8.0", "7.0", "7.5", "7.5", "8.0", "7.0");
		assertThat(carla.getTotal()).isEqualByComparingTo("74.5");
		// The inactive row 4 (all 10s) doesn't count
		assertThat(results.get(1).getTotal()).isEqualByComparingTo("33.0");
	}

	@Test
	void resultsHideDeletedCandidatesAndShowOnesWithoutEvaluations() {
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C2'");
		jdbcTemplate.update("insert into candidate (candidateid, firstname, lastname, positionkey, languagekey) "
				+ "values ('C3', 'Dana', 'Doe', 99, 1)");

		List<ResultInfo> results = dao.getResults();

		assertThat(results).extracting(ResultInfo::getCandidateid).containsExactly("C1", "C3");
		ResultInfo dana = results.get(1);
		assertThat(dana.getEvaluations()).isZero();
		assertThat(dana.getAverages()).containsOnlyNulls();
		assertThat(dana.getTotal()).isNull();
		// A missing position still lists the candidate (left join)
		assertThat(dana.getPositionname()).isNull();
	}

	@Test
	void evaluationsAreReadByNameWithTheInterviewerAndFlags() {
		// G10 fixed: names and scores in their own fields, read by column name
		List<MarksInfo> evaluations = dao.getEvaluations("C1");

		assertThat(evaluations).extracting(MarksInfo::getMarkkey, MarksInfo::getInterviewerid,
				MarksInfo::getInterviewername, MarksInfo::isIntervieweractive, MarksInfo::isAssigned)
				.containsExactly(tuple(1, "U2", "Ivan Interviewer", true, true),
						tuple(3, "U5", "Fay Former", false, true));
		MarksInfo first = evaluations.get(0);
		assertThat(first.getCandidateid()).isEqualTo("C1");
		assertThat(first.getScores()).containsExactly(8, 9, 7, 8, 9, 6, 8, 7, 9, 8);
		assertThat(first.getTotal()).isEqualTo(79);
		assertThat(first.getUpdatedlabel()).isEmpty();
		MarksInfo second = evaluations.get(1);
		assertThat(second.getComments()).isEqualTo("Calm and structured.");
		assertThat(second.getComment(Criterion.TECHKNOWLEDGE)).isEqualTo("Good Java basics.");
		assertThat(second.getComment(Criterion.WORKEXP)).isNull();
		assertThat(second.getUpdatedlabel()).isEqualTo("2026-09-12 15:30");
	}

	@Test
	void unassignedInterviewersEvaluationStillShows() {
		jdbcTemplate.update("update candidate_interviewer set isactive=0 where assignmentkey=1");

		assertThat(dao.getEvaluations("C1")).extracting(MarksInfo::getInterviewerid, MarksInfo::isAssigned)
				.containsExactly(tuple("U2", false), tuple("U5", true));
	}

	@Test
	void findReturnsTheOldestActiveEvaluationOrNull() {
		assertThat(dao.findEvaluation("C1", "U2").getMarkkey()).isEqualTo(1);
		assertThat(dao.findEvaluation("C2", "U5")).isNull();
		assertThat(dao.findEvaluation("C1", "U3")).isNull();
	}

	@Test
	void saveUpdatesTheExistingEvaluation() {
		jdbcTemplate.update("update candidate set candidatestatus='H' where candidateid='C1'");
		MarksInfo marks = evaluation("C1", "U2", 5);
		marks.setComments("Second look.");
		marks.setComment(Criterion.STRESS, "Calm.");

		assertThat(dao.saveMarks(marks)).isEqualTo(1);

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from marks where markkey=1");
		assertThat(row.get("workexp")).isEqualTo(5);
		assertThat(row.get("personality")).isEqualTo(5);
		assertThat(row.get("comments")).isEqualTo("Second look.");
		assertThat(row.get("stresscomment")).isEqualTo("Calm.");
		assertThat(row.get("updatedat")).isNotNull();
		assertThat(jdbcTemplate.queryForObject("select count(*) from marks", Integer.class)).isEqualTo(4);
	}

	@Test
	void saveInsertsAFirstEvaluation() {
		jdbcTemplate.update("update candidate set candidatestatus=null where candidateid='C2'");
		jdbcTemplate.update("update candidate_interviewer set isactive=1 where assignmentkey=4");

		int markkey = dao.saveMarks(evaluation("C2", "U5", 6));

		assertThat(markkey).isEqualTo(5);
		Map<String, Object> row = jdbcTemplate.queryForMap("select * from marks where markkey=5");
		assertThat(row.get("isactive")).isEqualTo(1);
		assertThat(row.get("interviewerid")).isEqualTo("U5");
		assertThat(row.get("candidateid")).isEqualTo("C2");
		assertThat(row.get("attitude")).isEqualTo(6);
		assertThat(row.get("createdat")).isNotNull();
		// The inactive row 4 is left alone
		assertThat(jdbcTemplate.queryForObject("select workexp from marks where markkey=4", Integer.class))
				.isEqualTo(10);
	}

	@Test
	void saveIsRefusedWhenDecidedUnassignedOrDeleted() {
		// C1 is Selected (seed): locked
		assertThat(dao.saveMarks(evaluation("C1", "U2", 5))).isZero();
		// Rejected in lower case is final too
		jdbcTemplate.update("update candidate set candidatestatus='r' where candidateid='C1'");
		assertThat(dao.saveMarks(evaluation("C1", "U2", 5))).isZero();

		jdbcTemplate.update("update candidate set candidatestatus='H' where candidateid='C1'");
		// U3 isn't assigned
		assertThat(dao.saveMarks(evaluation("C1", "U3", 5))).isZero();
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C1'");
		assertThat(dao.saveMarks(evaluation("C1", "U2", 5))).isZero();

		assertThat(jdbcTemplate.queryForObject("select workexp from marks where markkey=1", Integer.class))
				.isEqualTo(8);
	}

	private static MarksInfo evaluation(String candidateid, String interviewerid, int score) {
		MarksInfo marks = new MarksInfo();
		marks.setCandidateid(candidateid);
		marks.setInterviewerid(interviewerid);
		for (Criterion criterion : Criterion.values()) {
			marks.setScore(criterion, score);
		}
		return marks;
	}

}
