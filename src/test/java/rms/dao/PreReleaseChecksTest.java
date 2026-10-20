package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;

/**
 * db/checks/pre-release-checks.sql, the owner's read-only check run (docs/release-dev.md), on the test MySQL:
 * every statement runs, nothing can be written, and no seed name, password or e-mail is read out.
 * The test account has no right on mysql.user, as an owner's read-only account may not: that one check is
 * refused (1142) and the rest still answer, as with mysql --force.
 */
class PreReleaseChecksTest extends MySqlContainerSupport {

	private static final int ACCESS_DENIED = 1142;
	private static final int READ_ONLY_TRANSACTION = 1792;

	@Test
	void runsReadOnlyAndReadsOutNoPersonalData() throws Exception {
		String script = Files.readString(new File(System.getProperty("basedir", "."), "db/checks/pre-release-checks.sql")
				.toPath(), StandardCharsets.UTF_8);
		// As the mysql client reads it: comment lines dropped, one statement per terminating semicolon
		List<String> statements = Arrays.stream(script.replaceAll("(?m)^--.*$", "").split(";\\s*(\\R|$)"))
				.map(String::strip).filter(sql -> !sql.isEmpty()).toList();
		int candidates = jdbcTemplate.queryForObject("select count(*) from candidate", Integer.class);

		List<Integer> refused = new ArrayList<>();
		StringBuilder output = new StringBuilder();
		jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
			try (Statement statement = connection.createStatement()) {
				for (String sql : statements) {
					try {
						if (statement.execute(sql)) {
							read(statement.getResultSet(), output);
						}
					} catch (SQLException e) {
						refused.add(e.getErrorCode());
					}
				}
				// The session stays read only after the script's COMMIT
				assertThatThrownBy(() -> statement.executeUpdate("update candidate set candidatestatus = 'X'"))
						.isInstanceOfSatisfying(SQLException.class,
								e -> assertThat(e.getErrorCode()).isEqualTo(READ_ONLY_TRANSACTION));
			}
			return null;
		});

		assertThat(refused).as("refused statements (only mysql.user may be)").containsOnly(ACCESS_DENIED).hasSize(1);
		assertThat(jdbcTemplate.queryForObject("select count(*) from candidate", Integer.class)).isEqualTo(candidates);
		assertThat(output).contains("end of the RMS pre-release checks", "CREATE TABLE `marks`", "InnoDB");
		// db/test-seed.sql: usernames, a plain and a hashed password, names and e-mail addresses
		assertThat(output.toString()).doesNotContain("test.admin", "test-only-1", "{bcrypt}$2a$", "Carla", "Ivan",
				"@example.test", "000-1001");
	}

	private static void read(ResultSet rows, StringBuilder output) throws SQLException {
		int columns = rows.getMetaData().getColumnCount();
		while (rows.next()) {
			for (int i = 1; i <= columns; i++) {
				output.append(rows.getString(i)).append('|');
			}
			output.append('\n');
		}
	}
}
