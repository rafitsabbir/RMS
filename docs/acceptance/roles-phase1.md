# Roles Plan, Phase 1: Manual Checks

Purpose: The manual SQL checklist and browser test steps for Phase 1 of the roles plan (roles, Users and Roles, Change password, password storage, inactive users, login lock).
Last updated: 2026-10-02 (written with the Phase 1 commit; nothing below has been run yet)
Read this when: you're verifying Phase 1 on a machine with Docker, MySQL and Tomcat 11, or signing it off.

**DB-verified: NO.** The DAO tests (`UserDaoImplTest`, `LoginDaoImplTest`) were written but skipped on 2026-10-02, because the machine had no Docker. Run them first on a Docker machine: `RMS_REQUIRE_DOCKER=true ./mvnw -B verify`. Mark this line YES only after that passes.

**Where:** only a throwaway local MySQL (for example `rms_local` from [db/local](../../db/local/README.md), or a scratch database next to it). Never a shared or production server. Only the synthetic seed values below.

## A. Fresh database from the repo
Run `db/local/setup-local-db.ps1` (or `.sh`), which loads `db/schema.sql` and `db/test-seed.sql`.

| # | Query | Expected |
|---|---|---|
| A1 | `SELECT COUNT(*) FROM users; SELECT COUNT(*) FROM admin;` | 5 and 5 |
| A2 | `SELECT a.userid, u.username, a.role, a.isinterviewer, a.isactive FROM admin a JOIN users u ON u.userid=a.userid ORDER BY a.userid;` | U1 test.admin SUPER_ADMIN N 1 · U2 test.interviewer INTERVIEWER Y 1 · U3 test.hr HR Y 1 · U4 test.manager HIRING_MANAGER Y 1 · U5 test.former INTERVIEWER Y 0 |
| A3 | `SELECT userid, LENGTH(password), mustchangepassword FROM users ORDER BY userid;` | U1 and U2: 11, 0 (plain text). U3–U5: 68, 0 (`{bcrypt}`) |
| A4 | `SELECT @@sql_mode;` | contains `STRICT_TRANS_TABLES` (the DAO test `addIsAllOrNothing` relies on it) |
| A5 | `SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('users','admin');` | both `InnoDB` (the add-user transaction needs it; open question #27) |

## B. Migration 002 on a database from before it
Build a scratch database from the files as they were before Phase 1, then run the migration:
```bash
git show b07dee3:db/schema.sql > /tmp/schema-before.sql
git show b07dee3:db/test-seed.sql > /tmp/seed-before.sql
```
Load both into an empty scratch database (for example `rms_mig`), then run `db/migrations/002-user-roles.sql` against it.

| # | Step or query | Expected |
|---|---|---|
| B1 | Before 002: the password-length query from the top of 002 | length `100` and `IS_NULLABLE` `NO` (plus the database's character set and collation). 100 ≥ 68, so no `MODIFY` |
| B2 | Run 002 | no errors. In MySQL Workbench with safe updates on, the two `UPDATE`s fail with error 1175 unless `SET SQL_SAFE_UPDATES = 0;` is run first |
| B3 | `SELECT userid, role, isinterviewer FROM admin ORDER BY userid;` | U1 SUPER_ADMIN N · U2 INTERVIEWER Y |
| B4 | `SELECT userid, mustchangepassword FROM users ORDER BY userid;` | U1 0 · U2 0 |
| B5 | `SHOW COLUMNS FROM admin LIKE 'role'; SHOW COLUMNS FROM users LIKE 'mustchangepassword';` | `varchar(20)`, Null YES, no default · `tinyint`, Null NO, Default 0 |
| B6 | The three check queries at the end of 002 | SUPER_ADMIN/N 1 and INTERVIEWER/Y 1; only 0; at least 1 |
| B7 | `INSERT INTO admin (userid, isactive, isinterviewer) VALUES ('U8', 1, NULL), ('U9', 1, 'x');` then rerun only the two `UPDATE`s | U8 and U9 keep `role` NULL (no role; they still see only Home) |
| B8 | Run 002 a second time | fails at once with "Duplicate column name 'role'"; nothing changes (it isn't meant to run twice) |
| B9 | The two rollback lines | both columns gone: B5's `SHOW COLUMNS` return no rows |

## C. The Phase 1 SQL, run by hand on database A
Copy each statement from `UserDaoImpl` / `LoginDaoImpl` with the named parameters replaced by the values shown.

| # | Statement | Expected |
|---|---|---|
| C1 | `lastnumber` (`UserDaoImpl`) | `5` |
| C2 | `INSERT INTO marks (isactive, interviewerid, candidateid) VALUES (1, 'U20', 'C1');` then C1 again | `20`. Then `DELETE FROM marks WHERE interviewerid='U20';` |
| C3 | `INSERT INTO users (userid, username, password) VALUES ('U1234567890', 'long.id', 'x'), ('U12A', 'odd.id', 'x');` then C1 again | still `5` (other formats are ignored). Then delete both rows |
| C4 | `allusers` | 5 rows, U1…U5, with usernames, roles, `isactive` and `mustchangepassword` 0 |
| C5 | `userinfo` (`LoginDaoImpl`) with `:userid` = `'U2'` | one row: the admin columns plus `mustchangepassword` 0 |
| C6 | `usernamecount` with `'TEST.ADMIN'` | `1` (case-insensitive under the default collation) |
| C7 | `activesuperadmins` | `1` |
| C8 | `START TRANSACTION;` then `savelogin` with U6 / `new.user` / `'{bcrypt}x'`, then `saveprofile` with U6 and a 101-character first name | the second insert fails with error 1406 "Data too long". Then `ROLLBACK;` and `SELECT COUNT(*) FROM users;` gives `5` |
| C9 | `updateuser` for U2 with role `SUPER_ADMIN` and isinterviewer `N`, then `SELECT role, isinterviewer, username FROM admin WHERE userid='U2';` | `SUPER_ADMIN`, `N`, `test.interviewer` (username unchanged). Reload the seed afterwards |

## D. Migration 003 (password hashing), on database A only
Set `RMS_DB_URL` (pointing at the local database), `RMS_DB_USER` and `RMS_DB_PASSWORD` in the shell, then follow [db/migrations/003-hash-passwords/README.md](../../db/migrations/003-hash-passwords/README.md).

| # | Step | Expected |
|---|---|---|
| D1 | Dry run | `holds 100 characters`; to hash 2; starting with { 3; empty 0; longer than 72 bytes 0; "Dry run: nothing was changed" |
| D2 | `SELECT userid, LENGTH(password) FROM users ORDER BY userid;` | unchanged (U1, U2: 11) |
| D3 | `--apply` | "Hashed and committed: 2 rows." |
| D4 | D2 again | all five 68 |
| D5 | Sign in as `test.admin` with its seed password on the `dev` WAR | works |
| D6 | Run `--apply` again | to hash 0; nothing to do |

## E. Browser checks (Tomcat 11, `dev` WAR, database A)
Sign in with the seed users (passwords in `db/test-seed.sql`). Run `SmokeTest` too, with `RMS_SMOKE_USER=test.admin`; it now includes `userAndPasswordPagesRender`.

| # | Check | Expected |
|---|---|---|
| E1 | Each seed user signs in | Super Admin: Recruitment, Master data, Administration, Account. HR: no Administration. Hiring Manager: Candidate Status and Candidates only. Interviewer: Home, the two "Soon" entries, Account. Home tiles match |
| E2 | `test.former` (inactive) with the right password | "Invalid login!" |
| E3 | Hiring Manager opens Candidates | no Add button and no Actions column; typing `/createcandidate` gives 403 |
| E4 | HR types `/viewuserlist`; Interviewer types `/viewpositionlist`; anyone types `/nosuchpage` | 403 each |
| E5 | Super Admin adds a user (role HR, temporary password) | new row U6 with "Must change password"; signing in as them leads straight to Change password; every other page redirects there until it's changed |
| E6 | Add with a taken username (`TEST.ADMIN`), a short password, a mismatch | the matching messages; nothing saved |
| E7 | Edit U6: change role to Hiring Manager while U6 is signed in elsewhere | U6's next click goes to the login page with "You were signed out because your account changed." |
| E8 | Deactivate U6, then try to sign in as U6 | the list shows Inactive; "Invalid login!". Reactivate: signing in works again |
| E9 | Super Admin opens their own row | no Deactivate button; role read-only; no Reset password card |
| E10 | Reset U2's password | "The password was reset"; U2 must change it at next sign-in. `SELECT LENGTH(password), mustchangepassword FROM users WHERE userid='U2'` gives 68, 1 |
| E11 | Change password as U3: wrong current; then the right one | "The current password is wrong."; then "Your password was changed." and the new one works at the next sign-in |
| E12 | 5 wrong passwords for `test.interviewer`, then the right one | still "Invalid login!" for 15 minutes from that browser's address; `test.admin` still signs in. The log has one WARN "Login locked for 15 minutes after 5 failures", with no username |
| E13 | View the page source of Users and Roles and Change password | every POST form has a hidden `_csrf` field; no passwords in the page |

## F. Rollback check for `isinterviewer` (owner decision B)
Optional, on Tomcat 9 with the `master` WAR (`d535f1c`) against a scratch database after migration 002. That WAR compares passwords in SQL, so give these users plain-text passwords.

| # | Check | Expected |
|---|---|---|
| F1 | A user written by the new WAR as HR (`isinterviewer = 'Y'`) signs in | the old interviewer menu; every admin page gives 403 (its `AuthInterceptor` admits only `N`) |
| F2 | For comparison, set one user's `isinterviewer` to NULL and sign in | HTTP 500 after the login: `main.jsp` line 43 calls `getIsinterviewer().equalsIgnoreCase("N")` on NULL. This is why HR and Hiring Manager are written as `Y`, not NULL |
| F3 | A deactivated user (`admin.isactive = 0`) signs in | works: that WAR ignores `isactive` (noted in 002's rollback section) |
