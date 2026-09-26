# Build & Run

Purpose: How RMS is built, packaged, deployed and tested.
Last updated: 2026-09-26 (Phase 0 safety net)
Read this when: you're building, deploying, setting up an environment, or fixing a build or startup failure.

## Build
- **Tool:** Maven via the Maven Wrapper. The wrapper pins Maven 3.9.16 (`.mvn/wrapper/maven-wrapper.properties`); no global Maven install is needed. On first run the wrapper downloads Maven from Maven Central into `~/.m2/wrapper/dists/`. It runs from Git Bash (`./mvnw`) as well as `cmd` (`mvnw.cmd`). The artifact is `com.sabbir:rmsv2:1.0.1-SNAPSHOT`, packaged as a WAR (`pom.xml`).
- **JDK:** JDK 8 (for example Temurin 8). Set `JAVA_HOME` to it. On Windows, `winget install EclipseAdoptium.Temurin.8.JDK` installs it (used on 2026-09-26; the machine previously had only a JRE 8, with no `javac`). Phase 2 moves to JDK 21 ([modernization-plan.md](modernization-plan.md)).
- **Command:** `./mvnw -B verify` (or `mvnw.cmd -B verify` on Windows) compiles, runs the tests and builds `target/rmsv2-1.0.1-SNAPSHOT.war`. Verified on 2026-09-26 with Temurin 1.8.0_504: BUILD SUCCESS.
- **Compiler level:** Java 1.8 source and target (`pom.xml:16-17`), so class files are version 52. The WAR formerly committed on `master` has version 49 (Java 5) class files, because the pom set no level (`javap -v` on `master:target/rmsv2-1.0.1-SNAPSHOT.war`).
- **Source encoding:** UTF-8 (`pom.xml:18`). All Java sources are ASCII.
- **Pinned plugins:** `maven-compiler-plugin` 3.13.0, `maven-surefire-plugin` 3.5.3 and `maven-war-plugin` 3.4.0, the last with `warSourceDirectory=src/main/webapp` and `failOnMissingWebXml=false` (`pom.xml:131-155`).
- **WAR contents:** `WEB-INF/lib/` has the 9 Spring 4.3.0 jars, `jstl-1.2`, `mysql-connector-java-5.1.36` and `commons-logging-1.2`. Test libraries aren't bundled (`jar tf`).

## Run / deploy
- **How it runs:** deploy the WAR to a Servlet 3.1 container. `javax.servlet-api` 3.1.0 is `provided` (`pom.xml`). There's no embedded server and no `main()` (`rms/config/WebInitializer.java`).
- **Database connection:** the container must provide a JNDI DataSource named `jdbc/springrms`, which the app looks up as `java:comp/env/jdbc/springrms` (`WebConfig.getDataSource`). Its definition isn't in the repo.
- **Entry URLs:** `GET /login` shows the login page (`LoginController.loginPage`). `index.jsp` is a "Hello World!" placeholder (`src/main/webapp/index.jsp`).

## Test
Tests are characterization tests: they pin current behaviour, including known defects, so upgrades can be checked against it (`src/test/java/rms/`). As of 2026-09-26, `mvnw verify` runs 36 tests: 16 pass and 20 are skipped here.

| Kind | Classes | Needs | Status on this machine |
|---|---|---|---|
| Controller | `rms/controller/*ControllerTest` (standalone MockMvc, mocked services) | nothing | 16 pass |
| DAO | `rms/dao/*DaoImplTest`, base `MySqlContainerSupport` | Docker. Testcontainers starts one throwaway `mysql:5.7` shared by all DAO classes, and each test reloads `db/schema.sql` and `db/test-seed.sql`. Without Docker each test is reported as skipped; set `RMS_REQUIRE_DOCKER=true` (e.g. in CI) to make them fail instead | skipped (no Docker); the `RMS_REQUIRE_DOCKER=true` gate was checked and fails as intended |
| HTTP smoke | `rms/SmokeTest` | a deployed WAR; the environment variable `RMS_BASE_URL`. The login check also needs `RMS_SMOKE_USER` and `RMS_SMOKE_PASSWORD` set to seed test values | skipped (not set) |

- The DAO tests never touch a real database. `db/schema.sql` is inferred, not the production DDL (see [data-model.md](data-model.md)).
- Why `mysql:5.7`: the current driver, Connector/J 5.1.36, can't authenticate to MySQL 8's default `caching_sha2_password`. Phase 1 changes the driver and the image together.

## Repo notes
- **`.gitattributes`** keeps `mvnw` at LF and `*.cmd` at CRLF line endings, so the wrapper runs on Linux CI as well as Windows.
- **`target/` is not tracked:** `.gitignore` lists `target/`. The old committed WAR, which predates the Position module (G29), is only in `master`'s history.
- Eclipse project files are committed: `.project`, `.classpath`, `.settings/`. `.settings` still says Java 1.5; `pom.xml` is authoritative.
