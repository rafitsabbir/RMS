package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.AssignmentInfo;
import rms.model.UserInfo;

/**
 * Characterization tests against db/schema.sql + db/test-seed.sql. Assignments: 1 (C1, U2), 2 (C1, U5, an inactive
 * interviewer), 3 (C2, U2), 4 (C2, U5, unassigned). Users: U2 the only active interviewer; U5 inactive.
 */
class AssignmentDaoImplTest extends MySqlContainerSupport {

	AssignmentDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new AssignmentDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void assignmentsAreTheActiveOnesWithNameAndFlags() {
		assertThat(dao.getAssignments("C1"))
				.extracting(AssignmentInfo::getInterviewerid, AssignmentInfo::getInterviewername,
						AssignmentInfo::isIntervieweractive, AssignmentInfo::isEvaluated, AssignmentInfo::getAssignedlabel)
				.containsExactly(tuple("U2", "Ivan Interviewer", true, true, "2026-09-04 09:00"),
						// Deactivated: shown as "interviewer inactive" (addition 1), the assignment stays
						tuple("U5", "Fay Former", false, true, "2026-09-04 09:05"));
		assertThat(dao.getAssignments("C2")).extracting(AssignmentInfo::getInterviewerid).containsExactly("U2");
	}

	@Test
	void anInterviewerWhoseRoleChangedCountsAsInactive() {
		jdbcTemplate.update("update admin set role='HR' where userid='U2'");

		assertThat(dao.getAssignments("C1")).extracting(AssignmentInfo::isIntervieweractive).containsExactly(false,
				false);
	}

	@Test
	void isAssignedIsTheActiveAssignmentOnly() {
		assertThat(dao.isAssigned("C1", "U2")).isTrue();
		assertThat(dao.isAssigned("C2", "U5")).isFalse();
		assertThat(dao.isAssigned("C1", "U3")).isFalse();
	}

	@Test
	void assignableAreActiveInterviewersNotAssignedYet() {
		assertThat(dao.getAssignableInterviewers("C1")).isEmpty();
		jdbcTemplate.update("update candidate_interviewer set isactive=0 where assignmentkey=1");
		assertThat(dao.getAssignableInterviewers("C1")).extracting(UserInfo::getUserid).containsExactly("U2");
		// The isinterviewer fallback for a NULL role (rms.model.Role.of)
		jdbcTemplate.update("insert into admin (userid, isactive, firstname, lastname, isinterviewer) "
				+ "values ('U7', 1, 'Lee', 'Legacy', 'y')");
		assertThat(dao.getAssignableInterviewers("C1")).extracting(UserInfo::getUserid).containsExactly("U2", "U7");
		// Neither HR nor an inactive interviewer
		assertThat(dao.getAssignableInterviewers("C1")).extracting(UserInfo::getUserid).doesNotContain("U3", "U5");
	}

	@Test
	void assignAddsAnActiveRowOnce() {
		assertThat(dao.assign("C2", "U5", "U3")).isTrue();
		assertThat(dao.assign("C2", "U5", "U3")).isFalse();

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select * from candidate_interviewer where candidateid='C2' and interviewerid='U5' and isactive=1");
		assertThat(row.get("assignedby")).isEqualTo("U3");
		assertThat(row.get("assignedat")).isNotNull();
		assertThat(jdbcTemplate.queryForObject("select count(*) from candidate_interviewer where candidateid='C2' "
				+ "and interviewerid='U5'", Integer.class)).isEqualTo(2);
	}

	@Test
	void unassignKeepsTheRowAndTheEvaluation() {
		assertThat(dao.unassign("C1", "U2", "U1")).isTrue();
		assertThat(dao.unassign("C1", "U2", "U1")).isFalse();

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from candidate_interviewer where assignmentkey=1");
		assertThat(row.get("isactive")).isEqualTo(0);
		assertThat(row.get("unassignedby")).isEqualTo("U1");
		assertThat(row.get("unassignedat")).isNotNull();
		assertThat(jdbcTemplate.queryForObject("select count(*) from marks where candidateid='C1' and interviewerid='U2'",
				Integer.class)).isEqualTo(1);
	}

	@Test
	void myCandidatesAreAssignedOrEvaluatedOnes() {
		assertThat(dao.getMyCandidates("U2"))
				.extracting(AssignmentInfo::getCandidateid, AssignmentInfo::isAssigned, AssignmentInfo::isEvaluated,
						AssignmentInfo::getCandidatestatus, AssignmentInfo::getPositionname)
				.containsExactly(tuple("C1", true, true, "S", "SOFTWARE ENGINEER"),
						tuple("C2", true, true, "R", "QA ENGINEER"));
		AssignmentInfo first = dao.getMyCandidates("U2").get(0);
		assertThat(first.getCandidatefirstname()).isEqualTo("Carla");
		assertThat(first.isLocked()).isTrue();

		// Unassigned but evaluated: still listed, as no longer assigned
		jdbcTemplate.update("update candidate_interviewer set isactive=0 where assignmentkey=3");
		assertThat(dao.getMyCandidates("U2")).extracting(AssignmentInfo::getCandidateid, AssignmentInfo::isAssigned)
				.containsExactly(tuple("C1", true), tuple("C2", false));
		// Deleted candidates aren't listed
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C1'");
		assertThat(dao.getMyCandidates("U2")).extracting(AssignmentInfo::getCandidateid).containsExactly("C2");
		assertThat(dao.getMyCandidates("U3")).isEmpty();
	}
}
