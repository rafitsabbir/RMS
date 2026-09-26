package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.PositionInfo;

/** Characterization tests against db/schema.sql + db/test-seed.sql. */
class PositionDaoImplTest extends MySqlContainerSupport {

	PositionDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new PositionDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void listReturnsActiveRowsOnly() {
		assertThat(dao.getAllPosition()).extracting(PositionInfo::getPositionname)
				.containsExactlyInAnyOrder("SOFTWARE ENGINEER", "QA ENGINEER");
	}

	@Test
	void addUppercasesTrimsAndActivates() {
		dao.addPosition(position(0, "  dev ops "));

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from position where positionname='DEV OPS' and isactive=1", Integer.class))
				.isEqualTo(1);
	}

	@Test
	void addSilentlySkipsDuplicateName() {
		// characterizes G14: the duplicate is only printed, not reported
		dao.addPosition(position(0, "software engineer"));

		assertThat(countRows()).isEqualTo(3);
	}

	@Test
	void updateUppercasesName() {
		dao.updatePosition(position(1, " lead "));

		assertThat(dao.findPositionById(1).getPositionname()).isEqualTo("LEAD");
	}

	@Test
	void deleteRemovesTheRow() {
		// characterizes G17: hard delete, not isactive=0
		dao.deletePosition(2);

		assertThat(countRows()).isEqualTo(2);
	}

	@Test
	void findByIdIgnoresActiveFlag() {
		assertThat(dao.findPositionById(3).getPositionname()).isEqualTo("RETIRED ROLE");
	}

	private int countRows() {
		return jdbcTemplate.queryForObject("select count(*) from position", Integer.class);
	}

	private static PositionInfo position(int key, String name) {
		PositionInfo position = new PositionInfo();
		position.setPositionkey(key);
		position.setPositionname(name);
		return position;
	}

}
