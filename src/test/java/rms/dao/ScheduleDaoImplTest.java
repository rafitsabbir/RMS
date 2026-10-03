package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.ScheduleInfo;

/**
 * Characterization tests against db/schema.sql + db/test-seed.sql. Interviews: 1 (C1, U2, 2099-01-15 10:00, Room 1,
 * SCHEDULED), 2 (C2, U2, 2026-09-18 14:00, Online, DONE), 3 (C1, U5, 2099-01-16 09:00, no location, CANCELLED).
 */
class ScheduleDaoImplTest extends MySqlContainerSupport {

	static final LocalDateTime SEED_START = LocalDateTime.of(2099, 1, 15, 10, 0);

	ScheduleDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new ScheduleDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void everyInterviewIsListedByStartTimeWithNamesAndStatus() {
		assertThat(dao.getAllSchedule())
				.extracting(ScheduleInfo::getSchedulekey, ScheduleInfo::getCandidateid, ScheduleInfo::getCandidatename,
						ScheduleInfo::getInterviewerid, ScheduleInfo::getInterviewername, ScheduleInfo::getLocation,
						ScheduleInfo::getStatus, ScheduleInfo::getStartlabel)
				.containsExactly(
						tuple(2, "C2", "Cody Candidate", "U2", "Ivan Interviewer", "Online", "DONE", "2026-09-18 14:00"),
						tuple(1, "C1", "Carla Candidate", "U2", "Ivan Interviewer", "Room 1", "SCHEDULED",
								"2099-01-15 10:00"),
						// No location is null; U5 is deactivated but still named
						tuple(3, "C1", "Carla Candidate", "U5", "Fay Former", null, "CANCELLED", "2099-01-16 09:00"));
	}

	@Test
	void anInterviewersScheduleHoldsTheirInterviewsOnly() {
		assertThat(dao.getScheduleOf("U2")).extracting(ScheduleInfo::getSchedulekey).containsExactly(2, 1);
		assertThat(dao.getScheduleOf("U5")).extracting(ScheduleInfo::getSchedulekey).containsExactly(3);
		assertThat(dao.getScheduleOf("U3")).isEmpty();
	}

	@Test
	void anInterviewOfADeletedCandidateIsHidden() {
		jdbcTemplate.update("update candidate set isactive=0 where candidateid='C1'");

		assertThat(dao.getAllSchedule()).extracting(ScheduleInfo::getSchedulekey).containsExactly(2);
		assertThat(dao.findScheduleById(1)).isNull();
		assertThat(dao.getScheduleOf("U2")).extracting(ScheduleInfo::getSchedulekey).containsExactly(2);
		assertThat(dao.countUpcoming(null)).isZero();
	}

	@Test
	void findReturnsTheInterviewOrNull() {
		ScheduleInfo found = dao.findScheduleById(1);

		assertThat(found.getCandidateid()).isEqualTo("C1");
		assertThat(found.getInterviewerid()).isEqualTo("U2");
		assertThat(found.getStartat()).isEqualTo(SEED_START);
		assertThat(found.isScheduled()).isTrue();
		assertThat(dao.findScheduleById(99)).isNull();
		jdbcTemplate.update("update interview_schedule set isactive=0 where schedulekey=1");
		assertThat(dao.findScheduleById(1)).isNull();
		assertThat(dao.getAllSchedule()).extracting(ScheduleInfo::getSchedulekey).containsExactly(2, 3);
	}

	@Test
	void upcomingAreScheduledInterviewsFromNowOnSoonestFirst() {
		// Row 2 is in the past and DONE; row 3 is in the future but CANCELLED
		assertThat(dao.getUpcoming(null, 5)).extracting(ScheduleInfo::getSchedulekey).containsExactly(1);
		assertThat(dao.countUpcoming(null)).isEqualTo(1);
		assertThat(dao.countUpcoming("U2")).isEqualTo(1);
		assertThat(dao.countUpcoming("U5")).isZero();
		assertThat(dao.getUpcoming("U5", 5)).isEmpty();

		jdbcTemplate.update("insert into interview_schedule (candidateid, interviewerid, startat, status, createdby, "
				+ "createdat) values ('C2', 'U2', '2098-12-01 08:00:00', 'SCHEDULED', 'U3', now())");
		assertThat(dao.getUpcoming(null, 5)).extracting(ScheduleInfo::getStartlabel)
				.containsExactly("2098-12-01 08:00", "2099-01-15 10:00");
		assertThat(dao.getUpcoming("U2", 1)).extracting(ScheduleInfo::getStartlabel).containsExactly("2098-12-01 08:00");
		assertThat(dao.countUpcoming(null)).isEqualTo(2);
		// A past SCHEDULED interview is not upcoming
		jdbcTemplate.update("update interview_schedule set startat='2000-01-01 09:00:00' where schedulekey=1");
		assertThat(dao.countUpcoming(null)).isEqualTo(1);
	}

