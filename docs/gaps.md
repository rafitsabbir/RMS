# Gaps — Partial & Missing Features

Purpose: Every partially implemented or missing piece of RMS, with evidence, impact and a proposed order of work.
Last updated: 2026-09-25
Read this when: you're building an unfinished feature, fixing a known defect, or planning work. Find the gap ID first, then open the relevant business-flows file.

- **Scope:** RMS was last worked on on 2020-01-17. Modules were added one commit at a time: login, then language, then position (`git log`). The menu shows a full recruitment workflow, but only 4 capabilities are wired end to end (see [business-flows/README.md](business-flows/README.md)).
- **Marker scan:** there are no TODO/FIXME/HACK/XXX, "not implemented" or `UnsupportedOperationException` markers. The 35 Eclipse `// TODO Auto-generated method stub` comments sit above working code, except in `MarksDaoImpl` (G6–G8).

## Findings
| ID | Area | Capability | Type | Status | Evidence (file:line) | Impact | Confidence |
|---|---|---|---|---|---|---|---|
| G1 | Candidate | Candidate management | Missing link | Not implemented | `main.jsp:19-20,50,62,133-138` (`/createcandidate`, `/viewcandidatelist`); no controller in `rms/controller/` | High | Confirmed |
| G2 | Interviewer | Interviewer management | Missing link | Not implemented | `main.jsp:51,63,139-144` → `InterviewerController` is missing from the repo and its history | High | Confirmed |
| G3 | Schedule | Interview scheduling | Missing link | Not implemented | `main.jsp:52,64,145-150` → `ScheduleController` is missing | Med | Confirmed |
| G4 | Job | Job openings | Missing link | Not implemented | `main.jsp:53,65,160-165` → `JobController` is missing | Med | Confirmed |
| G5 | Marks | Interviewer score entry | Missing link | Not implemented | `main.jsp:68-69,151-156` calls `MarksController?user=…`; the only mapping is `/adminviewmarks` (`MarksController.java:22`) | High | Confirmed |
| G6 | Marks | Interviewer score entry | Stub | Not implemented | `MarksDaoImpl.java:62-65` `saveMarks` has an empty body; called from `MarksServiceImpl.java:20-22` | High | Confirmed |
| G7 | Marks | Interviewer's own evaluations | Stub | Not implemented | `MarksDaoImpl.java:74-77` returns `null`; `MarksServiceImpl.java:25-27` | High | Confirmed |
| G8 | Marks | Score total | Dead code | Unused | `MarksDaoImpl.java:68-71` `getFullmarks` returns 0 and is never called; the real total is in `viewmarks.jsp:29-35` | Low | Confirmed |
| G9 | Candidate / Marks | Selection decision | Partial flow | Partial | `candidatestatus` is only read (`MarksDaoImpl.java:19`, `viewmarks.jsp:89-94`); no code writes it | High | Confirmed |
| G10 | Marks | Admin results view | Partial flow | Partial | `MarksDaoImpl.java:40-41` puts the interviewer and candidate *names* into `*id` fields; `:44-55` reads columns by index against `m.*` | Med | Likely: depends on the `marks` column order, which isn't in the repo |
| G11 | All controllers | Access control | Missing link | Not implemented | No interceptor or filter in `WebConfig.java`; no controller reads the `user` session attribute (`rms/controller/*`). Every URL can be opened directly | High | Confirmed |
| G12 | Login | Session | Partial flow | Partial | `LoginController.java:22,43`: `userinfo` is an instance field on a singleton controller, so it's shared across concurrent logins | Med | Confirmed |
| G13 | Login | Authentication | Partial flow | Partial | `LoginDaoImpl.java:19` compares the raw password in SQL; there's no hashing in `src/main/java` | High | Confirmed |
| G14 | Position / Language | Master data | Silent failure | Partial | `PositionDaoImpl.java:66-74`, `LanguageDaoImpl.java:66-74`: a duplicate name only reaches `System.out.println`, and the user is still redirected as if the save worked | Med | Confirmed |
| G15 | Position / Language | Master data | Partial flow | Partial | `PositionDaoImpl.java:91`, `LanguageDaoImpl.java:83`: `queryForObject` with a missing key throws an unhandled error (HTTP 500); there's no `@ExceptionHandler` | Low | Confirmed |
| G16 | UI | Login menu / results | Partial flow | Partial | `main.jsp:42,67` (`getIsinterviewer()`) and `viewmarks.jsp:89,91` (`getCandidateStatus()`) crash with a NullPointerException if the column is NULL | Med | Likely: depends on whether the columns allow NULL |
| G17 | Position / Language | Master data | Partial flow | Partial | Inserts set `isActive=1` and lists filter on `isactive=1` (`PositionDaoImpl.java:23`, `LanguageDaoImpl.java:23`), but delete removes the row (`:24`, `:26`), through a GET (`PositionController.java:68`, `LanguageController.java:68`) | Med | Likely: soft-delete intent inferred |
| G18 | Position / Language | Master data | Partial flow | Partial | `PositionDaoImpl.java:47` `updatePosition` (and `updateLanguage`) doesn't check for duplicates; only add does | Low | Confirmed |
| G19 | Config | Data integrity | Dead code | Unused | `spring-tx` is declared (`pom.xml:35`) but there's no `@Transactional` or transaction manager | Low | Confirmed |
| G20 | UI | — | Dead code | Unused | `resources/js/main.js` is never included; `main.jsp:115-120` `load_menu`/`load_header` are never called and point to missing `jsp/menu.jsp` and `header.jsp`; `resources/header_backgorund.jpg` and `onlinejob.png` aren't referenced | Low | Confirmed |
| G21 | Model | — | Dead code | Unused | `UserInfo.java:8` `password` and `isactive` (`PositionInfo.java:5`, `LanguageInfo.java:5`) are never read or written by the app | Low | Confirmed |
| G22 | Data | All flows | Missing link | Not implemented | Tables `users`, `admin`, `position`, `language`, `candidate`, `marks` are used in `rms/dao/*` but have no DDL, migration or seed data in the repo | High | Confirmed |
| G23 | All | All flows | No tests | Not implemented | No `src/test/` and no test dependency in `pom.xml` | Med | Confirmed |
| G24 | Build | Security | Partial flow | Partial (abandoned) | Three unmerged Dependabot branches from 2022 (`git for-each-ref refs/remotes`); GitHub reports 26 known security issues on `master`; `pom.xml:13,69` still on Spring 4.3.0 and Connector/J 5.1.36 | High | Confirmed |
| G25 | Web | Landing | Missing link | Partial | No handler for `/` (the class-level `@RequestMapping("/")` only prefixes the method mappings); `index.jsp` is "Hello World!" and `web.xml` has no welcome-file | Low | Likely: container behaviour not tested |

