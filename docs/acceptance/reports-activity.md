# Reports, Activity Log and English-only: Manual Checks

Purpose: The manual SQL checklist and browser test steps for the reports, the activity log (migration 007) and the English-only rule.
Last updated: 2026-10-03 (written with the change; nothing below has been run yet)
Read this when: you're verifying this change on a machine with Docker, MySQL and Tomcat 11, or signing it off.

**DB-verified: NO.** `ActivityDaoImplTest` (4 tests) was written but skipped on 2026-10-03, because the machine had no Docker, MySQL or Tomcat. The SQL in `ActivityDaoImpl`, migration 007 and the new JSPs (`reports.jsp`, `viewactivity.jsp`) have never run; they were checked by hand against the seed only.

**Where:** only a throwaway local MySQL (for example `rms_local` from [db/local](../../db/local/README.md)). Never a shared or production server. Only the synthetic seed values below.

## A. Fresh database from the repo
Run `db/local/setup-local-db.ps1` (or `.sh`).

| # | Query | Expected |
|---|---|---|
| A1 | `SELECT COUNT(*) FROM activity_log;` | 2 (the script's row count line shows `activity_log` 2) |
| A2 | `SELECT activitykey, userid, action, entitytype, entityid, detail FROM activity_log ORDER BY activitykey;` | 1 U2 MARKED CANDIDATE C1 `total 80` · 2 U3 SELECTED CANDIDATE C1 NULL |
| A3 | `SHOW CREATE TABLE activity_log;` | InnoDB; `activitykey` BIGINT auto-increment; keys on `(action, activitykey)`, `(entitytype, entityid)`, `(userid, activitykey)` |

## B. Migration 007 on a database from before it
```bash
git show b089edf:db/schema.sql > /tmp/schema-before.sql
git show b089edf:db/test-seed.sql > /tmp/seed-before.sql
```
Load both into an empty scratch database (for example `rms_mig`), then:

| # | Step or query | Expected |
|---|---|---|
| B1 | `SHOW TABLES LIKE 'activity_log';` | no rows |
| B2 | Run `db/migrations/007-activity-log.sql` | no errors |
| B3 | `SELECT COUNT(*) FROM activity_log;` and `SHOW CREATE TABLE activity_log;` | 0; the table as in A3 |
| B4 | Run 007 a second time | fails at once with "Table 'activity_log' already exists"; nothing changes |
| B5 | The rollback line (`DROP TABLE activity_log;`) | the table is gone; nothing else changed |
| B6 | (Optional) With the table dropped, run the new WAR, sign in as `test.hr`, save a decision | the decision is saved; the log has one WARN "couldn't be logged" with only an exception class; `/viewactivity` shows HTTP 500 for `test.admin` |

## C. SQL by hand (after A)
| # | Query | Expected |
|---|---|---|
| C1 | `SELECT l.action, concat_ws(' ', a.firstname, a.lastname) FROM activity_log l LEFT JOIN admin a ON a.userid=l.userid ORDER BY l.activitykey DESC LIMIT 500;` | SELECTED first, then MARKED, each with the user's name |
| C2 | `SELECT DISTINCT action FROM activity_log ORDER BY action;` | MARKED, SELECTED |
| C3 | `SELECT COUNT(*) FROM activity_log WHERE action='SELECTED';` | 1 |

## D. Browser checks (Tomcat 11, on the `db/local` database)
Sign in as each seed user (see `db/local/README.md` for the synthetic accounts).

| # | Step | Expected |
|---|---|---|
| D1 | As Super Admin open **Activity Log** in the menu | the two seed entries, newest first, with names; the action filter lists Marked and Selected |
| D2 | Filter by `SELECTED` | one row |
| D3 | As the interviewer, save an evaluation of C1 (scores 1-10 for every criterion) | it saves; Activity Log shows **Marked**, CANDIDATE C1, `total <sum>`, the interviewer's name |
| D4 | As HR, save a decision (Selected, reason, today) for a candidate | Activity Log shows **Selected** (or Rejected, On hold) for that candidate and HR's name; the reason does not appear in the log |
| D5 | As HR, schedule, change and cancel an interview; assign and unassign an interviewer; add, edit and delete a candidate | one entry each: Interview scheduled, Interview changed, Interview cancelled, Assigned, Unassigned, Candidate added, Candidate changed, Candidate deleted |
| D6 | As Super Admin add a user, change the role, deactivate, reactivate, reset the password | User added, User changed, User deactivated, User reactivated, Password reset |
| D7 | As HR, HM and Interviewer open `/viewactivity` | HR and Hiring Manager: HTTP 403; Interviewer: HTTP 403 |
| D8 | As Super Admin open **Reports** | four downloads (Candidates, Candidate Status, Interview Schedule, Activity Log) |
| D9 | As HR open **Reports** | three downloads, no Activity Log; `/exportactivity` gives HTTP 403 |
| D10 | Download Candidates in Excel | opens with the header row and the two seed candidates; accents and the phone number look right; a phone starting with `+` shows with an apostrophe |
| D11 | Download Candidate Status | one row per candidate, 10 averages to one decimal, a total, the status; a candidate with no evaluations has empty averages |
| D12 | Download Interview Schedule | the three seed interviews with times as on the page |
| D13 | Open Activity Log again | three **Report downloaded** entries (one per download) |
| D14 | As an Interviewer open `/exportcandidates` | HTTP 403; no entry in the log |
| D15 | Add a candidate named `Jos` + `é` (e with an acute accent) | refused: "Names can use English letters only..."; nothing saved |
| D16 | Add a candidate with a Bengali or Cyrillic name | refused with the same message |
| D17 | Save a decision reason, an evaluation comment, a position or language name, a user name and an interview location with non-English text | each is refused with an "English" message and nothing is saved or logged |
| D18 | Enter a candidate named `Anna-Marie O'Neil Jr.` | saved |
| D19 | In a candidate's decision reason use two lines of English text | saved |
| D20 | Edit a candidate whose name already holds a non-English character (set it by SQL first) | the form refuses until the name is changed to English |
