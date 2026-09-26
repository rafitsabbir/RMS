package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rms.model.LanguageInfo;

/** Characterization tests against db/schema.sql + db/test-seed.sql. */
class LanguageDaoImplTest extends MySqlContainerSupport {

	LanguageDaoImpl dao;

	@BeforeEach
	void setUp() {
		dao = new LanguageDaoImpl();
		dao.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
	}

	@Test
	void listReturnsActiveRowsOnly() {
		assertThat(dao.getAllLanguage()).extracting(LanguageInfo::getLanguagename)
				.containsExactlyInAnyOrder("JAVA", "PYTHON");
	}

	@Test
	void addUppercasesTrimsAndActivates() {
		dao.addLanguage(language(0, "  kotlin "));

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from language where languagename='KOTLIN' and isactive=1", Integer.class))
				.isEqualTo(1);
	}

	@Test
	void addSilentlySkipsDuplicateName() {
		// characterizes G14: the duplicate is only printed, not reported
		dao.addLanguage(language(0, "java"));

		assertThat(countRows()).isEqualTo(3);
	}

	@Test
	void updateUppercasesName() {
		dao.updateLanguage(language(1, " go "));

		assertThat(dao.findLanguageById(1).getLanguagename()).isEqualTo("GO");
	}

	@Test
	void deleteRemovesTheRow() {
		// characterizes G17: hard delete, not isactive=0
		dao.deleteLanguage(2);

		assertThat(countRows()).isEqualTo(2);
	}

	@Test
	void findByIdIgnoresActiveFlag() {
		assertThat(dao.findLanguageById(3).getLanguagename()).isEqualTo("COBOL");
	}

	private int countRows() {
		return jdbcTemplate.queryForObject("select count(*) from language", Integer.class);
	}

	private static LanguageInfo language(int key, String name) {
		LanguageInfo language = new LanguageInfo();
		language.setLanguagekey(key);
		language.setLanguagename(name);
		return language;
	}

}
