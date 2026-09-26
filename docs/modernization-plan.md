# Modernization Plan

Purpose: The agreed plan for bringing RMS up to a supported stack: current state, target stack with reasons, phases, risks and gates.
Last updated: 2026-09-26 (Phase 1 code on `dev`, not released; Phase 0 owner steps still open)
Read this when: you're upgrading libraries, the JDK, the framework, the servlet container or the DB driver, or planning a security fix to the stack.

**Status and rules:**
- **Status:** approved. Phases 0 and 1 are coded on `dev` (2026-09-26); see their status blocks. Neither is signed off, so Phase 1 is not releasable yet. Phases 2–4 have not started.
- **Go-ahead:** every phase needs its own go-ahead, because it needs builds (see [CLAUDE.md](../CLAUDE.md) critical rules).
- **Principle:** make the smallest change that restores security patches, vendor support and maintainability:
  - no rewrite
  - no database migration
  - no schema or data change in any phase

**Labels:**
- **[C]** Confirmed by evidence in the repo; the file path is given.
- **[A]** Assumption: taken from an external source (named, checked 2026-09-25) or inferred. Re-check it when the phase runs.

**Scope notes:**
- **Database:** RMS uses **MySQL, not Oracle** (`pom.xml:66-70`) [C].
- **Stored procedures:** there are none, and no SQL calls one ([data-model.md](data-model.md)) [C].
- **What this changes:** database compatibility below means MySQL compatibility.

## 1. Current state
*This section is the assessment from before Phase 0 (2026-09-25). For what Phase 0 has since changed, see its status block in section 4.*

The application code is modern-friendly. The real blockers are:
- **No safety net:** no tests, no schema, no environment.
- **Unknown runtime:** the container, runtime JDK and MySQL server version are all configured outside the repo.

Versions are listed in [tech-stack.md](tech-stack.md); this table adds support status and risk.

**GitHub security alerts:** the push message of 2026-09-26 reported **26** Dependabot alerts on the default branch `master`:

| Severity | Alerts |
|---|---|
| Critical | 2 |
| High | 9 |
| Moderate | 12 |
| Low | 3 |

This is an external figure: which alert belongs to which dependency can't be verified from the code [A]. Phase 1 is meant to close the Spring and driver alerts; re-check the count after each phase.

