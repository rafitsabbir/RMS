package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.ActivityInfo;

/**
 * Characterization tests against db/schema.sql + db/test-seed.sql. The seed holds two entries: U2 MARKED C1 (total 80)
 * and U3 SELECTED C1, newest last.
 */
class ActivityDaoImplTest extends MySqlContainerSupport {

	ActivityDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new ActivityDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void theNewestEntryComesFirstWithTheUsersName() {
		List<ActivityInfo> entries = dao.getRecent(null, 10);

		assertThat(entries).extracting(ActivityInfo::getAction).containsExactly("SELECTED", "MARKED");
		assertThat(entries.get(0).getUserid()).isEqualTo("U3");
		assertThat(entries.get(0).getUsername()).isNotBlank();
		assertThat(entries.get(1).getDetail()).isEqualTo("total 80");
		assertThat(entries.get(1).getCreatedat()).isNotNull();
	}

	@Test
	void theListIsLimitedAndFilteredByAction() {
		assertThat(dao.getRecent(null, 1)).hasSize(1);
		assertThat(dao.getRecent("MARKED", 10)).extracting(ActivityInfo::getEntityid).containsExactly("C1");
		assertThat(dao.getRecent("UNKNOWN", 10)).isEmpty();
	}

	@Test
	void anEntryIsAddedWithTheServersTime() {
		ActivityInfo entry = new ActivityInfo();
		entry.setUserid("U3");
		entry.setAction("ON_HOLD");
		entry.setEntitytype("CANDIDATE");
		entry.setEntityid("C2");

		dao.add(entry);

		ActivityInfo newest = dao.getRecent(null, 1).get(0);
		assertThat(newest.getAction()).isEqualTo("ON_HOLD");
		assertThat(newest.getDetail()).isNull();
		assertThat(newest.getCreatedat()).isNotNull();
		assertThat(dao.getActions()).containsExactly("MARKED", "ON_HOLD", "SELECTED");
	}

	@Test
	void anEntryOfARemovedUserStillLists() {
		jdbcTemplate.update("delete from admin where userid='U3'");

		ActivityInfo newest = dao.getRecent(null, 1).get(0);

		assertThat(newest.getUserid()).isEqualTo("U3");
		assertThat(newest.getUsername()).isNull();
	}
}
