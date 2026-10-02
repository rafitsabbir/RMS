# Roles Plan, Phase 3: Manual Checks

Purpose: The manual SQL checklist and browser test steps for Phase 3 of the roles plan (interviewer assignment, evaluations, Candidate Status averages, decisions).
Last updated: 2026-10-02 (written with the Phase 3 commit; nothing below has been run yet)
Read this when: you're verifying Phase 3 on a machine with Docker, MySQL and Tomcat 11, or signing it off.

**DB-verified: NO.** The DAO tests (`MarksDaoImplTest`, `AssignmentDaoImplTest`, `DecisionDaoImplTest`) were written but skipped on 2026-10-02, because the machine had no Docker. Run them first on a Docker machine: `RMS_REQUIRE_DOCKER=true ./mvnw -B verify`. Mark this line YES only after that passes.

**Migration 005 is a DRAFT** until the owner has compared it with `SHOW CREATE TABLE marks` from production (open question #29).

**Where:** only a throwaway local MySQL (for example `rms_local` from [db/local](../../db/local/README.md)). Never a shared or production server. Only the synthetic seed values below.

## A. Fresh database from the repo
Run `db/local/setup-local-db.ps1` (or `.sh`).

| # | Query | Expected |
|---|---|---|
| A1 | `SELECT (SELECT COUNT(*) FROM marks), (SELECT COUNT(*) FROM candidate_interviewer), (SELECT COUNT(*) FROM candidate_decision);` | 4, 4, 3 |
| A2 | `SELECT markkey, isactive, interviewerid, candidateid, comments, createdat FROM marks ORDER BY markkey;` | 1 1 U2 C1 NULL NULL · 2 1 U2 C2 NULL NULL · 3 1 U5 C1 "Calm and structured." 2026-09-12 15:00:00 · 4 0 U5 C2 NULL NULL |
| A3 | `SELECT candidateid, candidatestatus, decisionreason, decisiondate, decidedby FROM candidate ORDER BY candidateid;` | C1 S "Strong technical interview" 2026-09-20 U3 · C2 R "Not enough experience for the role" 2026-09-22 U3 |
| A4 | The Candidate Status query: `MarksDaoImpl.results` (copy it from the Java string) | C1: evaluations 2, averages 7.0 8.0 7.5 7.0 8.0 7.0 7.5 7.5 8.0 7.0 (MySQL shows 4 decimals) · C2: 1, all row 2's scores (3 4 2 3 4 5 3 2 4 3) |
| A5 | `SELECT @@sql_mode;` | contains `ONLY_FULL_GROUP_BY` (MySQL 8 default); A4 must still run |
| A6 | `SHOW CREATE TABLE marks;` | the 13 original columns first, then `markkey` … `updatedat`; `UNIQUE KEY marks_markkey`, `KEY marks_candidate`; InnoDB |

## B. Migration 005 on a database from before it
```bash
git show 271c4e7:db/schema.sql > /tmp/schema-before.sql
git show 271c4e7:db/test-seed.sql > /tmp/seed-before.sql
```
Load both into an empty scratch database (for example `rms_mig`), then:

| # | Step or query | Expected |
|---|---|---|
| B1 | The BEFORE queries 1–4 at the top of 005 | 13 columns, no AUTO_INCREMENT, InnoDB · min 2, max 9, count 2 · no duplicates · S 1, R 1 |
| B2 | Run 005 | no errors |
| B3 | The check queries at the end of 005 | 2, 2, 1 · 0 · 0 · 0 and 0 · the 13 old columns unchanged and first |
| B4 | `SELECT markkey, interviewerid, candidateid FROM marks ORDER BY markkey;` | 1 U2 C1 · 2 U2 C2 (numbered in insert order) |
| B5 | Run 005 a second time | fails at once with "Duplicate column name 'markkey'"; nothing changes |
| B6 | The rollback lines | the two tables gone; `SHOW CREATE TABLE marks` and `SHOW COLUMNS FROM candidate` as before B2 |
| B6a | (Optional) The backfill at the top of 005, after B2 | 2 rows (U2 for C1 and C2); their evaluations no longer show "unassigned" |
| B7 | (Optional) With 005 applied, run the Phase 2 WAR (`271c4e7`) and open Candidate Status | scores right, every status "Pending" (the documented rollback limit); after B6, statuses right again |

## C. Phase 3 SQL by hand
On the fresh database from A (run the setup again before this section). The DAO statements with parameters filled in.

| # | Statement(s) | Expected |
|---|---|---|
| C1 | `MarksDaoImpl.evaluationsbycandidate` for C1 | markkeys 1 and 3; interviewername Ivan Interviewer / Fay Former; intervieweractive 1 / 0; assigned 1 / 1 |
| C2 | `AssignmentDaoImpl.assignmentsbycandidate` for C1 | U2 (active 1, evaluated 1) and U5 (active 0, evaluated 1) |
| C3 | `AssignmentDaoImpl.assignable` for C1; then `UPDATE candidate_interviewer SET isactive=0 WHERE assignmentkey=1;` and again | no rows; then U2 |
| C4 | `AssignmentDaoImpl.mycandidates` for U2 | C1 and C2, assignedat set, evaluated 1 |
| C5 | Save as U2 for C1: `START TRANSACTION;` `lockcandidate` (C1) | one row, `S`: RMS stops here (locked) and rolls back; run `ROLLBACK;` |
| C6 | `UPDATE candidate SET candidatestatus='H' WHERE candidateid='C1';` then C5 again, `isassigned` (C1, U2), `existingevaluation` (C1, U2), `updatemarks` with markkey 1 and all scores 5, `COMMIT;` | 1 row, `H`; 1; 1; 1 row changed; markkey 1 now 5s with `updatedat` set |
| C7 | In a second session, during C6 before `COMMIT`: `lockcandidate` for C1 | waits until the first session commits (the row lock) |
| C8 | `DecisionDaoImpl.updatecandidate` then `savedecision` for C2 (status H, reason "Second interview", date today, by U1), in a transaction | 1 row each; C2 is H with the new reason; `decisionsbycandidate` for C2 lists it first |
| C9 | `updatecandidate` for a deleted candidate (`UPDATE candidate SET isactive=0 WHERE candidateid='C2';` first) | 0 rows: RMS rolls back and logs nothing |

## D. Browser checks (Tomcat 11, on the `db/local` database)
Make a fresh candidate first (as `test.admin`: Candidates › Add, for example "Dana Doe"), so one candidate isn't decided yet.

| # | Steps | Expected |
|---|---|---|
| D1 | `test.admin`: Candidate Status | C1 2 evaluations, total 74.5, Selected; C2 1, total 33.0, Rejected; Dana 0, "-", Pending |
| D2 | Click Carla Candidate | two rows; Fay Former flagged "interviewer inactive"; the comments "Calm and structured." and "Technical Knowledge: Good Java basics."; average row; history: Selected then On hold |
| D3 | Dana's profile (click the name on Candidates): Interviewers card | "No interviewer is assigned yet."; the Assign list offers only Ivan Interviewer (not HR, the Hiring Manager or the inactive Fay) |
| D4 | Assign Ivan | "Interviewer assigned."; listed with "Not yet"; Ivan no longer in the list |
| D5 | Sign in as `test.interviewer`: menu and Home | "My Evaluations" in the menu and as a tile; no Candidate Status, Candidates or Jobs |
| D6 | My Evaluations | C1 and C2 (Selected / Rejected, "View"), Dana "To do", "Evaluate" |
| D7 | Evaluate Dana: save with one criterion at "-" | "Please score every criterion from 1 to 10." and the typed values stay |
| D8 | Score all 10, add comments, save | "Evaluation saved."; Dana "Submitted"; Edit shows the saved values; editing again updates (no second row: Candidate Status still shows 1 evaluation) |
| D9 | Open Dana's profile from My Evaluations, download a document (upload one first as `test.admin` if needed) | profile with details and documents only (no Interviewers card); the download works |
| D10 | Type `/viewcandidate?candidateid=C1` … with a candidate not assigned to Ivan (assign Dana only; unassign him from C1 as `test.admin` first), and `/downloaddocument/1` | 403 each; WARN lines with keys only |
| D11 | `test.interviewer`: open `/adminviewmarks`, `/viewevaluations?candidateid=C1`, `/viewcandidatelist` | 403 each |
| D12 | `test.hr`: Dana's evaluations, decide On hold with a reason | "Decision saved: On hold."; Ivan can still edit |
| D13 | Decide Selected | "… The candidate's evaluations can no longer be changed."; Ivan's Evaluate shows read-only; a crafted POST to `/saveevaluation` shows "This candidate has been selected…" |
| D14 | Decide with an empty reason, a future date, and without evaluations (on another new candidate) | "Please give the reason…" · "…can't be in the future." · a warning on the page and "Note: there are no evaluations yet." |
| D15 | Unassign Ivan from Dana (confirm prompt) | "Interviewer unassigned…"; his evaluation still on Candidate Status, flagged "unassigned"; on his My Evaluations Dana shows "No longer assigned", View only |
| D16 | `test.admin`: deactivate `test.interviewer` on Users and Roles, then open Dana's and C2's profiles | the assignments remain, flagged "interviewer inactive"; reactivate: the flag goes |
| D17 | `test.manager` (Hiring Manager): Candidate Status, evaluations, a profile | read-only: no decision form, no Assign or Unassign |
| D18 | The candidate list and profile | statuses show Selected / Rejected / On hold / Pending badges |
| D19 | Every new form (assign, unassign, evaluation, decision) | a hidden `_csrf` field |
| D20 | The log after D1–D19 | keys only: no scores, comments, statuses or reasons |
| D21 | `SmokeTest` with `RMS_SMOKE_USER=test.admin` | passes, including `evaluationPagesRender` |
