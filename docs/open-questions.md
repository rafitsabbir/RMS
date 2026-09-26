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
6. **Build output:** is committing `target/` (and having no `.gitignore`) intentional? (`git ls-files target`)
7. **Dependabot upgrades:** should the three unmerged branches (MySQL 8.0.28, spring-web 6.0.0, spring-webmvc 5.2.20) be closed? GitHub's push message reported 26 known security issues on `master`; this is an external figure that can't be verified from the code. Both Spring branches change the shared `spring.version` (`pom.xml:13`), so the 6.0.0 branch would move every Spring module to 6.0, and it can't compile here (`javax.servlet`, `WebMvcConfigurerAdapter`, JDK 17). The [modernization-plan.md](modernization-plan.md) Phase 1 proposes closing all three and upgrading through the Spring BOM instead (G24).
8. **Scoring rules:** what is the score scale for each of the 10 criteria? Can several interviewers score the same candidate? Should the total be weighted? (`MarksInfo.java`, `viewmarks.jsp:29-35`; blocks G5–G7)
9. **Job vs Position:** how is "Job" different from "Position"? Is it an opening with vacancies and dates? (`main.jsp:53,65`; blocks G4)
10. **Interview schedule:** what should a schedule hold (candidate, interviewer, date/time, location/link), and are notifications needed? (`main.jsp:52,64`; blocks G3)
11. **Password migration:** can existing plain-text passwords be migrated to hashes, and how should current users be switched over? (`LoginDaoImpl.java:19`; blocks G13)
12. **Nullable columns:** can `admin.isinterviewer` or `candidate.candidatestatus` be NULL? If so, `main.jsp:42,67` and `viewmarks.jsp:89,91` crash with a NullPointerException (G16).
13. **Unique users:** can two `users` rows share the same username and password? If so, `LoginDaoImpl.java:53` throws `IncorrectResultSizeDataAccessException` (HTTP 500). The same applies if `admin.userid` or `candidate.candidateid` aren't unique, which the subqueries at `MarksDaoImpl.java:19-21` rely on. This depends on the schema (G22).
14. **HTTPS:** is TLS enforced in front of the app? `web.xml` has no `<security-constraint>`, and the login form POSTs the password in plain text (G13).
15. **Old web.xml format:** does the target Tomcat handle the Servlet 2.3 `web.xml` correctly alongside `SpringServletContainerInitializer`? It evidently ran in 2020, but this hasn't been tested (G37).
16. **MySQL server version:** which MySQL server version runs in production? The newest Connector/J series supports only 8.4+, so this decides the driver series in [modernization-plan.md](modernization-plan.md) Phase 1 (`pom.xml:66-70`).
17. **Runtime ownership and usage:**
    - Is RMS in production use? The last commit was 2020-01-17 (`git log`).
    - Who owns the container and runtime JDK?
    - Is the MySQL driver jar in the container's `lib/` for the JNDI pool (`WebConfig.java:31-37`)?
    - Who can act as domain testers?
    - Is downtime acceptable for the Phase 1 and Phase 3 cutovers?
18. **Docker availability:** is Docker available on dev machines and in CI? The planned DAO tests use Testcontainers MySQL ([modernization-plan.md](modernization-plan.md) Phase 0; G23).
