# Open Questions

Purpose: Unresolved items and partial flows that need confirmation from the owner or a database.
Last updated: 2026-09-26
Read this when: your task touches one of the areas below, or before you assume something that isn't documented elsewhere.

## Partial flows
Partial and missing features are tracked with evidence in [gaps.md](gaps.md): candidate (G1), interviewer (G2), schedule (G3), job (G4), interviewer score entry (G5–G7), and the other gaps.

## Unresolved
1. **Results page mapping:** `MarksMapper` puts column 1 (`interviewername`) into `setInterviewerid` and column 2 (`candidatename`) into `setCandidateid`. The score columns depend on the column order of `marks`, which isn't in the repo. Does the page show the intended values? (`MarksDaoImpl.java`)
2. **Candidate status:** what sets `candidate.candidatestatus`, and does `S`/`R` mean Selected/Rejected? No code writes it (`viewmarks.jsp`, `MarksDaoImpl.java`).
3. **User tables:** how do `users` and `admin` relate? Login reads `users`; the profile and interviewer names read `admin` (`LoginDaoImpl.java`, `MarksDaoImpl.java`). If a `users` row can exist without an `admin` row, login fails with an HTTP 500 error (G26).
4. **Environment:** which servlet container is the target, and where are the DB schema and the `jdbc/springrms` JNDI setup defined? None are in the repo (`WebConfig.java`). The build JDK was 1.8 (WAR `MANIFEST.MF`), but the intended runtime JDK isn't confirmed.
5. **Deletes:** should they be soft deletes (`isactive = 0`)? What happens to `candidate` rows that point at a deleted position or language? (`*DaoImpl.delete*`)
6. **Build output:** *Resolved 2026-09-26:* `target/` is now git-ignored (`.gitignore`) and untracked on `dev` (Phase 0, G29). Kept here as a numbering placeholder.
7. **Dependabot upgrades:** *Resolved 2026-09-26:* the owner approved deleting the three stale branches (MySQL 8.0.28, spring-web 6.0.0, spring-webmvc 5.2.20), and they were deleted. Phase 1 upgrades through the Spring BOM instead (G24). GitHub reported 26 alerts on `master` before Phase 1 (2 critical, 9 high, 12 moderate, 3 low; external figure). Whether `.github/dependabot.yml` should be added was left out of Phase 1.
8. **Scoring rules:** what is the score scale for each of the 10 criteria? Can several interviewers score the same candidate? Should the total be weighted? (`MarksInfo.java`, `viewmarks.jsp:29-35`; blocks G5–G7)
9. **Job vs Position:** how is "Job" different from "Position"? Is it an opening with vacancies and dates? (`main.jsp:53,65`; blocks G4)
10. **Interview schedule:** what should a schedule hold (candidate, interviewer, date/time, location/link), and are notifications needed? (`main.jsp:52,64`; blocks G3)
11. **Password migration:** can existing plain-text passwords be migrated to hashes, and how should current users be switched over? (`LoginDaoImpl.java:19`; blocks G13)
12. **Nullable columns:** can `admin.isinterviewer` or `candidate.candidatestatus` be NULL? If so, `main.jsp:42,67` and `viewmarks.jsp:89,91` crash with a NullPointerException (G16).
13. **Unique users:** can two `users` rows share the same username and password? If so, `LoginDaoImpl.java:53` throws `IncorrectResultSizeDataAccessException` (HTTP 500). The same applies if `admin.userid` or `candidate.candidateid` aren't unique, which the subqueries at `MarksDaoImpl.java:19-21` rely on. This depends on the schema (G22).
14. **HTTPS:** is TLS enforced in front of the app? `web.xml` has no `<security-constraint>`, and the login form POSTs the password in plain text (G13).
15. **Old web.xml format:** does the target Tomcat handle the Servlet 2.3 `web.xml` correctly alongside `SpringServletContainerInitializer`? It evidently ran in 2020, but this hasn't been tested (G37).
16. **MySQL server version:** which MySQL server version runs in production? Phase 1 uses Connector/J 8.2.0 because it is the newest release that supports MySQL 5.7; 8.3.0 and later support 8.0+ only (Connector/J release notes, `pom.xml:93-104`). Once the server is known to be 8.0 or later, move to the newest series it supports. The DAO tests assume `mysql:8.0` (`MySqlContainerSupport.java`), a line that is itself past end of life; switch the image to the real version once known.
17. **Runtime ownership and usage:**
    - Is RMS in production use? The last commit was 2020-01-17 (`git log`).
    - Who owns the container and runtime JDK?
    - Is the MySQL driver jar in the container's `lib/` for the JNDI pool (`WebConfig.java:31-37`)?
    - Who can act as domain testers?
    - Is downtime acceptable for the Phase 1 and Phase 3 cutovers?
18. **Docker availability:** is Docker available on dev machines and in CI? The DAO tests (`src/test/java/rms/dao/`) use Testcontainers MySQL and are skipped without Docker ([modernization-plan.md](modernization-plan.md) Phase 0; G23). Docker is not installed on the machine used on 2026-09-26, so the DAO tests were skipped there. *Unverified:* the code reviewer recalled that Testcontainers before 1.21.4 can't connect to Docker Engine 29+ (raised minimum API version); the pom uses 1.21.4 for that reason. Confirm on the first run with a current Docker.
19. **Inferred schema:** `db/schema.sql` was reverse-engineered from the DAO SQL. Do the real types, keys, NULL rules and the `marks` column order match? A DDL-only export (`mysqldump --no-data`, no data, no credentials) would settle it (G22, [data-model.md](data-model.md)).
