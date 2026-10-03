# RMS — Recruitment Management System

**Before any task, read [docs/ROUTER.md](docs/ROUTER.md) and load only the files it points to.**
Use subagents in .claude/agents/ — see ROUTER.md.

## Summary
- RMS is a small recruitment management web app (`README.md`) with four roles in `admin.role`: Super Admin, HR, Hiring Manager (read-only) and Interviewer (`rms/model/Role.java`; NULL falls back to `isinterviewer`). Access is deny-by-default in `SecurityConfig`.
- Super Admins manage users (Users and Roles: add, role, deactivate, reset password; IDs U1, U2, …). Super Admins and HR maintain candidates (add, list, edit, soft delete; IDs C1, C2, …) and the job-position and language/skill lists, assign interviewers to candidates, see the average of each candidate's evaluations on 10 criteria (Candidate Status), record Selected / Rejected / On hold with a reason, and schedule interviews (`rms/controller/*`). Interviewers score their assigned candidates (My Evaluations). Hiring Managers read. Everyone can change their own password.
- Super Admins and HR schedule interviews (an assigned, active interviewer; `interview_schedule`); staff read the schedule, interviewers see their own (My Schedule); the home page shows dashboard figures by role (roles plan Phase 4, [docs/business-flows/schedule.md](docs/business-flows/schedule.md)). No menu entry is "Coming soon" any more.
- Staff download CSV reports (candidates, Candidate Status, interview schedule; Reports page). Actions that matter (marking, selecting, rejecting, interviews, candidate and user changes, report downloads) are written to `activity_log`, which Super Admins read under Activity Log. Only English text is accepted (`rms/service/EnglishText.java`). E-mail notifications are not built; the owner will decide later ([docs/business-flows/reports-activity.md](docs/business-flows/reports-activity.md)).

**Stack:**
- Java: 21 (`maven.compiler.release` in `pom.xml`), built with JDK 21.
- Build: Maven 3.9 via the Maven Wrapper (`mvnw`), packaged as a WAR.
- Framework: Spring MVC 7.0.9 (Spring BOM) on Jakarta EE 11, with JSP/Jakarta Tags (JSTL 3.0) and Spring JDBC (`NamedParameterJdbcTemplate`, no ORM).
- Database: MySQL (Connector/J 8.2.0, `com.mysql.cj.jdbc.Driver`) through JNDI `jdbc/springrms`, in an external Servlet 6.1 container (Tomcat 11).
- Logging: SLF4J 2.0 with Logback 1.5 (`src/main/resources/logback.xml`).
- Security: Spring Security 7.0.7: form login, four roles, deny by default, CSRF tokens, POST logout, login lock, {bcrypt} for new passwords (`rms/config/SecurityConfig.java`).

Evidence: `pom.xml`, `.mvn/wrapper/maven-wrapper.properties`, `rms/config/WebConfig.java`.

## Build / run / test
- **Build:** with `JAVA_HOME` set to a JDK 21, `./mvnw -B verify` (or `mvnw.cmd`) runs the tests and builds `target/rmsv2-1.0.1-SNAPSHOT.war`.
- **Run:** deploy the WAR to a Servlet 6.1 container (Tomcat 11) on a JDK 21+ runtime that provides the JNDI DataSource `jdbc/springrms` and the environment variable `RMS_DOC_DIR` (a writable folder for candidate documents). Open `/login`. See [docs/build-run.md](docs/build-run.md).
- **Test:** characterization tests live in `src/test/java/rms/`. Controller tests always run; DAO tests need Docker (throwaway MySQL via Testcontainers, never a real DB); `SmokeTest` needs `RMS_BASE_URL`. See [docs/build-run.md](docs/build-run.md).

## Critical rules
- **Database:** don't connect to or change any database unless the user explicitly asks. `db/schema.sql` is inferred from the SQL, not the production DDL; treat it and `docs/data-model.md` as what the SQL references, not the full truth.
- **Secrets:** never copy credentials, JNDI resource definitions or hostnames into code, docs or chat.
- **Git:** work on the `dev` branch; `master` is the default branch. Commit only when asked.
- **Code style:**
  - Match the existing layering (`controller → service → dao → model`), `*Impl` / `*Info` naming, SQL in DAO string fields with named params, and tab indentation. See [docs/conventions.md](docs/conventions.md).
  - Keep tests as characterization tests: pin current behaviour, and mark known defects with `// characterizes Gnn`.
- **Upgrade progress:** done, open and pending items, next actions and new-machine setup are in [docs/upgrade-status.md](docs/upgrade-status.md).
- **Docs:**
  - Keep them evidence-based: cite file paths, and record unconfirmed items in [docs/open-questions.md](docs/open-questions.md).
  - Update the owning file under `docs/` when facts change.

## Repo layout
```
pom.xml                       Maven build (WAR)
src/main/java/rms/
  config/                     WebInitializer (bootstrap), WebConfig (MVC, DataSource), SecurityConfig + SecurityInitializer (Spring Security), AccountCheckFilter, LoginThrottle
  controller/                 Login, Account, User, Position, Language, Candidate, Job, Document, Assignment, Evaluation, Marks, Schedule, Report, Activity
  service/  dao/  model/      *Service(+Impl), *Dao(+Impl), *Info
src/main/webapp/
  WEB-INF/jsp/                23 JSP views (main.jsp = home page)
  WEB-INF/tags/status.tag     decision badge (Selected, Rejected, On hold, Pending)
  WEB-INF/tags/schedulestatus.tag  interview badge (Scheduled, Done, Cancelled)
  WEB-INF/tags/layout.tag     shared page shell: head, sidebar menu, top bar
  WEB-INF/web.xml             Servlet 6.1: request encoding ISO-8859-1, metadata-complete
  resources/                  css (rms.css theme), js (rms.js), img (SVG logo, icon sprite)
src/test/java/rms/            controller, security, service, DAO (Testcontainers) and smoke tests
db/                           schema.sql (inferred), test-seed.sql (synthetic)
  local/                      local MySQL setup: create DB + load schema and seed (README.md)
  migrations/                 numbered changes for existing databases, run by the owner (001 candidate.isactive, 002 roles, 003 password hashing, 004 candidate documents and jobs, 005 assignment, evaluation and decision: a draft, 006 interview schedule, 007 activity log)
mvnw, .mvn/wrapper/           Maven Wrapper
.github/workflows/            CI: build + tests (JDK 21, Testcontainers), OSV-Scanner
deploy/tomcat/rms.xml         JNDI context template (env-var placeholders only)
docs/                         project knowledge; start at docs/ROUTER.md
target/                       build output (git-ignored)
```
