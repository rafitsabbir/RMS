# Roles Plan, Phase 4: Manual Checks

Purpose: The manual SQL checklist and browser test steps for Phase 4 of the roles plan (interview schedule and the home page dashboard).
Last updated: 2026-10-03 (written with the Phase 4 commit; nothing below has been run yet)
Read this when: you're verifying Phase 4 on a machine with Docker, MySQL and Tomcat 11, or signing it off.

**DB-verified: NO.** `ScheduleDaoImplTest` (12 tests) and `DashboardDaoImplTest` (7) were written but skipped on 2026-10-03, because the machine had no Docker, MySQL or Tomcat. None of the SQL in `ScheduleDaoImpl` and `DashboardDaoImpl` has run, and neither have the new JSPs (`viewschedule.jsp`, `myschedule.jsp`, `createschedule.jsp`, `schedulestatus.tag`, the dashboard in `main.jsp`). Run the DAO tests first on a Docker machine: `RMS_REQUIRE_DOCKER=true ./mvnw -B verify`. Then do this checklist.

**Where:** only a throwaway local MySQL (for example `rms_local` from [db/local](../../db/local/README.md)). Never a shared or production server. Only the synthetic seed values below.

## A. Fresh database from the repo
Run `db/local/setup-local-db.ps1` (or `.sh`).

