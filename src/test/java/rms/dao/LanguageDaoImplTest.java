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
		assertThat(dao.addLanguage(language(0, "  kotlin "))).isTrue();

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from language where languagename='KOTLIN' and isactive=1", Integer.class))
				.isEqualTo(1);
	}

	@Test
	void addRefusesDuplicateName() {
		assertThat(dao.addLanguage(language(0, "java"))).isFalse();

		assertThat(countRows()).isEqualTo(3);
	}

	@Test
	void addBringsBackADeletedLanguage() {
		assertThat(dao.addLanguage(language(0, " cobol "))).isTrue();

		assertThat(countRows()).isEqualTo(3);
		assertThat(isactive(3)).isEqualTo(1);
	}

	@Test
	void addRefusesNameThatAlreadyHasTwoRows() {
		jdbcTemplate.update("insert into language (languagename, isactive) values ('PYTHON', 1)");

		assertThat(dao.addLanguage(language(0, "python"))).isFalse();
		assertThat(countRows()).isEqualTo(4);
	}

	@Test
	void updateUppercasesName() {
		assertThat(dao.updateLanguage(language(1, " go "))).isTrue();

		assertThat(dao.findLanguageById(1).getLanguagename()).isEqualTo("GO");
	}

	@Test
	void updateRefusesNameOfAnotherActiveLanguage() {
		assertThat(dao.updateLanguage(language(1, "python"))).isFalse();

		assertThat(dao.findLanguageById(1).getLanguagename()).isEqualTo("JAVA");
	}

	@Test
	void updateKeepingTheSameNameSucceeds() {
		assertThat(dao.updateLanguage(language(1, "java"))).isTrue();
	}

	@Test
	void updateLeavesDeletedOrMissingLanguageAlone() {
		assertThat(dao.updateLanguage(language(3, "renamed"))).isTrue();
		assertThat(dao.updateLanguage(language(99, "renamed"))).isTrue();

		assertThat(name(3)).isEqualTo("COBOL");
		assertThat(countRows()).isEqualTo(3);
	}

	@Test
	void renameOntoTheNameOfADeletedLanguageIsAllowed() {
		assertThat(dao.updateLanguage(language(1, "cobol"))).isTrue();

		assertThat(name(1)).isEqualTo("COBOL");
	}

	@Test
	void addBringsBackTheOldestOfSeveralDeletedRows() {
		jdbcTemplate.update("insert into language (languagename, isactive) values ('COBOL', 0)");

		assertThat(dao.addLanguage(language(0, "cobol"))).isTrue();

		assertThat(isactive(3)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from language where languagename='COBOL' and isactive=1", Integer.class)).isEqualTo(1);
	}

	@Test
	void deleteDeactivatesTheRow() {
		dao.deleteLanguage(2);

		assertThat(countRows()).isEqualTo(3);
		assertThat(isactive(2)).isEqualTo(0);
		assertThat(dao.getAllLanguage()).extracting(LanguageInfo::getLanguagename).containsExactly("JAVA");
	}

	@Test
	void findByIdReturnsNullForDeletedOrMissingLanguage() {
		assertThat(dao.findLanguageById(3)).isNull();
		assertThat(dao.findLanguageById(99)).isNull();
	}

	private int countRows() {
		return jdbcTemplate.queryForObject("select count(*) from language", Integer.class);
	}

	private int isactive(int key) {
		return jdbcTemplate.queryForObject("select isactive from language where languagekey=?", Integer.class, key);
	}

	private String name(int key) {
		return jdbcTemplate.queryForObject("select languagename from language where languagekey=?", String.class, key);
	}

	private static LanguageInfo language(int key, String name) {
		LanguageInfo language = new LanguageInfo();
		language.setLanguagekey(key);
		language.setLanguagename(name);
		return language;
	}

}
