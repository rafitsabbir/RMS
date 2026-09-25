# Open Questions

Purpose: Unresolved items and partial flows that need confirmation from the owner or a database.
Last updated: 2026-09-25
Read this when: your task touches one of the areas below, or before you assume something that isn't documented elsewhere.

## Partial flows (menu entry exists, but the code path is incomplete)
| Flow | What exists | What's missing | Evidence |
|---|---|---|---|
| Interviewer score entry | Menu items "Evaluation" and "Show Evaluation" for `isinterviewer=Y`; `MarksService.saveMarks` and `getAllMarksByInterviewer` | `MarksDaoImpl.saveMarks` is empty, `getAllMarksByInterviewer` returns `null`, and no mapping matches `MarksController?user=…` | `main.jsp`, `MarksDaoImpl.java`, `MarksController.java` |
| Candidate management | Menu links to `/createcandidate` and `/viewcandidatelist` | No controller, service or DAO | `main.jsp` (the `spring:url` values) |
| Interviewer, Job, Interview Schedule | Menu links to `InterviewerController`, `JobController`, `ScheduleController` (servlet-style `?param=VIEW`) | Not in the repo or its git history | `main.jsp`, `git log` |

## Unresolved
1. **Results page mapping:** `MarksMapper` puts column 1 (`interviewername`) into `setInterviewerid` and column 2 (`candidatename`) into `setCandidateid`. The score columns depend on the column order of `marks`, which isn't in the repo. Does the page show the intended values? (`MarksDaoImpl.java`)
2. **Candidate status:** what sets `candidate.candidatestatus`, and does `S`/`R` mean Selected/Rejected? No code writes it (`viewmarks.jsp`, `MarksDaoImpl.java`).
3. **User tables:** how do `users` and `admin` relate? Login reads `users`; the profile and interviewer names read `admin` (`LoginDaoImpl.java`, `MarksDaoImpl.java`).
4. **Environment:** which servlet container is the target, and where are the DB schema and the `jdbc/springrms` JNDI setup defined? None are in the repo (`WebConfig.java`). The build JDK was 1.8 (WAR `MANIFEST.MF`), but the intended runtime JDK isn't confirmed.
5. **Deletes:** should they be soft deletes (`isactive = 0`)? What happens to `candidate` rows that point at a deleted position or language? (`*DaoImpl.delete*`)
6. **Build output:** is committing `target/` (and having no `.gitignore`) intentional? (`git ls-files target`)
7. **Dependabot upgrades:** should the three unmerged branches (MySQL 8.0.28, spring-web 6.0.0, spring-webmvc 5.2.20) be merged? GitHub reports 26 known security issues on `master`. spring-web 6.0.0 on its own wouldn't be compatible with Spring 4.3 (`git branch -r`, `pom.xml`).
