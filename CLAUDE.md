# RMS — Recruitment Management System

**Before any task, read [docs/ROUTER.md](docs/ROUTER.md) and load only the files it points to.**
Use subagents in .claude/agents/ — see ROUTER.md.

## Summary
- RMS is a small recruitment management web app (`README.md`) with two roles: admin and interviewer, chosen by `isinterviewer` (`main.jsp`).
- Admins maintain the job-position and language/skill lists, and view candidates' scores on 10 criteria with a total and a selected/rejected status (`rms/controller/*`, `viewmarks.jsp`).
- Candidate, interviewer, job and schedule modules, and interviewer score entry, are menu links only, with no backend (`main.jsp`, `MarksDaoImpl.java`).

**Stack:**
- Java: 1.8 (source/target in `pom.xml`), built with JDK 8.
- Build: Maven 3.9 via the Maven Wrapper (`mvnw`), packaged as a WAR.
- Framework: Spring MVC 4.3.0 with JSP/JSTL and Spring JDBC (`NamedParameterJdbcTemplate`, no ORM).
- Database: MySQL (Connector/J 5.1.36) through JNDI `jdbc/springrms`, in an external Servlet 3.1 container.

Evidence: `pom.xml`, `.mvn/wrapper/maven-wrapper.properties`, `rms/config/WebConfig.java`.

## Build / run / test
- **Build:** with `JAVA_HOME` set to a JDK 8, `./mvnw -B verify` (or `mvnw.cmd`) runs the tests and builds `target/rmsv2-1.0.1-SNAPSHOT.war`.
- **Run:** deploy the WAR to a Servlet 3.1 container that provides the JNDI DataSource `jdbc/springrms`. Open `/login`. See [docs/build-run.md](docs/build-run.md).
- **Test:** characterization tests live in `src/test/java/rms/`. Controller tests always run; DAO tests need Docker (throwaway MySQL via Testcontainers, never a real DB); `SmokeTest` needs `RMS_BASE_URL`. See [docs/build-run.md](docs/build-run.md).

## Critical rules
- **Database:** don't connect to or change any database unless the user explicitly asks. `db/schema.sql` is inferred from the SQL, not the production DDL; treat it and `docs/data-model.md` as what the SQL references, not the full truth.
- **Secrets:** never copy credentials, JNDI resource definitions or hostnames into code, docs or chat.
- **Git:** work on the `dev` branch; `master` is the default branch. Commit only when asked.
- **Code style:**
  - Match the existing layering (`controller → service → dao → model`), `*Impl` / `*Info` naming, SQL in DAO string fields with named params, and tab indentation. See [docs/conventions.md](docs/conventions.md).
  - Keep tests as characterization tests: pin current behaviour, and mark known defects with `// characterizes Gnn`.
- **Docs:**
  - Keep them evidence-based: cite file paths, and record unconfirmed items in [docs/open-questions.md](docs/open-questions.md).
  - Update the owning file under `docs/` when facts change.

## Repo layout
```
pom.xml                       Maven build (WAR)
src/main/java/rms/
  config/                     WebInitializer (bootstrap), WebConfig (MVC, DataSource)
  controller/                 Login, Position, Language, Marks
  service/  dao/  model/      *Service(+Impl), *Dao(+Impl), *Info
src/main/webapp/
  WEB-INF/jsp/                7 JSP views (main.jsp = menu shell)
  WEB-INF/web.xml             empty stub
  resources/                  css, js, images
src/test/java/rms/            controller, DAO (Testcontainers) and smoke tests
db/                           schema.sql (inferred), test-seed.sql (synthetic)
mvnw, .mvn/wrapper/           Maven Wrapper
docs/                         project knowledge; start at docs/ROUTER.md
target/                       build output (git-ignored)
```
