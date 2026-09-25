# Open Questions

Purpose: Unresolved items and partial flows that need confirmation from the owner or a database.
Last updated: 2026-09-25
Read this when: your task touches one of the areas below, or before you assume something that isn't documented elsewhere.

## Partial flows
Partial and missing features are tracked with evidence in [gaps.md](gaps.md): candidate (G1), interviewer (G2), schedule (G3), job (G4), interviewer score entry (G5–G7), and the other gaps.

## Unresolved
1. **Results page mapping:** `MarksMapper` puts column 1 (`interviewername`) into `setInterviewerid` and column 2 (`candidatename`) into `setCandidateid`. The score columns depend on the column order of `marks`, which isn't in the repo. Does the page show the intended values? (`MarksDaoImpl.java`)
2. **Candidate status:** what sets `candidate.candidatestatus`, and does `S`/`R` mean Selected/Rejected? No code writes it (`viewmarks.jsp`, `MarksDaoImpl.java`).
3. **User tables:** how do `users` and `admin` relate? Login reads `users`; the profile and interviewer names read `admin` (`LoginDaoImpl.java`, `MarksDaoImpl.java`).
4. **Environment:** which servlet container is the target, and where are the DB schema and the `jdbc/springrms` JNDI setup defined? None are in the repo (`WebConfig.java`). The build JDK was 1.8 (WAR `MANIFEST.MF`), but the intended runtime JDK isn't confirmed.
5. **Deletes:** should they be soft deletes (`isactive = 0`)? What happens to `candidate` rows that point at a deleted position or language? (`*DaoImpl.delete*`)
6. **Build output:** is committing `target/` (and having no `.gitignore`) intentional? (`git ls-files target`)
7. **Dependabot upgrades:** should the three unmerged branches (MySQL 8.0.28, spring-web 6.0.0, spring-webmvc 5.2.20) be merged? GitHub reports 26 known security issues on `master`. spring-web 6.0.0 on its own wouldn't be compatible with Spring 4.3 (`git branch -r`, `pom.xml`). See G24 in [gaps.md](gaps.md).
8. **Scoring rules:** what is the score scale for each of the 10 criteria? Can several interviewers score the same candidate? Should the total be weighted? (`MarksInfo.java`, `viewmarks.jsp:29-35`; blocks G5–G7)
9. **Job vs Position:** how is "Job" different from "Position"? Is it an opening with vacancies and dates? (`main.jsp:53,65`; blocks G4)
10. **Interview schedule:** what should a schedule hold (candidate, interviewer, date/time, location/link), and are notifications needed? (`main.jsp:52,64`; blocks G3)
11. **Password migration:** can existing plain-text passwords be migrated to hashes, and how should current users be switched over? (`LoginDaoImpl.java:19`; blocks G13)
12. **Nullable columns:** can `admin.isinterviewer` or `candidate.candidatestatus` be NULL? If so, `main.jsp:42,67` and `viewmarks.jsp:89,91` crash with a NullPointerException (G16).
