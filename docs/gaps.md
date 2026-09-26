# Gaps — Partial & Missing Features

Purpose: Every partially implemented or missing piece of RMS, with evidence, impact, effort and a proposed order of work.
Last updated: 2026-09-26 (Phase 0: G29 and G35 fixed, G22 and G23 partial; G24 plan points to [modernization-plan.md](modernization-plan.md)). 2026-09-25: re-verified against `dev`; G26–G31 from the gap check; G32–G38 and the Critical ratings from a full code review.
Read this when: you're building an unfinished feature, fixing a known defect, or planning work. Find the gap ID first, then open the relevant business-flows file.

- **Scope:** RMS was last worked on on 2020-01-17. Features came in over four commits:
  - `66c19dc`: login.
  - `4b727b0`: Language and Marks, although the commit message only says "folder structure change".
  - `74533c1`: login error handling.
  - `e5705c0`: Position, although the message says "add candidat position".

  The menu shows a full recruitment workflow, but only 4 capabilities are wired end to end (see [business-flows/README.md](business-flows/README.md)).
- **Marker scan:** there are no TODO/FIXME/HACK/XXX, "not implemented" or `UnsupportedOperationException` markers. There are 36 Eclipse `TODO Auto-generated method stub` comments: 35 in Java and one at `viewmarks.jsp:30`. They sit above working code, except in `MarksDaoImpl` (G6–G8). `WebInitializer.java:15-16` returns `null` on purpose.