Not found: stored procedures, feature flags, `printStackTrace`, empty catch blocks, commented-out logic, WIP commits.

## Counts
- **By type (25 total):** Partial flow 9 · Missing link 8 · Dead code 4 · Stub 2 · Silent failure 1 · No tests 1
- **By impact:** High 10 · Med 8 · Low 7
- **By confidence:** Confirmed 21 · Likely 4

## By business capability
- **Recruitment pipeline:** G1, G2, G3, G4, G5–G7, G8, G9, G10
- **Access & security:** G11, G12, G13, G24
- **Master data (Position/Language):** G14, G15, G17, G18
- **Platform:** G16, G19, G20, G21, G22, G23, G25

## High-impact items
| ID | Missing | Needed to complete | Depends on | Risk | Effort |
|---|---|---|---|---|---|
| G22 | DB schema | Get DDL from the owner or a DB dump; add `db/schema.sql` + seed | Owner access | Nothing can be run or tested without it | M |
| G24 | Security upgrades | Upgrade to Spring 5.3.x (Java 8 compatible, same `javax.servlet`) instead of the 6.0 branch; Connector/J 8.x (driver class and JNDI URL changes) | G22 and a working local environment | `WebMvcConfigurerAdapter` is deprecated in 5.x; the container's JNDI config changes | M |
| G11 | Auth enforcement | `HandlerInterceptor` registered in `WebConfig.addInterceptors`, excluding `/login`, `/welcome`, `/resources/**`; role check for admin URLs (`isinterviewer='N'`) | — | Low | S |
| G13 | Password hashing | BCrypt (e.g. `spring-security-crypto`); `LoginDaoImpl.checkUser` fetches the hash and compares in code; migrate existing rows | G22, owner sign-off | Existing logins break unless passwords are migrated | M |
| G1 | Candidate CRUD | `Candidate{Controller,Service,Dao,Info}` + `createcandidate.jsp`/`viewcandidatelist.jsp`, copying `PositionController`, with position and language dropdowns | G22 | Low; the pattern already exists | M |
| G2 | Interviewer management | CRUD on `admin` with `isinterviewer='Y'` plus a `users` login row; replace the servlet-style link | G22, the users/admin question | Creating logins touches G13 | M |
| G5–G7 | Score entry | `MarksController` GET/POST for the interviewer (10-criteria form); implement `saveMarks` and `getAllMarksByInterviewer`; fix the `main.jsp:151-156` URLs | G1, G2, G22 | Score scale and uniqueness unknown | M |
| G9 | Selection decision | Admin action to set `candidate.candidatestatus`, or a threshold rule | G5–G7, domain rules | Business rule unknown | S–M |

## Proposed order of work
0. **Unblock:**
   - G22: get the schema.
   - Set up a local environment (JDK 8, Maven, Tomcat with JNDI `jdbc/springrms`).
   - G23: add a JUnit + spring-test skeleton.
1. **Quick wins (S each, no domain input needed):** G11, G12, G14, G15, G16, G18, G20/G21, G25.
2. **Critical path:** G24 dependency upgrade → G13 password hashing → G10 map columns by name.
3. **Core features:** G1 Candidate → G2 Interviewer → G5–G7 Score entry → G9 Status decision.
4. **Secondary:** G4 Job, G3 Schedule, G17 soft delete with POST deletes, G19 transactions, and broader tests (G23).

The questions that must be answered before steps 2–4 are in [open-questions.md](open-questions.md).
