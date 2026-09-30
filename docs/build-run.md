# Build & Run

Purpose: How RMS is built, packaged, deployed and tested.
Last updated: 2026-09-30 (G11 tests: 45 tests, smoke 3/3). 2026-09-27 (full test run with Docker; Tomcat 9 smoke run; `db/local` script and Compose file executed; `mvnw` made executable)
Read this when: you're building, deploying, setting up an environment, or fixing a build or startup failure.

## Build
- **Tool:** Maven via the Maven Wrapper. The wrapper pins Maven 3.9.16 (`.mvn/wrapper/maven-wrapper.properties`); no global Maven install is needed. On first run the wrapper downloads Maven from Maven Central into `~/.m2/wrapper/dists/`. It runs from Git Bash (`./mvnw`) as well as `cmd` (`mvnw.cmd`). Since 2026-09-27, `mvnw` is committed as executable (mode `100755`, `git ls-files -s mvnw`). Before that it was `100644`, so `./mvnw` failed with "Permission denied" on Linux and macOS, although Git Bash on Windows ignores the bit. The artifact is `com.sabbir:rmsv2:1.0.1-SNAPSHOT`, packaged as a WAR (`pom.xml`).
- **JDK:** JDK 8 (for example Temurin 8). Set `JAVA_HOME` to it. On Windows, `winget install EclipseAdoptium.Temurin.8.JDK` installs it (used on 2026-09-26; the machine previously had only a JRE 8, with no `javac`). Phase 2 moves to JDK 21 ([modernization-plan.md](modernization-plan.md)).
- **Command:** `./mvnw -B verify` (or `mvnw.cmd -B verify` on Windows) compiles, runs the tests and builds `target/rmsv2-1.0.1-SNAPSHOT.war`. Verified on 2026-09-26 with Temurin 1.8.0_504: BUILD SUCCESS. On 2026-09-27 it also gave BUILD SUCCESS with OpenJDK 1.8.0_504 and, as a Phase 2 preview, with JDK 21.0.10, where `javac` only warns that source and target 8 are obsolete.
- **Compiler level:** Java 1.8 source and target (`pom.xml:16-17`), so class files are version 52. The WAR formerly committed on `master` has version 49 (Java 5) class files, because the pom set no level (`javap -v` on `master:target/rmsv2-1.0.1-SNAPSHOT.war`).
- **Source encoding:** UTF-8 (`pom.xml:18`). All Java sources are ASCII.
- **Pinned plugins:** `maven-compiler-plugin` 3.13.0, `maven-surefire-plugin` 3.5.3 and `maven-war-plugin` 3.4.0, the last with `warSourceDirectory=src/main/webapp` and `failOnMissingWebXml=false` (`pom.xml:131-155`).
- **WAR contents:** `WEB-INF/lib/` has the 10 Spring 5.3.39 jars (including `spring-jcl`), `jstl-1.2`, `mysql-connector-j-8.2.0`, `slf4j-api-2.0.20` and `logback-classic`/`logback-core` 1.3.16, plus `WEB-INF/classes/logback.xml`. Test libraries and `protobuf-java` aren't bundled (`jar tf`, 2026-09-26).

## Run / deploy
- **How it runs:** deploy the WAR to a Servlet 3.1 container. `javax.servlet-api` 3.1.0 is `provided` (`pom.xml`). There's no embedded server and no `main()` (`rms/config/WebInitializer.java`).
- **Database connection:** the container must provide a JNDI DataSource named `jdbc/springrms`, which the app looks up as `java:comp/env/jdbc/springrms` (`WebConfig.getDataSource`). Its definition isn't in the repo.
- **Driver change for Phase 1 (ops, outside the repo):**
  - The WAR now bundles Connector/J 8.2.0, the newest release that still supports MySQL 5.7 servers (8.3.0 and later need 8.0+; Connector/J release notes).
  - The container's JNDI resource should use `driverClassName` `com.mysql.cj.jdbc.Driver`. The old name `com.mysql.jdbc.Driver` still exists in the jar as a deprecated shim that logs a warning (`jar tf`).
  - Check the container's `lib/` for old `mysql-connector-java-5.1.x`, `slf4j` or `logback` jars. An old driver there keeps the pool on 5.1 silently, and against MySQL 8.0 a 5.1.36 driver can't connect at all (see *Why `mysql:8.0`* below). Duplicate logging jars can clash with the WAR's.
  - Connector/J 8 defaults to `sslMode=PREFERRED` and no longer offers TLS 1.0/1.1 (Connector/J documentation; not tested against the production server). Prefer `sslMode=REQUIRED` (or `VERIFY_CA`) in production. Don't copy the tests' `allowPublicKeyRetrieval=true` into production without the owner's sign-off: it lets a man-in-the-middle swap the server key.
  - Character sets: Connector/J 8 negotiates character sets differently from 5.1.36 [assumption]. Run the non-ASCII round-trip test (G34).
  - Keep the old driver jar and settings until sign-off, so the Phase 0 WAR can be redeployed ([modernization-plan.md](modernization-plan.md) Phase 1).