## Findings
| ID | Area | Capability | Type | Status | Evidence (file:line) | Impact | Effort | Confidence |
|---|---|---|---|---|---|---|---|---|
| G1 | Candidate | Candidate management | Missing link | Not implemented | `main.jsp:19-20,50,62,133-138` (`/createcandidate`, `/viewcandidatelist`); no controller in `rms/controller/` | High | M | Confirmed |
| G2 | Interviewer | Interviewer management | Missing link | Not implemented | `main.jsp:51,63,139-144` → `InterviewerController` is missing from the repo and its history | High | M | Confirmed |
| G3 | Schedule | Interview scheduling | Missing link | Not implemented | `main.jsp:52,64,145-150` → `ScheduleController` is missing | Med | M | Confirmed |
| G4 | Job | Job openings | Missing link | Not implemented | `main.jsp:53,65,160-165` → `JobController` is missing | Med | M | Confirmed |
| G5 | Marks | Interviewer score entry | Missing link | Not implemented | `main.jsp:68-69,151-156` calls `MarksController?user=…`; the only mapping is `/adminviewmarks` (`MarksController.java:22`) | High | M | Confirmed |
| G6 | Marks | Interviewer score entry | Stub | Not implemented | `MarksDaoImpl.java:62-65` `saveMarks` has an empty body; called from `MarksServiceImpl.java:20-22` | High | M | Confirmed |
| G7 | Marks | Interviewer's own evaluations | Stub | Not implemented | `MarksDaoImpl.java:74-77` returns `null`; `MarksServiceImpl.java:25-27` | High | S | Confirmed |
| G8 | Marks | Score total | Dead code | Unused | `MarksDaoImpl.java:68-71` `getFullmarks` returns 0, is never called and isn't on `MarksService`; the real total is in `viewmarks.jsp:29-35` | Low | S | Confirmed |
| G9 | Candidate / Marks | Selection decision | Partial flow | Partial | `candidatestatus` is only read: selected at `MarksDaoImpl.java:22`, mapped at `:55`, shown at `viewmarks.jsp:89-94`. No code writes it | High | S–M | Confirmed |
| G10 | Marks | Admin results view | Partial flow | Partial | `MarksDaoImpl.java:40-41` puts the interviewer and candidate *names* into `*id` fields; `:44-55` reads columns by index against `m.*` | Med | S | Likely: depends on the `marks` column order, which isn't in the repo |
| G11 | All controllers | Access control | Missing link | Not implemented | `WebConfig.java` has no `addInterceptors` and there's no filter; no controller reads the `user` session attribute (`rms/controller/*`). Every URL can be opened directly | **Critical** | S | Confirmed |
| G12 | Login | Session | Partial flow | Partial | `LoginController.java:22,43`: `userinfo` is an instance field on a singleton controller, so it's shared across concurrent logins | Med | S | Confirmed |
| G13 | Login | Authentication | Partial flow | Partial | `LoginDaoImpl.java:19` compares the raw password in SQL; there's no hashing in `src/main/java` | **Critical** | M | Confirmed |
| G14 | Position / Language | Master data | Silent failure | Partial | `PositionDaoImpl.java:66-74`, `LanguageDaoImpl.java:66-74`: a duplicate name only reaches `System.out.println`. Save always redirects (`PositionController.java:43`, `LanguageController.java:52`). The alerts in `createposition.jsp:26-45` and `createlanguage.jsp:27-46` (`errorMessage`/`successMessage`) are never set | Med | S | Confirmed |
| G15 | Position / Language | Master data | Partial flow | Partial | `PositionDaoImpl.java:91`, `LanguageDaoImpl.java:83`: `queryForObject` with a missing key throws an unhandled error (HTTP 500); there's no `@ExceptionHandler` or `@ControllerAdvice` | Low | S | Confirmed |
| G16 | UI | Login menu / results | Partial flow | Partial | `main.jsp:42,67` (`getIsinterviewer()`) and `viewmarks.jsp:89,91` (`getCandidateStatus()`) crash with a NullPointerException if the column is NULL | Med | S | Likely: depends on whether the columns allow NULL |
| G17 | Position / Language | Master data | Partial flow | Partial | Inserts set `isActive=1` (`:72` in both DAOs) and lists filter on `isactive=1` (`:23`), but delete removes the row (`PositionDaoImpl.java:24`, `LanguageDaoImpl.java:26`), through a GET (`PositionController.java:68`, `LanguageController.java:68`) | Med | M | Likely: soft-delete intent inferred |
| G18 | Position / Language | Master data | Partial flow | Partial | `PositionDaoImpl.java:47` `updatePosition` and `LanguageDaoImpl.java:87` `updateLanguage` don't check for duplicates; only add does | Low | S | Confirmed |
| G19 | Config | Data integrity | Dead code | Unused | `spring-tx` is declared (`pom.xml:33-37`) but there's no `@Transactional` or transaction manager | Low | S | Confirmed |
| G20 | UI | — | Dead code | Unused | `resources/js/main.js` is never included. `main.jsp:115-120` `load_menu`/`load_header` are never called and point to missing `jsp/menu.jsp` and `header.jsp`. Three images are never referenced: `resources/header.jpg`, `header_backgorund.jpg` and `onlinejob.png`. `main.css:80` has a commented-out rule. | Low | S | Confirmed |
| G21 | Model | — | Dead code | Unused | `UserInfo.java:8` `password` is only read by the unused `toString` (`:39`). `isactive` is never set on `PositionInfo`/`LanguageInfo` (`:5`), and is set but never read on `UserInfo` (`LoginDaoImpl.java:33`) and `MarksInfo` (`MarksDaoImpl.java:44`) | Low | S | Confirmed |
| G22 | Data | All flows | Missing link | Partial | Tables `users`, `admin` (`LoginDaoImpl.java:19-20`), `position`, `language`, `candidate` and `marks` (`MarksDaoImpl.java:19-24`) had no DDL. Phase 0 added `db/schema.sql`, **inferred** from the SQL and not verified against production, plus synthetic `db/test-seed.sql`. Still missing: the real DDL and migrations (open question #19) | High | M | Confirmed |
| G23 | All | All flows | No tests | Partial | Phase 0 added characterization tests only: controllers (`src/test/java/rms/controller/`, run everywhere), DAOs (`src/test/java/rms/dao/`, need Docker) and an HTTP smoke test (`rms/SmokeTest`, needs a deployed WAR). No service tests, no JSP rendering tests, and the DAO tests have not yet run anywhere | Med | M | Confirmed |
| G24 | Build | Security | Partial flow | Partial (abandoned) | Three unmerged Dependabot branches (2022-06-21, 2022-12-10, 2022-12-16; `git for-each-ref refs/remotes`); `pom.xml:13,69` still on Spring 4.3.0 and Connector/J 5.1.36. GitHub's push message reported 26 known security issues on `master` (2 critical, 9 high, 12 moderate, 3 low as of 2026-09-26; external figure, not verifiable from the code) | High | M | Confirmed |
| G25 | Web | Landing | Missing link | Partial | No handler for `/` (the class-level `@RequestMapping("/")` only prefixes the method mappings; the DispatcherServlet is on `/`, `WebInitializer.java:22`). `index.jsp:3` is "Hello World!" and `web.xml` has no welcome-file | Low | S | Likely: container behaviour not tested |
| G26 | Login | Authentication | Partial flow | Partial | `LoginDaoImpl.java:20,64`: `getUserInfo` uses `queryForObject` on `admin`. A `users` row with no `admin` row throws an unhandled `EmptyResultDataAccessException` (HTTP 500) after the credentials are accepted (`LoginController.java:43`) | Med | S | Likely: depends on whether every `users` row has an `admin` row |
| G27 | UI | Security (output escaping) | Partial flow | Partial | Output isn't escaped: EL at `viewposition.jsp:47` and `viewlanguage.jsp:47`; scriptlets at `main.jsp:81,84,87` and `viewmarks.jsp:74-76`. A script saved in a position or language name runs in other users' browsers (stored XSS); upper-casing (`PositionDaoImpl.java:62-63`) doesn't stop HTML tags. Reachable without login because of G11 | Med | S | Confirmed |
| G28 | Position / Language | Master data | Partial flow | Partial | No `@Valid` or `BindingResult` (`PositionController.java:33-45`, `LanguageController.java:42-54`); blank names are uppercased and saved (`PositionDaoImpl.java:62-73`, `LanguageDaoImpl.java:62-73`); a null name causes a NullPointerException (`LanguageDaoImpl.java:62,91`, `PositionDaoImpl.java:51,62`) | Low | S | Confirmed |
| G29 | Build | Deployment | Dead code | Fixed 2026-09-26 | Was: `target/` tracked in git with a stale WAR that predates `e5705c0`. Now `.gitignore` lists `target/` and it is untracked on `dev`; a fresh `mvnw verify` WAR contains the Position classes and JSPs (`jar tf`) | Low | S | Confirmed |
| G30 | Login | Navigation / session | Partial flow | Partial | The menu is only the response to `POST /welcome` (`LoginController.java:32`): there's no GET home route, and refreshing resubmits the credentials. The not-logged-in fallback at `main.jsp:170-174` only uses `System.out` and redirects to `/login` without the app's context path. The session isn't renewed at login (`LoginController.java:45`) | Low | S | Confirmed (the redirect problem is Likely) |
| G31 | UI | Front-end assets | Partial flow | Partial | `login.jsp:15-18` loads Bootstrap 4 JS before jQuery. `viewlanguage.jsp`, `viewposition.jsp` and `viewmarks.jsp` (head, lines 9-21) load jQuery twice and mix Bootstrap 3 CSS with the Bootstrap 4 DataTables add-on. Everything comes from external CDNs | Low | S | Likely: not run in a browser |
| G32 | All controllers | Security (CSRF) | Missing link | Not implemented | No CSRF token anywhere (`grep -ri csrf` finds nothing). POST forms have no token: `createposition.jsp:20`, `createlanguage.jsp:21`, `login.jsp:30`. Deletes are GET links (`viewposition.jsp:53-54`, `viewlanguage.jsp:53-54` → `PositionController.java:68`, `LanguageController.java:68`), and so is logout (`main.jsp:71` → `LoginController.java:24`). Another site can make a logged-in user's browser delete or save data. This becomes the main exposure once G11 is fixed | Med | M | Confirmed |
| G33 | Position / Language | Master data | Partial flow | Partial | The duplicate check uses `queryForObject(ifexist, …, String.class)` (`PositionDaoImpl.java:66-67`, `LanguageDaoImpl.java:66-67`). Updates can create duplicate names (G18), and after that every add of the name throws `IncorrectResultSizeDataAccessException` (HTTP 500), which isn't caught. The check-then-insert is also racy, because no unique constraint is known (G22). Fix with `select count(*)` plus a `UNIQUE` index | Med | S | Confirmed |
| G34 | UI | Internationalisation | Missing link | Not implemented | All 7 JSPs declare `ISO-8859-1` (`WEB-INF/jsp/*.jsp`, lines 1-2). There's no `CharacterEncodingFilter` or `getServletFilters()` in `WebInitializer.java`, so non-Latin input (e.g. Bengali names) is garbled | Low | S | Confirmed |
| G35 | Build | Build portability | Missing link | Fixed 2026-09-26 | Was: no compiler level, so Maven defaulted to Java 1.5 (the old `master` WAR has class version 49). Now `pom.xml:16-17` sets 1.8 and `pom.xml:125-148` pins the compiler, surefire and war plugins | Low | S | Confirmed |
| G36 | Login | Session | Partial flow | Partial | `UserInfo.java:3` isn't `Serializable`, but it's stored in the session (`LoginController.java:45`, `main.jsp:34`). Persisting or replicating sessions fails, and users are logged out silently | Low | S | Confirmed |
| G37 | Config | Web descriptor | Partial flow | Partial | `web.xml:1-3` uses the Servlet 2.3 DTD, under which EL is ignored by default. Each JSP only works by opting in (`isELIgnored="false"`); `viewmarks.jsp:1-2` doesn't opt in, so any EL added there would print as literal text | Low | S | Confirmed |
| G38 | UI / Marks | Security (XSS, IDOR) | Partial flow | Partial | `main.jsp:152,155` write `userinfo.getUserid()` unescaped into a JavaScript string, and pass it as the client-side `user` parameter to the planned score-entry URL. That's an XSS sink not covered by G27, and G5 might trust the parameter instead of the session (IDOR: acting on another user's data) | Low | S | Confirmed |

