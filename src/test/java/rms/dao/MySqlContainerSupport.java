package rms.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base class for DAO characterization tests. Runs against a throwaway MySQL
 * container only, never a real database.
 *
 * Without Docker the tests are skipped, unless RMS_REQUIRE_DOCKER=true (e.g. in CI),
 * which makes them fail instead. One container is shared by all DAO test classes
 * and removed by Testcontainers when the JVM exits.
 *
 * MySQL 8.0 is an assumed production line (open question #16). Connector/J 8.2.0 supports
 * servers from 5.7 on (release notes). Change the image when the production version is known.
 */
abstract class MySqlContainerSupport {

	private static final String[] TABLES = { "marks", "candidate", "language", "position", "admin", "users" };

	private static MySQLContainer<?> mysql;
	private static boolean docker;

	NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	JdbcTemplate jdbcTemplate;

	@BeforeAll
	static synchronized void startContainer() {
		docker = DockerClientFactory.instance().isDockerAvailable();
		if ("true".equals(System.getenv("RMS_REQUIRE_DOCKER"))) {
			assertTrue(docker, "Docker is required (RMS_REQUIRE_DOCKER=true) but not available");
		}
		if (docker && mysql == null) {
			mysql = new MySQLContainer<>("mysql:8.0");
			mysql.start();
		}
	}

	@BeforeEach
	void resetDatabase() {
		assumeTrue(docker, "Docker not available: DAO test skipped");
		String url = mysql.getJdbcUrl();
		url += (url.contains("?") ? "&" : "?") + "sslMode=DISABLED&allowPublicKeyRetrieval=true";
		DriverManagerDataSource datasource = new DriverManagerDataSource(url, mysql.getUsername(),
				mysql.getPassword());
		datasource.setDriverClassName("com.mysql.cj.jdbc.Driver");

		jdbcTemplate = new JdbcTemplate(datasource);
		for (String table : TABLES) {
			jdbcTemplate.execute("drop table if exists " + table);
		}
		populate(datasource);
		namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(datasource);
	}

	private static void populate(DataSource datasource) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
				dbFile("schema.sql"), dbFile("test-seed.sql"));
		populator.execute(datasource);
	}

	private static FileSystemResource dbFile(String name) {
		return new FileSystemResource(new File(System.getProperty("basedir", "."), "db/" + name));
	}

}