- **Local database:** `db/local/` builds a local `rms_local` database from `db/schema.sql` and `db/test-seed.sql`. It offers either a script for an existing local MySQL (`setup-local-db.ps1` or `.sh`) or a throwaway `mysql:8.0` through `docker-compose.yml`. The scripts refuse non-loopback hosts by default and commit no passwords. See `db/local/README.md`. Written on 2026-09-26. On 2026-09-27, `setup-local-db.sh` was run against a throwaway MySQL 8.0.46 (not a shared server):
  - Every run printed the expected row counts, and a rerun reset data changed in between.
  - A non-loopback host was refused.
  - The app's DAOs from a `dev` build (Connector/J 8.2.0, default SSL) logged in as `test.admin`, added a position, and read both seeded score rows correctly from `rms_local`.
  - `docker-compose.yml` was run the same day on Docker Engine 29.3.1. The first start loaded the schema and seed (row counts as in its README, `utf8mb4_unicode_ci`), and `docker compose down -v` removed it again.
  - `setup-local-db.ps1` hasn't been executed: there was no PowerShell on that machine.
- **Entry URLs:** `GET /login` shows the login page (`LoginController.loginPage`). `index.jsp` is a "Hello World!" placeholder (`src/main/webapp/index.jsp`).

## Test
Tests are characterization tests: they pin current behaviour, including known defects, so upgrades can be checked against it (`src/test/java/rms/`). `mvnw verify` runs 45 tests. On 2026-09-30, with Docker available and `RMS_REQUIRE_DOCKER=true`, 42 passed on JDK 8, and only the 3 smoke tests were skipped. Those 3 then passed against a deployed WAR (see *Smoke run* below). On 2026-09-27, before the G11 tests, 34 of 36 passed on both JDK 8u504 and JDK 21.

| Kind | Classes | Needs | Last result |
|---|---|---|---|
| Controller | `rms/controller/*ControllerTest` (standalone MockMvc, mocked services, no interceptor) | nothing | 16 pass |
| Access control | `rms/config/AuthInterceptorTest` (standalone MockMvc with the interceptor exactly as `WebConfig` registers it) | nothing | 8 pass (2026-09-30) |
| DAO | `rms/dao/*DaoImplTest`, base `MySqlContainerSupport` | Docker. Testcontainers starts one throwaway `mysql:8.0` shared by all DAO classes, and each test reloads `db/schema.sql` and `db/test-seed.sql`. Without Docker each test is reported as skipped; set `RMS_REQUIRE_DOCKER=true` (e.g. in CI) to make them fail instead | 18 pass on `mysql:8.0` (2026-09-27, Docker Engine 29.3.1, Testcontainers 1.21.4). Skipped where there's no Docker; the `RMS_REQUIRE_DOCKER=true` gate was checked and fails as intended |
| HTTP smoke | `rms/SmokeTest` | a deployed WAR; the environment variable `RMS_BASE_URL`. The login check also needs `RMS_SMOKE_USER` and `RMS_SMOKE_PASSWORD` set to seed test values | 3 pass against Tomcat 9.0.122 on JDK 8 (2026-09-30), including the logged-out redirect; skipped when `RMS_BASE_URL` isn't set |

- The DAO tests never touch a real database. `db/schema.sql` is inferred, not the production DDL (see [data-model.md](data-model.md)).
- **Smoke run (2026-09-27):**
  - **Setup:** Tomcat 9.0.122 on JDK 8u504 with the Connector/J 8.2.0 jar in Tomcat's `lib/`. The JNDI `jdbc/springrms` used `com.mysql.cj.jdbc.Driver` and `sslMode=REQUIRED`, and pointed at the `db/local/docker-compose.yml` database. The config stayed outside the repo.
  - **Result:** both `SmokeTest` tests passed. The admin results page, the position and language lists and the create form returned 200 with the seed data and no unrendered EL. A position saved through `/saveposition` appeared in the list, and Tomcat logged no errors.
  - **Since G11 (2026-09-30):** those pages need an admin login. Logged out, they redirect to `/login`, so a plain `curl` now gets a 302.
  - **Later the same day:** the acceptance checklist, the browser check and the baseline screenshots ran in Chromium against this setup and a Phase 0 build ([acceptance/README.md](acceptance/README.md)).
- Why `mysql:8.0`: the production server version is unknown (open question #16); 8.0 is an assumption. Phase 0 used `mysql:5.7` because Connector/J 5.1.36 can't connect to MySQL 8.0 at all. It fails in the handshake (`NullPointerException` on `serverVariables`) even when both the account and the server default use `mysql_native_password`, while 8.x drivers connect (tested 2026-09-26 on MySQL 8.0.46). Phase 1 moved the driver and the image together.

## Repo notes
- **`.gitattributes`** keeps `mvnw` at LF and `*.cmd` at CRLF line endings, so the wrapper runs on Linux CI as well as Windows.
- **`target/` is not tracked:** `.gitignore` lists `target/`. The old committed WAR, which predates the Position module (G29), is only in `master`'s history.
- Eclipse project files are committed: `.project`, `.classpath`, `.settings/`. `.settings` still says Java 1.5; `pom.xml` is authoritative.