| # | Query | Expected |
|---|---|---|
| A1 | `SELECT COUNT(*) FROM interview_schedule;` | 3 (the script's last row count line shows `interview_schedule` 3) |
| A2 | `SELECT schedulekey, candidateid, interviewerid, startat, location, status FROM interview_schedule ORDER BY schedulekey;` | 1 C1 U2 2099-01-15 10:00:00 Room 1 SCHEDULED · 2 C2 U2 2026-09-18 14:00:00 Online DONE · 3 C1 U5 2099-01-16 09:00:00 NULL CANCELLED |
| A3 | `SHOW CREATE TABLE interview_schedule;` | InnoDB; `startat` DATETIME NOT NULL; keys `interview_schedule_candidate`, `interview_schedule_interviewer` |
| A4 | `SELECT @@global.time_zone, @@session.time_zone, NOW();` | note them: every time on the pages is in this zone (migration 006, TIME ZONE) |

## B. Migration 006 on a database from before it
```bash
git show 0e47bdd:db/schema.sql > /tmp/schema-before.sql
git show 0e47bdd:db/test-seed.sql > /tmp/seed-before.sql
```
Load both into an empty scratch database (for example `rms_mig`), then:

| # | Step or query | Expected |
|---|---|---|
| B1 | `SHOW TABLES LIKE 'interview_schedule';` | no rows |
| B2 | Run `db/migrations/006-interview-schedule.sql` | no errors |
| B3 | The check queries at the end of 006 | 0 rows; the table as in A3 |
| B4 | Run 006 a second time | fails at once with "Table 'interview_schedule' already exists"; nothing changes |
| B5 | The rollback line (`DROP TABLE interview_schedule;`) | the table is gone; nothing else changed (`SHOW TABLES` matches B1) |
| B6 | (Optional) With the table dropped (B5), run the Phase 4 WAR and open Home as `test.admin` | the page opens without the figures; the log has one WARN "The dashboard isn't available" and no stack trace; Interview Schedule shows HTTP 500 |

## C. Phase 4 SQL by hand
On the fresh database from A (run the setup again before this section). The DAO statements with parameters filled in (copy each from the Java string).

| # | Statement | Expected |
|---|---|---|
| C1 | `ScheduleDaoImpl.allschedule` | keys 2, 1, 3 (by start time), with candidatename "Cody Candidate" / "Carla Candidate" / "Carla Candidate" and interviewername "Ivan Interviewer" / "Ivan Interviewer" / "Fay Former" |
| C2 | `scheduleof` for U2, U5, U3 | keys 2, 1 · 3 · none |
| C3 | `UPDATE candidate SET isactive=0 WHERE candidateid='C1';` then `allschedule` and `findschedulebyid` (1) | only key 2; no row for 1. Undo: `UPDATE candidate SET isactive=1 WHERE candidateid='C1';` |
| C4 | `selectschedule` + `upcomingfilter` + `order` (the upcoming list), and `countupcoming` | key 1 only; 1 (row 2 is past and DONE, row 3 is CANCELLED); with `ofinterviewer` for U2: 1, for U5: 0 |
| C5 | `conflict` with U2, startat `2099-01-15 10:00:00`, schedulekey 0; then schedulekey 1; then U5 at `2099-01-16 09:00:00`; then U2 at `2026-09-18 14:00:00` | 1 · 0 · 0 (cancelled) · 0 (done) |
| C6 | `addschedule` for C2, U2, `2099-02-01 09:30:00`, "Room 9", by U3; then `SELECT * FROM interview_schedule WHERE schedulekey=4;` | 1 row; status SCHEDULED, createdby U3, createdat now, updatedby NULL, isactive 1 |
| C7 | `updateschedule` for key 1 (U2, `2099-01-20 15:45:00`, "Online", DONE, by U1) | 1 row changed; `updatedby` U1, `updatedat` set, `candidateid` still C1 |
| C8 | `updateschedule` for key 3 (the cancelled one) with any values | 0 rows changed |
| C9 | `cancelschedule` for key 4, again for key 4, for key 2 (DONE), for key 99 | 1 row · 0 · 0 · 0; key 4 is CANCELLED with `updatedby` set and `isactive` still 1 |
| C10 | `DashboardDaoImpl.bystatus` | S 1, R 1 |
| C11 | `pendingevaluations` as is | 0 (C1 and C2 are decided) |
| C12 | `UPDATE candidate SET candidatestatus=NULL WHERE candidateid='C1'; UPDATE marks SET isactive=0 WHERE interviewerid='U2' AND candidateid='C1';` then `pendingevaluations`, and with `and ci.interviewerid='U2'`, with `'U5'` | 1 · 1 · 0. Then `UPDATE candidate SET candidatestatus='s' WHERE candidateid='C1';` and again: 0 |
| C13 | `openjobs`; then `UPDATE job SET status='OPEN' WHERE jobkey=2;` and again | 1 · 2 |
| C14 | `withoutcv`; then `UPDATE candidate_document SET isactive=0 WHERE documentkey=1;` and again; then `UPDATE candidate_document SET isactive=1, purgedat=NOW() WHERE documentkey=1;` and again | 1 (C2) · 2 · 2 |
| C15 | Run the setup again. `withinactiveinterviewer` as is; then `UPDATE candidate SET candidatestatus='H' WHERE candidateid='C1';` and again; then `UPDATE candidate SET candidatestatus=NULL WHERE candidateid='C2'; UPDATE admin SET role='HR' WHERE userid='U2';` and again | 0 (C1 is decided) · 1 · 2 |
| C16 | `SET SESSION sql_mode='ONLY_FULL_GROUP_BY';` then C10 | still runs (MySQL 8's default) |

## D. Browser checks (Tomcat 11, on the `db/local` database)
Sign in as each seed user (`test.admin`, `test.hr`, `test.manager`, `test.interviewer`; the passwords are in `db/test-seed.sql`). Set `RMS_SMOKE_USER` and `RMS_SMOKE_PASSWORD` to `test.admin`'s and run `SmokeTest` first: `schedulePagesAndDashboardRender` must pass.

| # | As | Steps | Expected |
|---|---|---|---|
| D1 | `test.admin` | Home | "Candidates by status" shows Selected 1, Rejected 1, On hold 0, Pending 0 (2 active). "Needs attention": evaluations still to do 0, upcoming interviews 1, open jobs 1, candidates with no CV 1 (a warning border), candidates with an inactive interviewer 0. "Next interviews" lists 2099-01-15 10:00, Carla Candidate, Ivan Interviewer, Room 1. No "Coming soon" tile; an Interview Schedule tile |
| D2 | `test.admin` | The sidebar | Interview Schedule under Recruitment; no "Coming soon" section |
| D3 | `test.admin` | Interview Schedule | 3 rows by start time (2026-09-18 Done, 2099-01-15 Scheduled, 2099-01-16 Cancelled); Edit on rows 1 and 2, and Cancel on the Scheduled one only; the candidate names link to their profile |
| D4 | `test.hr` | Schedule interview | step 1: a candidate list (C1 and C2); choose C2 and Next: the form with C2's name and the interviewer list (Ivan Interviewer; Fay Former is not offered) |
| D5 | `test.hr` | Save with no interviewer; with no date | the form again with "Please choose an interviewer." / "Please enter the date and time of the interview." |
| D6 | `test.hr` | Choose Ivan, date 2099-01-15 10:00 (Room 1's time) for C2, Save | the form again: "That interviewer already has an interview at that time." |
| D7 | `test.hr` | The same with 2099-01-15 11:00 and "Room 2", Save | the list with "Interview scheduled." and a new Scheduled row |
| D8 | `test.hr` | Edit the new row: set Status to Done and Save | "Interview updated."; the row is Done, and the Cancel button is gone |
| D9 | `test.hr` | Cancel another Scheduled row (confirm prompt, then OK) | "Interview cancelled."; the row is Cancelled and stays in the list; Edit is gone; its URL `/updateschedule/{key}` redirects to the list with "A cancelled interview can't be changed." |
| D10 | `test.hr` | On the profile of C2, unassign Ivan; then Schedule interview for C2 | Ivan is not offered; the form says no active interviewer is assigned (assign again afterwards) |
| D11 | `test.manager` | Home; Interview Schedule | the same figures as D1; the list without Schedule interview, Edit or Cancel; `/createschedule` and `POST /cancelschedule/1` answer 403 |
| D12 | `test.interviewer` | Home | only "Evaluations still to do" and "Upcoming interviews" (their own: 0 and 1, counting D7's row if Ivan was chosen) and "My next interviews"; tiles My Evaluations and My Schedule |
| D13 | `test.interviewer` | My Schedule | only Ivan's rows (no other interviewer's); no actions. `/viewschedulelist` answers 403, and `/myschedule?interviewerid=U5` still shows only Ivan's own rows (the parameter is ignored) |
| D14 | `test.admin` | Make a new candidate with no CV, assign Ivan, deactivate Ivan under Users and Roles, then Home | candidates with no CV 2; candidates with an inactive interviewer 1 (C-new); evaluations still to do 0 for Ivan's pairs. Reactivate Ivan afterwards: 0 |
| D15 | `test.admin` | A candidate's decision to Selected, then Home | Selected 2; the candidate leaves "evaluations still to do" and "inactive interviewer" |
| D16 | any | View source of Home, Interview Schedule and the form | no inline script; names appear escaped; the form has a hidden `_csrf` field and `type="datetime-local"` |
| D17 | `test.hr` | A schedule form posted after the session expired | the login page with "Your session expired" (CSRF), not an error page |
| D18 | `test.hr` | A location of 201 characters | the form again with "Please keep the location to 200 characters." (the field has `maxlength="200"`, so type or paste past it with the browser tools, or POST directly) |
| D19 | `test.hr`, then `test.interviewer` | Unassign Ivan from C1 on the profile. As `test.hr`, open that interview on Interview Schedule and Edit it; as Ivan, open My Schedule and Home | The row is still in the staff list; the edit form lists Ivan as "Ivan Interviewer (not assigned now)" and Save with Status Done works. Ivan's My Schedule, upcoming count and next interviews no longer show it. Changing the interviewer to someone not assigned is refused |

Record the results here, with the date and the machine, when it has run; then change the **DB-verified** line at the top.
