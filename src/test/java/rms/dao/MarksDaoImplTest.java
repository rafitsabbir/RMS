package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.MarksInfo;

/** Characterization tests against db/schema.sql + db/test-seed.sql. */
class MarksDaoImplTest extends MySqlContainerSupport {

	MarksDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new MarksDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void adminMarksAreOrderedByCandidate() {
		assertThat(dao.getAllMarksByAdmin()).extracting(MarksInfo::getCandidateid)
				.containsExactly("Carla Candidate", "Cody Candidate");
	}

	@Test
	void adminMarksMapNamesIntoIdFieldsAndScoresByPosition() {
		// characterizes G10: names land in the *id fields; m.* is read by column index
		List<MarksInfo> marks = dao.getAllMarksByAdmin();
		MarksInfo first = marks.get(0);

		assertThat(first.getInterviewerid()).isEqualTo("Ivan Interviewer");
		assertThat(first.getCandidateid()).isEqualTo("Carla Candidate");
		assertThat(first.getPosition()).isEqualTo("SOFTWARE ENGINEER");
		assertThat(first.getLanguage()).isEqualTo("JAVA");
		assertThat(first.getIsactive()).isEqualTo(1);
		assertThat(first.getWorkexp()).isEqualTo(8);
		assertThat(first.getTechknowledge()).isEqualTo(9);
		assertThat(first.getLeadership()).isEqualTo(7);
		assertThat(first.getDecision()).isEqualTo(8);
		assertThat(first.getProbsolving()).isEqualTo(9);
		assertThat(first.getStress()).isEqualTo(6);
		assertThat(first.getEducation()).isEqualTo(8);
		assertThat(first.getComskill()).isEqualTo(7);
		assertThat(first.getAttitude()).isEqualTo(9);
		assertThat(first.getPersonality()).isEqualTo(8);
		assertThat(first.getCandidateStatus()).isEqualTo("S");
		assertThat(marks.get(1).getCandidateStatus()).isEqualTo("R");
	}

}
