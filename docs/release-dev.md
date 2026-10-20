# Release Runbook: `dev` (Spring 7, Tomcat 11, MySQL 9.7 LTS)

Purpose: How to release the current `dev` to production: what ships, what users notice, the owner checks, the ops steps in order (backup, MySQL server upgrade, migrations, application), the checks after deploy, and the rollback. It includes the rehearsal that proved those steps.
Last updated: 2026-10-20 (migration 008: hiring stages). 2026-10-20 (the database checks in one read-only script, `db/checks/`). 2026-10-20 (health check after deploy, DB timeout options; written and rehearsed from a MySQL 5.7 + Tomcat 9 starting point)
Read this when: you're preparing, doing or signing off the release of `dev`, or rolling it back.

No credentials, hostnames or JNDI definitions belong in this file. Each environment's values stay in its container or secret store.

This replaces nothing: [release-phase1.md](release-phase1.md) still describes the smaller Phase 1 release from `release/phase1`, which keeps Java 8, Tomcat 9 and MySQL 5.7. Release one or the other. If Phase 1 was released first, this runbook still applies; its starting point is then Connector/J 8.2.0 instead of 5.1.36, which changes nothing below.

## What ships
Compared with the 2020 code, `dev` adds (details: [upgrade-status.md](upgrade-status.md), [gaps.md](gaps.md)):
- **Stack:** Java 21, Spring 7.0.9 on Jakarta EE 11 and **Tomcat 11** (Servlet 6.1), Spring Security 7, Connector/J 9.7.0, Logback 1.5. The WAR no longer runs on Tomcat 9 or Java 8.
- **Database:** **MySQL 9.7 LTS** (owner decision 2026-10-19). Connector/J 9.7 needs MySQL 8.0 or later, so a 5.7 server must be upgraded first ([mysql-upgrade.md](mysql-upgrade.md)).
- **Schema changes:** migrations `001`, `002`, `004`, `005`, `006`, `007` and `008` in `db/migrations/` (new columns and seven new tables). `003` (hashing the stored passwords) is separate and optional (below).
- **Features (since the rehearsal, on `dev` 2026-10-20):** hiring stages (migration 008), interview calendar files, a possible-duplicate warning, filters on the Candidates list, and a hiring report. Migration 008 was rehearsed on its own on MySQL 9.7 (run, rollback, re-run), not in the full 5.7 → 9.7 sequence below.
- **Features:** four roles (Super Admin, HR, Hiring Manager, Interviewer) with Users and Roles; candidates with documents, jobs, interviewer assignment, evaluations on 10 criteria, decisions with reasons and history; the interview schedule and dashboard; CSV reports; the activity log; Deleted Candidates.
- **UI:** a new look on Bootstrap 5 with a sidebar menu; every page is a real page (back, refresh and bookmarks work).
- **Security:** form login with CSRF tokens and POST logout, deny-by-default access per role, a login lock after 5 failures, `{bcrypt}` for every new or changed password, inactive users signed out, security headers including a Content-Security-Policy.
- **Optional:** a container image (`Dockerfile`; [build-run.md](build-run.md), "Container") instead of a hand-built Tomcat.