**Not found:** stored procedures, feature flags, `printStackTrace`, empty catch blocks, commented-out Java logic (only the `main.css:80` rule, G20), and WIP commits. The messages of `4b727b0` and `e5705c0` don't describe their contents.

## Counts
- **By type (38 total):** Partial flow 18 · Missing link 11 · Dead code 5 · Stub 2 · Silent failure 1 · No tests 1
- **By impact:** Critical 2 · High 8 · Med 12 · Low 16
- **By confidence:** Confirmed 31 · Likely 7

## By business capability
- **Recruitment pipeline:** G1, G2, G3, G4, G5–G7, G8, G9, G10
- **Access & security:** G11, G12, G13, G24, G26, G27, G30, G32, G36, G38
- **Master data (Position/Language):** G14, G15, G17, G18, G28, G33
- **Platform / UI:** G16, G19, G20, G21, G22, G23, G25, G29, G31, G34, G35, G37

## Critical & high-impact items
| ID | Missing | Needed to complete | Depends on | Risk | Effort |
|---|---|---|---|---|---|
| G22 | Verified DB schema | Replace the inferred `db/schema.sql` with a DDL-only export from the owner; rerun the DAO tests | Owner access | DAO tests only prove behaviour against the inferred schema | S |
| G24 | Security upgrades | Phased per [modernization-plan.md](modernization-plan.md):<br>• Spring 5.3.39 via the Spring BOM as a waypoint (Java 8, `javax`)<br>• JDK 21<br>• the target, Spring 7.0 + Jakarta + Tomcat 11<br>• `com.mysql:mysql-connector-j` in the series matching the server (driver class and JNDI URL change)<br>Close the three Dependabot branches | G22, G23 (Phase 0 safety net) | `WebMvcConfigurerAdapter` must go before 6.x; the container's JNDI config changes; the JDK must not move before Spring (Spring4Shell) | M |
| G11 (Critical) | Auth enforcement | `HandlerInterceptor` registered in `WebConfig.addInterceptors`, excluding `/login`, `/welcome`, `/resources/**`; role check for admin URLs (`isinterviewer='N'`) | — | Low | S |
| G13 (Critical) | Password hashing | BCrypt (e.g. `spring-security-crypto`); `LoginDaoImpl.checkUser` fetches the hash and compares in code; migrate existing rows | G22, owner sign-off | Existing logins break unless passwords are migrated | M |
| G1 | Candidate CRUD | `Candidate{Controller,Service,Dao,Info}` + `createcandidate.jsp`/`viewcandidatelist.jsp`, copying `PositionController`, with position and language dropdowns | G22 | Low; the pattern already exists | M |
| G2 | Interviewer management | CRUD on `admin` with `isinterviewer='Y'` plus a `users` login row; replace the servlet-style link | G22, the users/admin question | Creating logins touches G13 | M |
| G5–G7 | Score entry | `MarksController` GET/POST for the interviewer (10-criteria form); implement `saveMarks` and `getAllMarksByInterviewer`; fix the `main.jsp:151-156` URLs | G1, G2, G22 | Score scale and uniqueness unknown | M |
| G9 | Selection decision | Admin action to set `candidate.candidatestatus`, or a threshold rule | G5–G7, domain rules | Business rule unknown | S–M |

## Proposed order of work
0. **Unblock:**
   - Done in Phase 0 (2026-09-26): G29, G35, the inferred schema and the characterization tests (G22, G23 partial).
   - G22: get the real DDL to replace the inferred one.
   - Set up a local environment (Docker for the DAO tests, Tomcat with JNDI `jdbc/springrms`).
1. **Quick wins (S each, no domain input needed):**
   - G11 (Critical), G27, G38
   - G12, G14, G33, G15, G16, G18
   - G26, G28, G30, G36, G34, G37
   - G20/G21, G25, G31
2. **Critical path:** G32 CSRF with POST-only state changes (build it on the G11 interceptor) → G24 dependency upgrade (phases and gates in [modernization-plan.md](modernization-plan.md)) → G13 password hashing (Critical) → G10 map columns by name.
3. **Core features:** G1 Candidate → G2 Interviewer → G5–G7 Score entry → G9 Status decision.
4. **Secondary:** G4 Job, G3 Schedule, G17 soft delete with POST deletes, G19 transactions, and broader tests (G23).

The questions that must be answered before steps 2–4 are in [open-questions.md](open-questions.md).
