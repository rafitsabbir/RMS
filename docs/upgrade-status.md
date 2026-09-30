# Upgrade Status

Purpose: The live tracker for the tech upgrade: what is done, in progress and pending, what to do next, and how to resume on another machine. Details stay in the linked owning docs.
Last updated: 2026-09-30 (G11 interim login check on `dev`)
Read this when: you're resuming upgrade work, or asking "where are we / what's next".

## Snapshot (2026-09-27)
- **Branch:** all upgrade work is on `dev`, and nothing is merged to `master`. `master` still ships the old stack: Spring 4.3.0 and Connector/J 5.1.36 ([gaps.md](gaps.md) G24).
- **Alerts:** GitHub reported 26 alerts on `master` before Phase 1: 2 critical, 9 high, 12 moderate and 3 low (an external figure; [open-questions.md](open-questions.md) #7).
- **Last build:** on 2026-09-27, `RMS_REQUIRE_DOCKER=true ./mvnw -B verify` gave BUILD SUCCESS on JDK 8u504 and on JDK 21. 34 of 36 tests passed, including all 18 DAO tests. The 2 smoke tests then passed separately against Tomcat 9.0.122 ([build-run.md](build-run.md)).
- **Current phase:** Phase 1 is coded, and its acceptance checklist matches the Phase 0 baseline ([acceptance/README.md](acceptance/README.md)). It's still **not releasable**: the owner must confirm the DDL, and ops must make the container changes (below).

## Status by work item
States:
- **Done:** committed on `dev` and verified by build or tests.
- **Open:** still to do in this phase.
- **Pending:** a later phase, not started.
- **Deferred:** left out by the owner.

Owners:
- **dev:** anyone with the repo.
- **ops:** container or server access.
- **owner:** needs a business or system decision.

### Phase 0: safety net ([plan §4](modernization-plan.md#phase-0--safety-net-m-l-if-the-schema-has-to-be-reverse-engineered))
| Item | State | Owner | Evidence |
|---|---|---|---|
| Temurin JDK 8 plus the Maven Wrapper (Maven 3.9.16) | Done | dev | `mvnw`, `.mvn/wrapper/maven-wrapper.properties` |
| `pom.xml` build fixes: compiler 1.8, UTF-8, pinned plugins (G35) | Done | dev | `pom.xml` |
| `target/` untracked, `.gitignore` (G29) | Done | dev | `.gitignore` |
| Inferred `db/schema.sql` and synthetic `db/test-seed.sql` | Done | dev | `db/` |
| Characterization tests: controller 16, DAO 18, smoke 2 (3 since G11, plus 6 access-control tests) | Done | dev | `src/test/java/rms/` |
| Local DB setup scripts (`rms_local`: script or Docker Compose) | Done. `setup-local-db.sh` and `docker-compose.yml` executed 2026-09-27 ([build-run.md](build-run.md)); `.ps1` **not yet executed** | dev | `db/local/README.md` |
| Run the DAO tests with Docker (`RMS_REQUIRE_DOCKER=true`) | Done: 18/18 on 2026-09-27 (JDK 8u504 and 21, Docker Engine 29.3.1) | dev | [build-run.md](build-run.md); #18 |
| Confirm or replace the inferred DDL with a DDL-only export | **Open** | owner | [open-questions.md](open-questions.md) #19, G22 |
| Deploy to local Tomcat 9; run `SmokeTest` and the acceptance checklist | Done 2026-09-27: `SmokeTest` 2/2 and the checklist in Chromium on Tomcat 9.0.122. Everything passes except item 5 (G34), same as Phase 0 ([acceptance/README.md](acceptance/README.md)) | dev | [plan §6](modernization-plan.md#acceptance-checklist-for-domain-users-after-phases-1-2-and-3) |
| Baseline screenshot of the admin results page with the seed data | Done 2026-09-27 (`acceptance/phase0/`) | dev | [plan §4](modernization-plan.md) Phase 0 scope 6 |

### Phase 1: dependency and security upgrades on Java 8 ([plan §4](modernization-plan.md#phase-1--dependency-and-security-upgrades-on-java-8-with-javax-m))
| Item | State | Owner | Evidence |
|---|---|---|---|
| Spring BOM 5.3.39, no per-module versions | Done | dev | `pom.xml:13,32-39` |
| `WebConfig implements WebMvcConfigurer` (B5) | Done | dev | `WebConfig.java:21` |
| `com.mysql:mysql-connector-j` 8.2.0 (supports MySQL 5.7+), `protobuf-java` excluded | Done | dev | `pom.xml:93-104`; [plan §6](modernization-plan.md#mysql-compatibility) |
| SLF4J 2.0.20 and Logback 1.3.16; DAO `System.out` → `log.warn` | Done | dev | `logback.xml`, `PositionDaoImpl.java:73` |
| CDN bumps with SRI: jQuery 3.7.1, Bootstrap 3.4.1/4.6.2, DataTables 1.13.11, Font Awesome 4.7.0 | Done | dev | the 7 JSP heads; [tech-stack.md](tech-stack.md) |
| Three stale Dependabot branches deleted | Done | owner | #7 |
| The Phase 0 open items above | **Open** | dev / owner | Phase 0 table |
| Container: `driverClassName` → `com.mysql.cj.jdbc.Driver`; no old MySQL or logging jars in `lib/`; review SSL | **Open** | ops | [build-run.md](build-run.md) |
| Browser check of every page after the front-end bumps | Done 2026-09-27: pixel-identical to Phase 0, no SRI errors; the pre-existing G31 login-page error is unchanged | dev | [acceptance/README.md](acceptance/README.md), G31 |
| Non-ASCII round-trip test | Done 2026-09-27: Latin-1 round-trips; other scripts are stored as HTML entities, the same as Phase 0 (not a Phase 1 regression) | dev | G34 |
| Merge `dev` → `master`, then confirm which alerts close (some Spring ones may stay open [A]) | **Open** | owner | G24 |
| `.github/dependabot.yml` | Deferred | owner | [plan §9](modernization-plan.md#9-before-phase-0-starts) |

### Phases 2–4 and follow-ups
| Item | State | Gate / deadline | Evidence |
|---|---|---|---|
| **Phase 2:** JDK 8 → 21, Mockito 5, Logback 1.5, regenerate `.settings` | Pending | Phase 1 must be **in production** first (Spring4Shell) | [plan §4–5](modernization-plan.md#phase-2--jdk-8--21-s) |
| **Phase 3:** Spring 7.0, Jakarta namespace, Tomcat 11, `web.xml` (G37) | Pending | In production before **2027-03-31** (end of Tomcat 9 support) | [plan §4](modernization-plan.md#phase-3--spring-70-jakarta-namespace-and-tomcat-11-m) |
| **Phase 4:** GitHub Actions CI, JNDI context template, optional image | Pending | After Phase 3 | [plan §4](modernization-plan.md#phase-4--packaging-and-deployment-m) |
| **Follow-ups:** JDK 25; Spring Security 7 (G11 has an interim interceptor since 2026-09-30; G13, G32); replacing the JSPs | Pending | After Phase 4 | [gaps.md](gaps.md) |

## Owner decisions still open
- The MySQL server version in production (#16). It decides whether the driver can move past 8.2.0.
- The container, the JNDI setup and runtime ownership (#4, #17).
- The JDK standard: 21, or an organisational 17 or 25.
- The deployment target after Phase 4, and whether GitHub Actions is acceptable for CI.
- Confirm there is no Oracle database and no plan to move to one.

## Next actions, in order
1. ~~Run the DAO tests with Docker~~ done 2026-09-27 (18/18).
2. ~~Deploy to a local Tomcat 9, run `SmokeTest`, the acceptance checklist and the baseline screenshot~~ done 2026-09-27 ([acceptance/README.md](acceptance/README.md)). Optional: have a domain user repeat the checklist on a desktop browser with real CDN access.
3. ~~Do the browser check of every page and the non-ASCII test (G34)~~ done 2026-09-27.
4. Hand the container changes to ops (driver class name, `lib/`, SSL), then merge to `master` and release Phase 1.
5. Once Phase 1 is in production, start Phase 2.

## Resume on another machine
1. Clone the repo, then `git checkout dev`.
2. Install a JDK 8 and set `JAVA_HOME` to it. On Windows: `winget install EclipseAdoptium.Temurin.8.JDK`. A JRE is not enough, because `javac` is needed.
3. Run `./mvnw -B verify` (or `mvnw.cmd -B verify`). The first run downloads Maven 3.9.16 into `~/.m2/wrapper/dists/`.
4. Optional:
   - Docker, for the DAO tests.
   - Tomcat 9 with `RMS_BASE_URL`, `RMS_SMOKE_USER` and `RMS_SMOKE_PASSWORD` (seed test values only), for the smoke test.
5. Rules that still apply ([CLAUDE.md](../CLAUDE.md)):
   - Never connect to or change a real database.
   - Never copy credentials, JNDI definitions or hostnames into the repo or chat.
   - Work on `dev`, and commit only when asked.
   - Keep the docs evidence-based.
6. With Claude Code, start at `CLAUDE.md` → [ROUTER.md](ROUTER.md). No local Claude memory from the first machine is needed; all state is in the repo.