## What users will notice
- A new look and menu. Interviewers see My Evaluations and My Schedule.
- The admin of today becomes a **Super Admin**, interviewers become **Interviewers** (migration 002). Super Admins must give HR and Hiring Manager roles under Users and Roles; nobody has them at first.
- Existing passwords keep working (plain-text rows are still checked exactly, open question #25). A password set or reset from now on is stored hashed.
- Five wrong passwords lock that username from that address for 15 minutes.
- Only English text is accepted in names, reasons, comments and locations.
- Opening a page that a role may not use gives RMS's own error page (HTTP 403); other errors show the same page, with no technical detail.
- Pages and libraries load from `cdn.jsdelivr.net` only; a proxy that lists allowed sites must allow it.

## Before the release (owner)
1. **Decisions:** Tomcat 11 on the server, or the container image (#4, #17); the JDK standard (21); the release window.
2. **Database checks** on production, read-only, in one run: [`db/checks/pre-release-checks.sql`](../db/checks/README.md) (send its output to the developers). It covers the DDL against `db/schema.sql` (#19), and the checks of #27 (who becomes Super Admin, password column, keys, storage engine), #28 (documents and jobs), **#29 (`SHOW CREATE TABLE marks`; migration 005 is a draft until this is done)** and #30 (time zone). A difference found here stops the release until the migration is adjusted.
3. **Migration 003** (hash the stored plain-text passwords): approve it now, later, or not (#25, #27). The release works without it.
4. **Document storage:** a folder for `RMS_DOC_DIR` with its owner, rights, backup and size ([build-run.md](build-run.md), "Document storage").
5. **Accounts:** the MySQL account the application uses (data rights on the RMS database only: SELECT, INSERT, UPDATE, DELETE) and an admin account for the server upgrade and the migrations (schema rights).
6. **Sign-off:** a domain user for the checks after deploy.

## Ops steps
These were rehearsed in this order (below). Keep everything you replace: it is the rollback. Users are off the system from step 1 to step 7.
1. **Stop the application** (Tomcat, or the old container). Record the Tomcat and JDK versions, the `lib/` listing and the context file.
2. **Back up:**
   - `mysqldump --single-transaction --routines --triggers --events --all-databases` from the 5.7 server, kept off the server. `--all-databases` includes the `mysql` schema, so the accounts come back with a restore.
   - Also a file-level copy of the data directory if the server is stopped anyway, and the old WAR, context file and `lib/` jars.
   - Check the dump is complete (it ends with `-- Dump completed`).
3. **Upgrade the MySQL server to 9.7 LTS**, one series at a time, exactly as [mysql-upgrade.md](mysql-upgrade.md) describes: a slow shutdown (`SET GLOBAL innodb_fast_shutdown = 0`) before each step; 5.7 → 8.0; **on 8.0, move every account off `mysql_native_password`** (`ALTER USER ... IDENTIFIED WITH caching_sha2_password BY ...`, the application's account included, with its existing password); 8.0 → 8.4 → 9.7. After 9.7: `SELECT VERSION();`, `CHECK TABLE` on the RMS tables, and no account other than the internal `mysql.*` ones on `mysql_native_password`. Remove server settings that 9.x no longer knows (for example `innodb_log_file_size`) from the server's configuration, or it won't start.
4. **Run the migrations** with the admin account, in this order, against the RMS database: `001-candidate-isactive.sql`, `002-user-roles.sql`, `004-candidate-documents-jobs.sql`, `005-assignment-evaluation-decision.sql`, `006-interview-schedule.sql`, `007-activity-log.sql`, `008-candidate-stage.sql`. Each must finish without errors; run the check queries at the end of each file. (`003` only if approved: [db/migrations/003-hash-passwords/README.md](../db/migrations/003-hash-passwords/README.md).) The application account needs no new grant when it has its rights on the whole RMS database.
5. **Create the document folder** for `RMS_DOC_DIR`, writable by the application only.
6. **Install the application**, one of:
   - **Tomcat 11 on JDK 21:** remove old MySQL driver jars (`mysql-connector-java-5.1.*`, `mysql-connector-j-8.*`) from Tomcat's `lib/` and put `mysql-connector-j-9.7.0.jar` there (check it against its published checksum); use `deploy/tomcat/rms.xml` as the context file (`conf/Catalina/localhost/rms.xml`) with `RMS_DB_URL` (with `sslMode=REQUIRED&connectTimeout=5000&socketTimeout=60000`, as the file's comment says; `socketTimeout` must exceed the slowest query), `RMS_DB_USER`, `RMS_DB_PASSWORD` and `RMS_DOC_DIR` in Tomcat's environment and the property source set as the file says; deploy the WAR built from `dev` (`./mvnw -B verify`) at `/rms`.
   - **Container:** build the image from `dev` ([build-run.md](build-run.md), "Container") and run it with the same four variables from the orchestrator's secret store and the document folder mounted at `/var/rms/docs` (UID 10001).
7. **Start** and check the log: no SEVERE or ERROR lines.

## After deploy
1. **Health:** `GET /rms/health` returns `ok` (HTTP 200). `unavailable` (503) means the application can't reach the database; check the URL options, the account and the log ([build-run.md](build-run.md), *Container*). The container's HEALTHCHECK uses the same URL.
2. **Smoke test** from a dev machine, against the release: `RMS_BASE_URL=<the release URL> RMS_SMOKE_SEEDED=false RMS_SMOKE_USER=<a Super Admin> RMS_SMOKE_PASSWORD=<…> ./mvnw -B test -Dtest=SmokeTest`. Keep the credentials in environment variables only. `RMS_SMOKE_SEEDED=false` leaves out the checks that need the synthetic test data. It changes no business data: it signs in and out, reads pages, downloads the candidates CSV and sends posts that are refused (no CSRF token, a wrong password). Those sign-ins, failed sign-ins and the download appear in the activity log, made by the smoke account.
3. **By hand** (about 15 minutes):
   - A Super Admin signs in: the menu has Recruitment, Master data, Administration and Account; Candidate Status, Candidates, Positions and Languages show the existing data.
   - An interviewer signs in: Home, My Evaluations, My Schedule and Change password only; opening `/viewpositionlist` gives 403.
   - The Super Admin gives an HR and a Hiring Manager role under Users and Roles; each signs in and sees their menu.
   - Upload a small PDF on a candidate's profile and download it again (checks `RMS_DOC_DIR`).
4. **Acceptance:** the domain user runs the role checks in [acceptance/](acceptance/README.md).
5. **Update** [upgrade-status.md](upgrade-status.md): `dev` is in production.

## Rollback
- **Before step 3** (server not upgraded yet): start the old application again. Nothing changed.
- **After step 3, before or after the migrations:** the old application **can't** use the 9.7 server (its old driver fails with "Could not create connection"; rehearsed), and MySQL can't be downgraded in place. Stop the new application, keep the 9.7 server's data directory aside for analysis, start a **5.7 server on an empty data directory, load the step 2 dump**, then put back the old WAR, context file and `lib/` jars and start the old application. Everything written after step 1 is lost, so decide early.
- **Only the application** (server and migrations kept, the new release misbehaves): deploying the previous `dev` build is the only quick path; the 2020 application can't run on 9.7.
- The migration files' own rollback lines undo the schema on 9.7 (rehearsed on 2026-10-19), but they don't make the old application usable there; they are for repeating a migration step.

## Rehearsal (2026-10-20)
A scratch environment with throwaway containers and the synthetic test data only. Nothing real was touched.
- **Starting point, like production is assumed to be:** MySQL 5.7.44 with the schema and seed from before the migrations (`git show 870e473:db/schema.sql`, `a3b92b0^:db/test-seed.sql`) and an application account created on 5.7 (`mysql_native_password`, data rights only); the 2020 application (`phase0.war`, built from `d813e71`) on Tomcat 9 / JDK 8 with Connector/J 5.1.36 in `lib/`. It signed in and listed the positions.
- **Steps 1–2:** application stopped; the `--all-databases` dump completed; slow shutdown.
- **Step 3:** 5.7.44 → 8.0.46 (the server upgraded its data dictionary itself) → the three accounts moved to `caching_sha2_password` → 8.4.11 → 9.7.2. `CHECK TABLE` OK on all six old tables.
- **Step 4:** the six migrations ran with no errors or warnings; U1 became SUPER_ADMIN, U2 INTERVIEWER; 12 tables.
- **Step 6 (container):** the image built from `dev` with the application account over TLS: healthy; `SmokeTest` with `RMS_SMOKE_SEEDED=false`: 17 of 17, none skipped (with the seed checks on, 3 failed only because the migrated data isn't the current test seed, which is why the switch was added); no SEVERE or ERROR lines; the interviewer signed in with their old password and got exactly their pages (403 on master data).
- **Rollback:** the old application against 9.7 failed ("Could not create connection"). A fresh 5.7.44 loaded from the dump restored the accounts (`mysql_native_password` again) and the six tables, and the old application signed in and listed the data again.
- **Not covered:** the real server, its configuration file, data volume and TLS setup; the standalone Tomcat 11 variant in this sequence (it uses the same context file and driver as the container, and passed `SmokeTest` on 2026-10-19 against a fresh database); migration 003; real CDN access.
