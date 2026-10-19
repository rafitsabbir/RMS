# MySQL Server Upgrade to 9.7 LTS

Purpose: The rehearsed procedure and blocker for moving the production MySQL server to 9.7 LTS (owner decision 2026-10-19). Part of the Phase 3 release; it runs before the WAR and the migrations.
Last updated: 2026-10-19
Read this when: you're planning or running the production database server upgrade, or the Phase 3 release runbook ([upgrade-status.md](upgrade-status.md) next actions item 6).

## Decision and scope
- **Owner decision 2026-10-19:** production moves to the MySQL 9.7 LTS line (9.7 is the current LTS, released April 2026). After 9.7, MySQL uses calendar versions such as 26.7.0, which are Innovation releases, not LTS. Open question #16 ([open-questions.md](open-questions.md)).
- The current production version is still not stated. The path below assumes 5.7.44 (the Phase 0 baseline); start from the real version.
- The `dev` WAR bundles Connector/J 9.7.0, which needs MySQL 8.0 or later (`pom.xml`, `mysql.version`). `release/phase1` (Connector/J 8.2.0) still supports 5.7 and is unchanged ([release-phase1.md](release-phase1.md)).

## Upgrade path (rehearsed 2026-10-19)
Rehearsed on throwaway containers with the pre-migration schema and synthetic seed. MySQL upgrades in place only one series at a time.

| Hop | From | To |
|---|---|---|
| 1 | 5.7.44 | 8.0.46 |
| 2 | 8.0.46 | 8.4.11 (LTS) |
| 3 | 8.4.11 | 9.7.2 (LTS to LTS) |

## Blocker: mysql_native_password
- Every 5.7 account uses `mysql_native_password`. MySQL 8.4 disables it by default and 9.x removed it ("Plugin 'mysql_native_password' is not loaded"). After the 8.4 step no account (root or the app account) could log in.
- **Fix (rehearsed):** while still on 8.0, run `ALTER USER ... IDENTIFIED WITH caching_sha2_password BY '<password>'` for every account (root and the application account). Only the internal locked accounts `mysql.session` and `mysql.sys` stay on native; the upgrade handles them.
- With `caching_sha2_password` the JDBC connection must use TLS (`sslMode=REQUIRED`, already the default in `deploy/tomcat/rms.xml`) or `allowPublicKeyRetrieval`.

## Steps
1. Back up before each hop.
2. On 8.0: switch every account to `caching_sha2_password` (above).
3. Upgrade 8.0 -> 8.4 -> 9.7, one series at a time.
4. Check `my.cnf` for server settings removed in 9.x (for example `innodb_log_file_size`, which also broke the Testcontainers config).
5. Check: `CHECK TABLE` on `candidate`, `marks`, `users`, `admin` (OK in the rehearsal); the app account logs in over TLS.
6. Then run migrations 001 to 007 as the release requires ([db/migrations/](../db/migrations/)), then deploy the WAR with `mysql-connector-j-9.7.0.jar` in Tomcat's `lib/` ([build-run.md](build-run.md)).

## Verified on 9.7.2 (2026-10-19)
- Rehearsal: after the account fix 8.4 and 9.7 started, data intact (CHECK TABLE OK), the app account logged in over TLS, and migrations 001 to 007 ran cleanly on the upgraded 9.7.
- Migrations 001, 002, 004, 005, 006, 007 from the pre-migration schema on 9.7: no errors or warnings; columns and indexes identical to `db/schema.sql`; rollbacks restore the old schema. Test results: [build-run.md](build-run.md) (Test).
