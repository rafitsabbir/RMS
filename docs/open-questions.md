# Open Questions

Purpose: Unresolved items and partial flows that need confirmation from the owner or a database.
Last updated: 2026-10-01 (#15 answered for Phase 3; #4 adds the runtime JDK and Security Manager for Phase 3). 2026-10-01 (#16: Phase 1 rehearsed on MySQL 5.7). 2026-09-30 (#12 extended; #20 added after the G11 fix; #21 added after the G27 fix)
Read this when: your task touches one of the areas below, or before you assume something that isn't documented elsewhere.

## Partial flows
Partial and missing features are tracked with evidence in [gaps.md](gaps.md): candidate (G1), interviewer (G2), schedule (G3), job (G4), interviewer score entry (G5–G7), and the other gaps.

## Unresolved
1. **Results page mapping:** `MarksMapper` puts column 1 (`interviewername`) into `setInterviewerid` and column 2 (`candidatename`) into `setCandidateid`. The score columns depend on the column order of `marks`, which isn't in the repo. Does the page show the intended values? (`MarksDaoImpl.java`)
2. **Candidate status:** what sets `candidate.candidatestatus`, and does `S`/`R` mean Selected/Rejected? No code writes it (`viewmarks.jsp`, `MarksDaoImpl.java`).
3. **User tables:** how do `users` and `admin` relate? Login reads `users`; the profile and interviewer names read `admin` (`LoginDaoImpl.java`, `MarksDaoImpl.java`). If a `users` row can exist without an `admin` row, login fails with an HTTP 500 error (G26).
4. **Environment:** which servlet container is the target, and where are the DB schema and the `jdbc/springrms` JNDI setup defined? None are in the repo (`WebConfig.java`). The build JDK was 1.8 (WAR `MANIFEST.MF`), but the intended runtime JDK isn't confirmed. Phase 3 needs a JDK 21+ runtime as built (17 if the owner picks that standard; `release` 17 also builds). Does production Tomcat run with a Security Manager (`-security` or `-Djava.security.manager`)? Tomcat 11 has dropped Security Manager support [assumption: Tomcat 11 release notes], so a policy that depends on it needs another answer before Phase 3.
5. **Deletes:** should they be soft deletes (`isactive = 0`)? What happens to `candidate` rows that point at a deleted position or language? (`*DaoImpl.delete*`)
6. **Build output:** *Resolved 2026-09-26:* `target/` is now git-ignored (`.gitignore`) and untracked on `dev` (Phase 0, G29). Kept here as a numbering placeholder.
7. **Dependabot upgrades:** *Resolved 2026-09-26:* the owner approved deleting the three stale branches (MySQL 8.0.28, spring-web 6.0.0, spring-webmvc 5.2.20), and they were deleted. Phase 1 upgrades through the Spring BOM instead (G24). GitHub reported 26 alerts on `master` before Phase 1 (2 critical, 9 high, 12 moderate, 3 low; external figure). Whether `.github/dependabot.yml` should be added was left out of Phase 1.
8. **Scoring rules:** what is the score scale for each of the 10 criteria? Can several interviewers score the same candidate? Should the total be weighted? (`MarksInfo.java`, `viewmarks.jsp:33-40`; blocks G5–G7)
9. **Job vs Position:** how is "Job" different from "Position"? Is it an opening with vacancies and dates? (`main.jsp:54,66`; blocks G4)
10. **Interview schedule:** what should a schedule hold (candidate, interviewer, date/time, location/link), and are notifications needed? (`main.jsp:53,65`; blocks G3)
11. **Password migration:** can existing plain-text passwords be migrated to hashes, and how should current users be switched over? (`LoginDaoImpl.java:19`; blocks G13)
12. **Nullable columns:** can `admin.isinterviewer` or `candidate.candidatestatus` be NULL? If so, `main.jsp:43,68` and `viewmarks.jsp:93,95` crash with a NullPointerException (G16). Since the G11 fix (2026-09-30), such a user also gets HTTP 403 on every page (`AuthInterceptor`).
13. **Unique users:** can two `users` rows share the same username and password? If so, `LoginDaoImpl.java:53` throws `IncorrectResultSizeDataAccessException` (HTTP 500). The same applies if `admin.userid` or `candidate.candidateid` aren't unique, which the subqueries at `MarksDaoImpl.java:19-21` rely on. This depends on the schema (G22).
14. **HTTPS:** is TLS enforced in front of the app? `web.xml` has no `<security-constraint>`, and the login form POSTs the password in plain text (G13).
15. **Old web.xml format:** does the target Tomcat handle the Servlet 2.3 `web.xml` correctly alongside `SpringServletContainerInitializer`? It evidently ran in 2020, but this hasn't been tested (G37). Since Phase 1 two container initializers are involved: Spring's and Logback's `LogbackServletContainerInitializer` (from `logback-classic-1.3.16.jar`). If the old `web.xml` format stopped them running, Spring wouldn't start at all, while Logback would only lose its clean shutdown. **Answered for Phase 3 (2026-10-01):** `web.xml` now uses the Servlet 6.1 schema. On Tomcat 11.0.26 Spring started and Logback logged; Logback 1.5.38 registers a `jakarta.servlet.ServletContainerInitializer`.
16. **MySQL server version:** which MySQL server version runs in production? Phase 1 uses Connector/J 8.2.0 because it is the newest release that supports MySQL 5.7; 8.3.0 and later support 8.0+ only (Connector/J release notes, `pom.xml:118-129`). Phase 1 was rehearsed on MySQL 5.7.44 on 2026-10-01 ([release-phase1.md](release-phase1.md)). Once the server is known to be 8.0 or later, move to the newest series it supports. The DAO tests assume `mysql:8.0` (`MySqlContainerSupport.java`), a line that is itself past end of life; switch the image to the real version once known.
17. **Runtime ownership and usage:**
    - Is RMS in production use? The last commit was 2020-01-17 (`git log`).
    - Who owns the container and runtime JDK?
    - Is the MySQL driver jar in the container's `lib/` for the JNDI pool (`WebConfig.java:32-38`)?
    - Who can act as domain testers?
    - Is downtime acceptable for the Phase 1 and Phase 3 cutovers?
18. **Docker availability:** is Docker available on dev machines and in CI? The DAO tests (`src/test/java/rms/dao/`) use Testcontainers MySQL and are skipped without Docker ([modernization-plan.md](modernization-plan.md) Phase 0; G23). Docker is not installed on the machine used on 2026-09-26, so the DAO tests were skipped there. The code reviewer recalled that Testcontainers before 1.21.4 can't connect to Docker Engine 29+ (raised minimum API version); the pom uses 1.21.4 for that reason. *Checked 2026-09-27:* Testcontainers 1.21.4 works with Docker Engine 29.3.1, and all 18 DAO tests ran (older Testcontainers versions weren't tried). Still open: whether dev machines and CI will have Docker.
19. **Inferred schema:** `db/schema.sql` was reverse-engineered from the DAO SQL. Do the real types, keys, NULL rules and the `marks` column order match? A DDL-only export (`mysqldump --no-data`, no data, no credentials) would settle it (G22, [data-model.md](data-model.md)).
20. **Inactive admins and user IDs:** the login doesn't check `admin.isactive` (G21), so a deactivated admin can still log in and passes the G11 check (`AuthInterceptor`). Is that intended? And what do production `userid` values look like? A refused request logs the userid at WARN, which would put personal data in the log if it's an email address or an employee number.
21. **Names stored as codes or control characters (G34):** the ISO-8859-1 forms store characters outside windows-1252 as `&#NNNN;` codes. This includes Cyrillic, Bengali and Latin letters such as ł and č. They store €, š, dashes and curly quotes as control characters U+0080–U+009F.
    - **What changed:** since the G27 fix (2026-09-30) the lists show the codes instead of the letters. The control characters still display as before.
    - **Question:** do any production position or language names contain either kind? Read-only checks for the owner:
      - `SELECT positionkey FROM position WHERE positionname LIKE '%&#%'`, for the codes.
      - `SELECT positionkey FROM position WHERE positionname REGEXP '[\\x{0080}-\\x{009F}]'`, for the control characters. This was tested on MySQL 8.0.46; MySQL 5.7's REGEXP may not accept it (#16).
      - The same queries on `language` (`languagekey`, `languagename`).
    - **If any exist:** fixing G34 needs a one-off conversion of those rows.
    - **Names entered outside the forms:** candidate and user names aren't entered through RMS forms, so they show exactly as stored. Do any of them contain intended markup or entities such as `&amp;`? Those now show literally.
