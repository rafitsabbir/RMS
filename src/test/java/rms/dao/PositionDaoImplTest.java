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
		assertThat(dao.addPosition(position(0, "  dev ops "))).isTrue();

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from position where positionname='DEV OPS' and isactive=1", Integer.class))
				.isEqualTo(1);
	}

	@Test
	void addRefusesDuplicateName() {
		assertThat(dao.addPosition(position(0, "software engineer"))).isFalse();

		assertThat(countRows()).isEqualTo(3);
	}

	@Test
	void addBringsBackADeletedPosition() {
		assertThat(dao.addPosition(position(0, " retired role "))).isTrue();

		assertThat(countRows()).isEqualTo(3);
		assertThat(isactive(3)).isEqualTo(1);
	}

	@Test
	void addRefusesNameThatAlreadyHasTwoRows() {
		jdbcTemplate.update("insert into position (positionname, isactive) values ('QA ENGINEER', 1)");

		assertThat(dao.addPosition(position(0, "qa engineer"))).isFalse();
		assertThat(countRows()).isEqualTo(4);
	}

	@Test
	void updateUppercasesName() {
		assertThat(dao.updatePosition(position(1, " lead "))).isTrue();

		assertThat(dao.findPositionById(1).getPositionname()).isEqualTo("LEAD");
	}

	@Test
	void updateRefusesNameOfAnotherActivePosition() {
		assertThat(dao.updatePosition(position(1, "qa engineer"))).isFalse();

		assertThat(dao.findPositionById(1).getPositionname()).isEqualTo("SOFTWARE ENGINEER");
	}

	@Test
	void updateKeepingTheSameNameSucceeds() {
		assertThat(dao.updatePosition(position(1, "software engineer"))).isTrue();
	}

	@Test
	void updateLeavesDeletedOrMissingPositionAlone() {
		assertThat(dao.updatePosition(position(3, "renamed"))).isTrue();
		assertThat(dao.updatePosition(position(99, "renamed"))).isTrue();

		assertThat(name(3)).isEqualTo("RETIRED ROLE");
		assertThat(countRows()).isEqualTo(3);
	}

	@Test
	void renameOntoTheNameOfADeletedPositionIsAllowed() {
		assertThat(dao.updatePosition(position(1, "retired role"))).isTrue();

		assertThat(name(1)).isEqualTo("RETIRED ROLE");
	}

	@Test
	void addBringsBackTheOldestOfSeveralDeletedRows() {
		jdbcTemplate.update("insert into position (positionname, isactive) values ('RETIRED ROLE', 0)");

		assertThat(dao.addPosition(position(0, "retired role"))).isTrue();

		assertThat(isactive(3)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from position where positionname='RETIRED ROLE' and isactive=1", Integer.class)).isEqualTo(1);
	}

	@Test
	void deleteDeactivatesTheRow() {
		dao.deletePosition(2);

		assertThat(countRows()).isEqualTo(3);
		assertThat(isactive(2)).isEqualTo(0);
		assertThat(dao.getAllPosition()).extracting(PositionInfo::getPositionname).containsExactly("SOFTWARE ENGINEER");
	}

	@Test
	void findByIdReturnsNullForDeletedOrMissingPosition() {
		assertThat(dao.findPositionById(3)).isNull();
		assertThat(dao.findPositionById(99)).isNull();
	}

	private int countRows() {
		return jdbcTemplate.queryForObject("select count(*) from position", Integer.class);
	}

	private int isactive(int key) {
		return jdbcTemplate.queryForObject("select isactive from position where positionkey=?", Integer.class, key);
	}

	private String name(int key) {
		return jdbcTemplate.queryForObject("select positionname from position where positionkey=?", String.class, key);
	}

	private static PositionInfo position(int key, String name) {
		PositionInfo position = new PositionInfo();
		position.setPositionkey(key);
		position.setPositionname(name);
		return position;
	}

}