	@Test
	void anUnassignedInterviewersInterviewsDropOutOfTheirOwnPagesOnly() {
		jdbcTemplate.update("update candidate_interviewer set isactive=0 where assignmentkey=1");

		// U2 is no longer assigned to C1: interview 1 leaves their schedule and their upcoming list and count
		assertThat(dao.getScheduleOf("U2")).extracting(ScheduleInfo::getSchedulekey).containsExactly(2);
		assertThat(dao.getUpcoming("U2", 5)).isEmpty();
		assertThat(dao.countUpcoming("U2")).isZero();
		// Staff still see it, and can cancel it
		assertThat(dao.getAllSchedule()).extracting(ScheduleInfo::getSchedulekey).containsExactly(2, 1, 3);
		assertThat(dao.countUpcoming(null)).isEqualTo(1);
	}

	@Test
	void aConflictIsAnotherScheduledInterviewOfTheSameInterviewerAtTheSameTime() {
		assertThat(dao.hasConflict("U2", SEED_START, 0)).isTrue();
		// The interview being changed isn't its own conflict
		assertThat(dao.hasConflict("U2", SEED_START, 1)).isFalse();
		assertThat(dao.hasConflict("U2", SEED_START.plusMinutes(30), 0)).isFalse();
		assertThat(dao.hasConflict("U7", SEED_START, 0)).isFalse();
		// Cancelled and done interviews don't block the time
		assertThat(dao.hasConflict("U5", LocalDateTime.of(2099, 1, 16, 9, 0), 0)).isFalse();
		assertThat(dao.hasConflict("U2", LocalDateTime.of(2026, 9, 18, 14, 0), 0)).isFalse();
	}

	@Test
	void addInsertsAScheduledRowWithWhoAndWhen() {
		ScheduleInfo info = new ScheduleInfo();
		info.setCandidateid("C2");
		info.setInterviewerid("U2");
		info.setStartat(LocalDateTime.of(2099, 2, 1, 9, 30));
		info.setLocation("Room 9");
		// A status on the object is ignored: a new interview is SCHEDULED
		info.setStatus(ScheduleInfo.DONE);

		dao.addSchedule(info, "U3");

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from interview_schedule where schedulekey=4");
		assertThat(row.get("candidateid")).isEqualTo("C2");
		assertThat(row.get("location")).isEqualTo("Room 9");
		assertThat(row.get("status")).isEqualTo("SCHEDULED");
		assertThat(row.get("createdby")).isEqualTo("U3");
		assertThat(row.get("createdat")).isNotNull();
		assertThat(row.get("updatedby")).isNull();
		assertThat(row.get("isactive")).isEqualTo(1);
		assertThat(dao.findScheduleById(4).getStartat()).isEqualTo(LocalDateTime.of(2099, 2, 1, 9, 30));
	}

	@Test
	void aMissingLocationIsStoredAsNull() {
		ScheduleInfo info = new ScheduleInfo();
		info.setCandidateid("C2");
		info.setInterviewerid("U2");
		info.setStartat(LocalDateTime.of(2099, 2, 1, 9, 30));

		dao.addSchedule(info, "U3");

		assertThat(dao.findScheduleById(4).getLocation()).isNull();
	}

	@Test
	void updateChangesTheFieldsAndRecordsWho() {
		ScheduleInfo info = dao.findScheduleById(1);
		info.setStartat(LocalDateTime.of(2099, 1, 20, 15, 45));
		info.setLocation("Online");
		info.setStatus(ScheduleInfo.DONE);

		assertThat(dao.updateSchedule(info, "U1")).isTrue();

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from interview_schedule where schedulekey=1");
		assertThat(row.get("status")).isEqualTo("DONE");
		assertThat(row.get("location")).isEqualTo("Online");
		assertThat(row.get("updatedby")).isEqualTo("U1");
		assertThat(row.get("updatedat")).isNotNull();
		assertThat(row.get("candidateid")).isEqualTo("C1");
		assertThat(dao.findScheduleById(1).getStartlabel()).isEqualTo("2099-01-20 15:45");
	}

	@Test
	void aCancelledInterviewCantBeUpdated() {
		ScheduleInfo info = dao.findScheduleById(3);
		info.setLocation("Changed");
		info.setStatus(ScheduleInfo.SCHEDULED);

		assertThat(dao.updateSchedule(info, "U1")).isFalse();

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from interview_schedule where schedulekey=3");
		assertThat(row.get("status")).isEqualTo("CANCELLED");
		assertThat(row.get("location")).isNull();
	}

	@Test
	void cancelKeepsTheRowAndWorksOnAScheduledInterviewOnce() {
		assertThat(dao.cancelSchedule(1, "U3")).isTrue();
		assertThat(dao.cancelSchedule(1, "U3")).isFalse();

		Map<String, Object> row = jdbcTemplate.queryForMap("select * from interview_schedule where schedulekey=1");
		assertThat(row.get("status")).isEqualTo("CANCELLED");
		assertThat(row.get("updatedby")).isEqualTo("U3");
		assertThat(row.get("isactive")).isEqualTo(1);
		// Still listed, as cancelled
		assertThat(dao.findScheduleById(1).isCancelled()).isTrue();
		// A done interview can't be cancelled, nor can a missing one
		assertThat(dao.cancelSchedule(2, "U3")).isFalse();
		assertThat(dao.cancelSchedule(99, "U3")).isFalse();
		assertThat(dao.findScheduleById(2).isDone()).isTrue();
	}
}