| Component | Current | Support status | Known security risk | Upgrade difficulty | Evidence |
|---|---|---|---|---|---|
| JDK | Built with 1.8.0_221; the pom sets no level (Maven's default is 1.5); Eclipse is set to 1.5 | Temurin 8 is patched until 2030-12-31, but Spring 6/7 and Tomcat 10.1/11 need 17+ [A] | The runtime JDK is unknown. If it's still 8u221 (2019), it's missing years of fixes [A] | **Low** for code; blocked by G35 and the old war plugin | WAR `META-INF/MANIFEST.MF`, `pom.xml:12-17`, `.settings/org.eclipse.jdt.core.prefs:4,6,16` [C] |
| Build | Maven; `maven-war-plugin` 2.3; no compiler or surefire plugin, wrapper or `sourceEncoding` | Maven 3.9 is current [A] | The build isn't reproducible. War plugin 2.x is known to fail on JDK 9+ [A] | **Low** | `pom.xml:74-88` [C] |
| Servlet container | External, not named; Servlet API 3.1 `provided` | Tomcat 8.5 and older are EOL. **Tomcat 9 support ends 2027-03-31**; 10.1 and 11 are active [A] | Unknown until the container is identified | **Med** (ops-owned) | `pom.xml:59-64`, `WebInitializer.java` [C] |
| Spring Framework | 4.3.0.RELEASE, the first 4.3 release | EOL 2020-12-31 (the last patch was 4.3.30). Only **7.0** has open-source support today [A] | Many CVEs published after 2016 [A]. **Spring4Shell (CVE-2022-22965)** needs JDK 9+, a WAR on Tomcat and POJO binding; RMS binds POJOs | 5.3: **Low**. 7.0: **Med** | `pom.xml:13,21-49` [C]; binding at `PositionController.java:35`, `LanguageController.java:44` [C]; G24 |
| Persistence | Spring JDBC `NamedParameterJdbcTemplate`, no ORM | Follows Spring | Named parameters throughout; no injection pattern found [C] | **Low**: the API is unchanged from 4.3 to 7.0 [A] | `rms/dao/*DaoImpl.java` [C] |
| JDBC driver | `mysql:mysql-connector-java` 5.1.36 | The 5.1 line is legacy; its coordinates were replaced by `com.mysql:mysql-connector-j`. Current series: 8.4.0, 9.7.0 and 26.7.0; the newest supports **MySQL 8.4+** only [A] | 5.1.x has several later-fixed CVEs [A] | **Med**: driver class and URL properties change; the container probably loads the driver [A] | `pom.xml:66-70` [C] |
| MySQL server | Version unknown | 5.7 and 8.0 are EOL; 8.4 and 9.7 are the supported LTS lines [A] | Out of scope (no DB migration), but it decides the driver series | — | Not in the repo (G22) [C] |
| Servlet / JSTL APIs | `javax.servlet-api` 3.1.0, `javax.servlet:jstl` 1.2 | Replaced by the Jakarta versions [A] | Only the core JSTL tags are used [C] | **Low** now, **Med** at the Jakarta switch | `pom.xml:52-64`, `viewposition.jsp:3`, `viewlanguage.jsp:3` [C] |
| Views | 7 JSPs with scriptlets, Spring form/url tags, ISO-8859-1 | Supported in Spring 7 MVC [A] | XSS (G27, G38) is a code issue, not a version issue | **Low** (kept as-is) | `WEB-INF/jsp/*.jsp:1-6` [C] |
| Web descriptor | Servlet 2.3 DTD | Obsolete format | — | **Low** | `web.xml:1-3` [C], G37 |
| Front-end (CDN) | Bootstrap 3.3.7 and 4.1.1, jQuery 3.2.1 and 3.3.1, DataTables 1.10.19, Font Awesome 4.7 | Out of date; Bootstrap 3 and 4 are EOL [A] | XSS and prototype-pollution CVEs, fixed in jQuery 3.5+, Bootstrap 3.4.1 and 4.3.1+ [A] | **Low** for in-major bumps; **High** for merging on one Bootstrap version (out of scope) | `login.jsp:13-18`, `create*.jsp:10-14`, `view*.jsp:12-21`, `main.jsp:26` [C] |
| Logging | None: 3 `System.out` calls; `commons-logging` 1.2 comes in through Spring | — | No audit trail of logins or failures | **Low** | `PositionDaoImpl.java:69`, `LanguageDaoImpl.java:69`, `main.jsp:172` [C] |
| Testing | None | — | No regression detection | **Blocker** | No `src/test/` (G23) [C] |
| Packaging | WAR; `target/` was committed with a stale WAR (untracked in Phase 0, 2026-09-26) | — | Deploying the committed WAR ships old code | **Low** | G29 [C] |

## 2. Blockers
| # | Blocker | Evidence | Resolved in |
|---|---|---|---|
| B1 | No tests | G23 [C] | Phase 0 |
| B2 | No DB schema in the repo, so DAO tests are impossible | G22 [C] | Phase 0 (owner supplies the DDL) |
| B3 | No toolchain on the dev machine; no container or JNDI definition in the repo | [build-run.md](build-run.md) [C]; [open-questions.md](open-questions.md) #4 | Phase 0 (toolchain); ops (container) |
| B4 | The build fails on JDK 9+: no compiler level (G35), and war plugin 2.3 | `pom.xml:74-88` [C]; plugin failure [A] | Phase 0 |
| B5 | `WebMvcConfigurerAdapter` is used; it's deprecated in Spring 5 and removed in 6 | `WebConfig.java:14,21` [C]; removal [A] | Phase 1 (resolved on `dev` 2026-09-26) |
| B6 | `javax.*` Jakarta EE usage, in exactly 4 places (see below) | Import grep [C] | Phase 3 |
| B7 | Coupling to the app server: the JNDI name `java:comp/env/jdbc/springrms` is standard and portable. The resource definition, and probably the driver jar, live in the container | `WebConfig.java:31-37` [C]; driver location [A] | Phases 1 and 3 (ops) |
| B8 | Both Spring Dependabot branches change the **shared** `spring.version`. The 6.0.0 branch moves every Spring module to 6.0 and can't compile (Jakarta namespace, removed adapter, JDK 17). The 5.2.20 branch compiles, but 5.2 is EOL | `git diff master...origin/dependabot/*` → `pom.xml:13` [C]; EOL [A] | Close them in Phase 1 |
| B9 | MySQL-specific SQL: 3-argument `concat()`, comma joins, and `m.*` columns mapped by position (G10). Matters only for a DB change; tests pin it | `MarksDaoImpl.java:19-25,40-55` [C] | Phase 0 tests |

**B6 locations:**
- `LoginController.java:3-5`
- `pom.xml:52-64`
- the JSTL URI at `viewposition.jsp:3` and `viewlanguage.jsp:3`
- the `web.xml:1-3` DTD

`javax.naming` and `javax.sql` (`WebConfig.java:3-4`) are JDK packages, so they stay.

**Not found** [C]:
- Removed JDK APIs: Java imports are limited to `java.util`, `java.sql`, `javax.naming` and `javax.sql`, and JSP imports to `java.util.*` and `rms.model.*`.
- XML Spring configuration.
- Oracle features and stored procedures.
- Date or time columns: the row mappers use only `getInt`/`getString`.

## 3. Target stack
Move one axis at a time. Pass through Spring 5.3 so the security fixes ship before the container has to change.

| Area | Target | Why this | Why not the alternatives | Cost | Label |
|---|---|---|---|---|---|
| **JDK** | **21 LTS** (any OpenJDK build); optionally 25 after Phase 3 | It's the only LTS supported by both the 5.3 waypoint (JDK 8–21) and Spring 7 (17–25+), as well as Tomcat 11 (17+), so the migration needs one JDK move. Patched until 2029-12-31 | **17:** patches end 2027-10-31.<br>**25:** unsupported by 5.3, which would merge the JDK and framework steps; do 21→25 later as an S change (patched to 2031-09-30).<br>**8:** Spring 6/7 and Tomcat 10.1+ don't run on it | S | [A] dates and ranges; [C] no removed APIs |
| **Build** | Maven 3.9 with the Maven Wrapper; pinned plugins; `spring-framework-bom` | The team already uses Maven. The BOM keeps all Spring modules on one version, which is exactly what went wrong in B8 | **Gradle:** a rewrite of the build for no gain | S | [C] `pom.xml`, B8 |
| **Framework** | **Spring Framework 7.0.x** (plain Spring MVC, no Boot), via **5.3.39** | The only Spring line with open-source support. The code changes are 1 config class, 1 controller, the pom and 2 taglib lines | **Spring Boot 4:** its docs say JSPs should be avoided with embedded containers and work only with WAR packaging. All 7 views are scriptlet JSPs, there are only 2 config classes, and Boot 4 uses the same Framework 7, so there's no support gain. Revisit once the views are replaced.<br>**Stopping at 5.3 or 6.2:** open-source EOL | M | [A] support dates, Boot docs; [C] code footprint |
| **Container** | Tomcat 9.0 (Phases 0–2) → **Tomcat 11.0** (Phase 3) | Tomcat 9 runs `javax` code on JDK 8 and 21. Tomcat 11 provides Servlet 6.1, the Spring 7 baseline (Jakarta EE 11) | **Tomcat 10.1:** Servlet 6.0 is below Spring 7's baseline [A].<br>**Other servers:** the current one is unknown; Tomcat is the lowest-friction choice for JSP | M (ops) | [A] |
| **Persistence** | Keep `NamedParameterJdbcTemplate` and the row mappers | All the DAOs use it; the API is stable through 7.0 | **JPA, MyBatis or jOOQ:** each rewrites every DAO. `JdbcClient` (6.1+) is optional, for new code only | None | [C] `rms/dao/*` |
| **DB driver** | `com.mysql:mysql-connector-j`, in the newest series that supports the production server (the latest for 8.4+; 8.2.0 is the newest that supports 5.7, since 8.3.0+ need 8.0+ per the release notes) | Keeps MySQL and the existing SQL. Supports JRE 8+, so it can ship in Phase 1 | **5.1:** legacy, with known CVEs | S (code) + S–M (container) | [A] MySQL docs |
| **Logging** | SLF4J 2 + Logback (1.3.x on Java 8, 1.5.x on 21) | Spring 5.3+ routes its logging to SLF4J. Replaces the 3 `System.out` calls | **Log4j2:** heavier configuration.<br>**java.util.logging:** weak configuration.<br>No existing config to keep | S | [A] routing; [C] call sites |
| **Testing** | JUnit Jupiter 5.x, Mockito, AssertJ, spring-test `MockMvc` (standalone), **Testcontainers MySQL**, and a manual acceptance checklist | Standalone MockMvc needs no Spring test runner, so the same tests work from 4.3 to 7.0. A real MySQL matches the MySQL-specific SQL | **H2 in MySQL mode:** doesn't prove driver or `concat()` behaviour.<br>**JUnit 4:** a dead end.<br>JUnit 6 and Mockito 5 need newer Java, so switch in Phase 2 | M | [A] compatibility; [C] SQL |
| **Packaging** | WAR on external Tomcat 11 with the JNDI name kept; CI builds the WAR; `target/` no longer committed; optional container image | Keeps today's model. JSP and `src/main/webapp` need a WAR | **Executable JAR:** `src/main/webapp` is ignored in a JAR [A: Boot docs] | S–M | [C] `pom.xml:6`, G29 |
| **Security framework** | Not part of this plan | G11, G13 and G32 are functional fixes. Spring Security 7 is the follow-up after Phase 3 | — | — | [C] [gaps.md](gaps.md) |

## 4. Phases
**Rules:**
- The DB is never touched, so a rollback never needs a data restore.
- Each phase is released and signed off before the next starts.

### Phase 0 — Safety net (**M**; **L** if the schema has to be reverse-engineered)
- **Status (2026-09-26):**
  - **Done:**
    - Toolchain: Temurin JDK 8 and the Maven Wrapper (Maven 3.9.16).
    - `pom.xml` build fixes (G35), with no existing dependency version changes; the war plugin moved 2.3 → 3.4.0, and test-scope dependencies were added.
    - `.gitignore`, and `target/` untracked (G29).
    - `db/schema.sql`, **inferred** from the DAO SQL (owner decision: reverse-engineer first, verify later), plus synthetic `db/test-seed.sql`.
    - Characterization tests: controller, DAO and smoke.
    - `mvnw -B verify` passes on JDK 8: 16 run and pass, 20 skipped.
    - The new WAR contains the Position classes and has the same `WEB-INF/lib` as before.
  - **Open (owner, or a machine with Docker and Tomcat):**
    - Run the DAO tests with Docker.
    - Confirm or replace the inferred DDL (open question #19).
    - Deploy to local Tomcat 9, run `SmokeTest` and the acceptance checklist.
    - Screenshot the results page.
  - Phase 0 is complete when these pass.
  - Details: [build-run.md](build-run.md).
- **Scope:**
  1. **Toolchain:** JDK 8 and Maven (via the Maven Wrapper); Docker for Testcontainers; a local Tomcat 9 plus MySQL (dev only).
  2. **Build fixes in `pom.xml`, with no dependency version changes:**
     - compiler source/target 1.8 (G35)
     - `sourceEncoding` UTF-8
     - pinned compiler, war 3.4.x and surefire 3.x plugins
  3. **`.gitignore`:** stop tracking `target/` with `git rm -r --cached target` (G29).
  4. **Schema (G22):** `db/schema.sql`, DDL only from the owner, with no data and no secrets; plus a synthetic `db/test-seed.sql`.
  5. **Characterization tests:** these pin current behaviour, including known defects. Mark such tests, e.g. `// characterizes G14`.
     - **Controllers** (standalone MockMvc with mocked services):
       - Login success: view `main` and the session attribute `user`.
       - Login failure: view `login` and `errorMessage`.
       - `GET /login` invalidates the session.
       - Position and Language: save creates or updates depending on the key; list; edit; delete redirects.
       - `/adminviewmarks` model.
     - **DAOs** (Testcontainers MySQL):
       - login check and profile
       - add (including the silent duplicate skip, G14), update, delete, and the `isactive=1` filter
       - the `getAllMarksByAdmin` column mapping, as-is (G10)
     - **HTTP smoke test** against a deployed WAR, with the base URL taken from an environment variable:
       - `/login` returns 200
       - the admin and interviewer menus appear
       - the list pages render
  6. **Baseline:**
     - Build the unchanged app from source and run the acceptance checklist (section 6).
     - Screenshot the admin results page with the seed data.
     - Record the current security alerts.
- **Files:** `pom.xml`, `src/test/java/rms/**`, `db/*.sql`, `.gitignore`, `.mvn/wrapper/*`, `mvnw`/`mvnw.cmd`.
- **Risks:**
  - A reverse-engineered DDL may have wrong types or constraints.
  - Docker may not be available.
- **Rollback:** everything is additive, so revert the commit. The only production-visible change is the bytecode level: 1.5 → 1.8, on the same JDK (confirmed: the old `master` WAR has class version 49, the Phase 0 build 52) 8.
- **Verification:**
  - `mvnw -B verify` passes on JDK 8.
  - `jar tf` shows the Position classes, which the stale WAR lacks.
  - The acceptance checklist passes on local Tomcat 9.
  - `git status` shows no `target/`.

### Phase 1 — Dependency and security upgrades on Java 8 with `javax` (**M**)
- **Status (2026-09-26):**
  - **Done on `dev`:**
    - Spring BOM 5.3.39, with no per-module versions (`pom.xml:13,32-39`). The `spring.version` property stays; it now only sets the BOM version.
    - `WebConfig implements WebMvcConfigurer` (B5).
    - `com.mysql:mysql-connector-j` 8.2.0, excluding `protobuf-java` (only for the X DevAPI). 8.2.0 is the newest release that still supports MySQL 5.7; 8.3.0+ need 8.0+ (release notes). It was chosen because the server version is unknown (open question #16). 8.4.0 was tried first and replaced after the code review found that it doesn't support 5.7.
    - SLF4J 2.0.20 and Logback 1.3.16, with `logback.xml`; the two DAO `System.out` calls are now `log.warn`. CR/LF in log messages are replaced with `_` (checked with a scratch run).
    - Front end: jQuery 3.7.1, Bootstrap 3.4.1/4.6.2 (now from jsDelivr), DataTables 1.13.11 and Font Awesome 4.7.0, all with SRI hashes. The hashes were computed from the files the CDNs served on 2026-09-26; the vendor-published Bootstrap hashes match.
    - The DAO test image moved to `mysql:8.0`.
    - The three Dependabot branches were deleted (owner approved).
    - `mvnw -B verify` passes: 16 run and pass, 20 skipped. The WAR has one Spring version and no old driver.
  - **Left out by the owner:** `.github/dependabot.yml`.
  - **Open before release:**
    - The Phase 0 owner steps (DAO tests with Docker, smoke test, checklist).
    - Container: `driverClassName` → `com.mysql.cj.jdbc.Driver`, no old MySQL or logging jars in `lib/`, and review the SSL settings ([build-run.md](build-run.md)).
    - Code review (2026-09-26): nothing Critical or High; the Med findings were all about the container and the docs, and are addressed in build-run.md.
    - A browser check of every page after the front-end bumps.
    - A non-ASCII round-trip test (G34).
    - Confirm which GitHub alerts close once the change reaches `master`. Some Spring advisories are likely fixed only in commercial 5.3.x releases after 5.3.39 [A], so a few may stay open until Phases 2–3.
- **Scope:**
  1. **Spring:** import `spring-framework-bom` **5.3.39** in `dependencyManagement`, and remove the per-module versions. (`spring.version` is kept, but only as the BOM version.)
  2. **`WebConfig`:** `extends WebMvcConfigurerAdapter` → `implements WebMvcConfigurer` (`WebConfig.java:14,21`).
  3. **Driver:** `com.mysql:mysql-connector-j` in the series that matches the server.
     - Update it in the pom **and** in the container's `lib/` if the JNDI pool loads it from there.
     - The JNDI `driverClassName` becomes `com.mysql.cj.jdbc.Driver`.
     - Review the SSL, time-zone and character-set URL properties (ops; nothing goes into the repo).
  4. **Logging:**
     - Add `slf4j-api` 2.0.x and `logback-classic` 1.3.x, plus `src/main/resources/logback.xml`.
     - Replace `System.out` at `PositionDaoImpl.java:69` and `LanguageDaoImpl.java:69`.
     - `main.jsp:172` is left for G30.
  5. **Front end, drop-in versions within the same major:**
     - jQuery → 3.7.x
     - Bootstrap 3.3.7 → 3.4.1 and 4.1.1 → 4.6.x
     - DataTables → 1.13.x
     - Add Subresource Integrity (`integrity`) hashes.
     - G31 (the mixed Bootstrap versions) stays.
  6. **Dependabot:**
     - Close the three stale Dependabot branches (owner's approval).
     - Add `.github/dependabot.yml`, grouping all `org.springframework*` updates.
- **Files:**
  - Java: `pom.xml`, `WebConfig.java`, `PositionDaoImpl.java`, `LanguageDaoImpl.java`.
  - Front end: the 7 JSP head sections.
  - New: `logback.xml`, `.github/dependabot.yml`.
  - Outside the repo: the container's JNDI configuration.
- **Risks:**
  - **Spring 4→5:** suffix-pattern URL matching is off by default [A]. RMS URLs have no extensions [C].
  - **Driver:** SSL, time-zone and character-set defaults change [A]. There are no date columns [C]. Non-ASCII text needs testing (G34).
  - **Front-end bumps:** the visual appearance may shift.
- **Rollback:**
  - Redeploy the Phase 0 WAR.
  - Keep the old driver jar and `driverClassName` until sign-off.
- **Verification:**
  - The Phase 0 tests pass unchanged.
  - `mvn dependency:tree` shows one Spring version and no `mysql:mysql-connector-java`.
  - The Spring and driver alerts are closed.
  - The acceptance checklist results match the Phase 0 screenshots.

### Phase 2 — JDK 8 → 21 (**S**)
- **Gate:** Phase 1 must be **in production first** (see section 5).
- **Scope:**
  - Set `maven.compiler.release=21`.
  - Run Tomcat 9 on JDK 21.
  - Test libraries: Mockito 5, Logback 1.5; JUnit can move to 6.
  - Move CI and dev to JDK 21.
  - Regenerate or delete the stale Eclipse `.settings` (Java 1.5).
- **Files:** `pom.xml`, `.settings/*`, and the container's JDK (ops).
- **Risks:** low.
  - Spring 5.3 supports JDK 8–21 [A].
  - Tomcat 9 compiles JSPs on 21 [A].
  - No JDK-internal APIs are used [C].
- **Rollback:** point Tomcat back to JDK 8 and redeploy the kept Phase 1 WAR. Bytecode built for 21 won't run on 8.
- **Verification:**
  - Build and tests pass on 21.
  - The smoke test and acceptance checklist pass.
  - The logs show no illegal-reflective-access or JSP compile warnings.

### Phase 3 — Spring 7.0, Jakarta namespace and Tomcat 11 (**M**)
- **Deadline:** in production before **2027-03-31** (see section 5).
- **Scope:**
  1. **`pom.xml`:**
     - `spring-framework-bom` 7.0.x
     - `jakarta.servlet-api` 6.1 as `provided`
     - Jakarta Tags 3.0 API and implementation (Tomcat doesn't bundle JSTL [A])
     - remove every `javax.servlet` artifact
  2. **`LoginController.java:3-5`:** `javax.servlet.http.*` → `jakarta.servlet.http.*`. It's the only Java file affected [C].
  3. **JSTL URIs:** `http://java.sun.com/jsp/jstl/core` → `jakarta.tags.core` in `viewposition.jsp:3` and `viewlanguage.jsp:3`.
  4. **`web.xml`:** replace the 2.3 DTD with the Servlet 6.1 schema, or delete the file (`failOnMissingWebXml=false`, `pom.xml:83`).
     - EL becomes enabled by default.
     - That's safe: the 6 EL-using JSPs already opt in with `isELIgnored="false"`, and `viewmarks.jsp` contains no `${` [C].
     - It closes G37.
  5. **Container:** Tomcat 11 on JDK 21, with the JNDI `jdbc/springrms` resource re-created (ops; no secrets in the repo).
- **Files:** `pom.xml`, `LoginController.java`, `viewposition.jsp`, `viewlanguage.jsp`, `web.xml`, and the container (ops).
- **Risks:**
  - Spring 6+ stops matching trailing slashes by default [A]. Links come from `spring:url` without trailing slashes (`viewposition.jsp:49,53`) [C], but bookmarks may break.
  - Spring 7 removals affecting `JstlView` or `JndiTemplate` would show up at compile time [A].
  - Tomcat 11 session persistence warns on the non-`Serializable` `UserInfo` (G36) [A].
- **Rollback:** run Tomcat 9/JDK 21 with the Phase 2 WAR side by side until sign-off, and switch back by redeploying.
- **Verification:**
  - `grep -rn "javax.servlet" src pom.xml` returns nothing.
  - Build and tests pass.
  - The acceptance checklist passes on Tomcat 11.

### Phase 4 — Packaging and deployment (**M**)
- **Scope:**
  1. **CI** (GitHub Actions):
     - runs `mvnw verify` with Testcontainers on PRs to `dev` and `master`
     - runs a dependency scan
     - publishes the WAR as a build artifact
  2. **Deploy templates:**
     - A Tomcat context template for `jdbc/springrms`, with its values read from environment variables. No credentials or hosts go in the repo [A: mechanism].
     - An optional container image (Tomcat 11 + JDK 21) that deploys the WAR as `ROOT`, which sidesteps the context-path redirect bug in G30.
- **Follow-ups, outside this plan:**
  - JDK 21 → 25 (S)
  - Spring Security 7 for G11, G13 and G32
  - replacing the JSPs, then re-evaluating Spring Boot
- **Files:** `.github/workflows/*.yml`, a `deploy/` template, an optional `Dockerfile`.
- **Rollback:** the manual deployment path stays valid.
- **Verification:**
  - CI passes on a PR.
  - The image boots locally and the smoke test passes.

## 5. Gates and deadlines
- **JDK after Spring:**
  - Never run RMS on JDK 9+ while it's on Spring 4.3. Spring4Shell (CVE-2022-22965) applies to Spring MVC WARs on Tomcat with POJO binding [A].
  - RMS binds `PositionInfo` and `LanguageInfo` (`PositionController.java:35`, `LanguageController.java:44`) [C].
  - Phase 2 therefore waits for Phase 1 to reach production.
- **5.3 is only a waypoint:** its open-source support ended 2024-08-31 [A], so Phases 1–3 run back to back.
- **Tomcat 9 support ends 2027-03-31** [A]. Phase 3 must be in production before then.
- **Spring 7.0 open-source support ends 2027-07-31** [A]. After Phase 3, keep to the routine 7.x minor updates.

## 6. Risk and compatibility

### Business flows at risk per phase
| Phase | [Login and menu](business-flows/login.md) | [Position/Language](business-flows/masters.md) | [Admin results](business-flows/evaluation.md) | Why |
|---|---|---|---|---|
| 0 | — | — | — | Tests only; bytecode level 1.8 on the same JDK |
| 1 | **Med** | **Med** | **High** | The driver swap touches every query. Results depend on `concat()` and column positions (G10). The list pages use the jQuery and DataTables bumps |
| 2 | Low | Low | Low | Runtime JDK change only |
| 3 | **High** | Med | Med | Login is the only `javax.servlet` user; the list pages have the JSTL URI; the menu shell loads through EL and `<object>` |
| 4 | Med | Med | Med | DB connectivity comes from a new config template |

### MySQL compatibility
- **Server version:** the unknown server version is the biggest DB risk. The newest driver series supports 8.4+ only [A].
- **Column positions:** results are mapped by column position over `m.*` (`MarksDaoImpl.java:40-55`). No driver changes that, but it's fragile, so Phase 0 tests pin it.
- **MySQL syntax:** the 3-argument `concat()` and comma joins (`MarksDaoImpl.java:19-25`) work with any MySQL driver.
- **Active flags:** `isactive` is read with `getInt` (`LoginDaoImpl.java:33`, `MarksDaoImpl.java:44`), which is safe for both TINYINT(1) and BIT [A].
- **Character sets:** Connector/J 8+ negotiates character sets differently [A]. With ISO-8859-1 JSPs and no encoding filter (G34), non-ASCII names need a round-trip test.
- **No exposure:** there are no date columns [C], no stored procedures and no schema changes.

### Acceptance checklist for domain users (after Phases 1, 2 and 3)
1. **Login:** log in as an admin and as an interviewer, and check the menu, name, role and email. A wrong password shows "Invalid login!". Logout works.
2. **Position:**
   - create one
   - create a duplicate: today it's silently skipped
   - edit it
   - delete it
   - check the list's search and sort
3. **Language:** the same steps as Position.
4. **Admin view marks:** check each candidate's names, position, language, 10 scores, total and Selected/Rejected label against the Phase 0 screenshots.
5. **Non-Latin characters:** enter a name with non-Latin characters (if the business uses them) and check it's shown correctly.
6. **Unfinished menu items:** they behave exactly as before.

## 7. Support dates
Checked 2026-09-25. Sources: endoflife.date, the Spring Framework Versions wiki, the MySQL Connector/J documentation and the Spring Boot reference. Re-check each date before relying on it [A].

| Product | Line | Open-source / security support ends |
|---|---|---|
| Spring Framework | 4.3 | 2020-12-31 |
| Spring Framework | 5.3 | 2024-08-31 (commercial support to 2029-06-30) |
| Spring Framework | 6.2 | 2026-06-30 |
| Spring Framework | 7.0 | 2027-07-31 (JDK 17–25+, Servlet 6.1) |
| Tomcat | 9.0 | 2027-03-31 |
| Tomcat | 10.1, 11.0 | Active (11.0 needs Java 17) |
| Temurin JDK | 8 | 2030-12-31 |
| Temurin JDK | 17 | 2027-10-31 |
| Temurin JDK | 21 | 2029-12-31 |
| Temurin JDK | 25 | 2031-09-30 |
| MySQL server | 5.7, 8.0 | Ended |
| MySQL server | 8.4 LTS | Premier support to 2029-04 |
| MySQL server | 9.7 LTS | Premier support to 2034-04 |

## 8. Top risks
1. **No safety net** (G22, G23). Every phase depends on Phase 0.
2. **Unknown runtime:** the container, JDK, MySQL version and JNDI setup are all outside the repo, and the container may keep loading the old driver jar.
3. **Wrong order:** upgrading the JDK before Spring exposes Spring4Shell (section 5).
4. **Support deadlines:** the 5.3 waypoint is unsupported, and Tomcat 9 support ends 2027-03-31.
5. **Silent regressions:** column-position mapping (G10), scriptlet JSPs and the front-end bumps can fail in ways only users would notice. Run the acceptance checklist after every phase.

## 9. Before Phase 0 starts
**System facts:** tracked in [open-questions.md](open-questions.md):
- #4: the container and JNDI setup
- #7: the Dependabot branches
- #16: the MySQL server version
- #17: runtime ownership and usage
- #18: Docker availability

The schema (G22) is also needed.

**Decisions made on 2026-09-26 (Phase 0 start):**
- The "no builds" rule is lifted for phase work; "no DB access" stays. Tests use a throwaway MySQL container only.
- Toolchain: Temurin JDK 8 via winget, plus the Maven Wrapper.
- Schema: reverse-engineer it from the DAO SQL now (`db/schema.sql`) and verify it later against a DDL-only export (open question #19).
- DAO tests: Testcontainers, skipped where Docker is missing, unless `RMS_REQUIRE_DOCKER=true`.
- `target/`: untrack it.

**Decisions made on 2026-09-26 (Phase 1 start):**
- Start Phase 1 before Phase 0 is signed off, but don't release it until the DAO tests and checklist pass.
- The server version is unknown, so use Connector/J 8.2.0 (the last release supporting 5.7; corrected from 8.4.0 after the review) and test on `mysql:8.0`.
- Include the front-end bumps with SRI and SLF4J/Logback; leave out `dependabot.yml`.
- Delete the three Dependabot branches (done).

**Owner decisions still open:**
- Choose the JDK standard: 21, or an organisational 17 or 25.
- Choose the deployment target after Phase 4, and whether GitHub Actions is acceptable for CI.
- Confirm there's no Oracle database and no plan to move to one.
