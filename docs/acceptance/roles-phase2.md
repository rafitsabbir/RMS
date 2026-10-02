# Roles Plan, Phase 2: Manual Checks

Purpose: The manual SQL checklist and browser test steps for Phase 2 of the roles plan (candidate profile fields, jobs, candidate documents and the permanent delete).
Last updated: 2026-10-02 (written with the Phase 2 commit; nothing below has been run yet)
Read this when: you're verifying Phase 2 on a machine with Docker, MySQL and Tomcat 11, or signing it off.

**DB-verified: NO.** The DAO tests (`JobDaoImplTest`, `DocumentDaoImplTest`, the new `CandidateDaoImplTest` cases) were written but skipped on 2026-10-02, because the machine had no Docker. Run them first on a Docker machine: `RMS_REQUIRE_DOCKER=true ./mvnw -B verify`. Mark this line YES only after that passes.

**Where:** only a throwaway local MySQL (for example `rms_local` from [db/local](../../db/local/README.md), or a scratch database next to it), and a scratch folder for `RMS_DOC_DIR`. Never a shared or production server. Only the synthetic seed values and the synthetic files below.

## A. Fresh database from the repo
Run `db/local/setup-local-db.ps1` (or `.sh`), which loads `db/schema.sql` and `db/test-seed.sql`.

| # | Query | Expected |
|---|---|---|
| A1 | `SELECT (SELECT COUNT(*) FROM job), (SELECT COUNT(*) FROM candidate), (SELECT COUNT(*) FROM candidate_document);` | 3, 2, 4 |
| A2 | `SELECT candidateid, email, phone, source, applieddate, jobkey FROM candidate ORDER BY candidateid;` | C1 carla@example.test 000-1001 REFERRAL 2026-09-01 1 · C2 NULL NULL JOB_BOARD 2026-09-15 NULL |
| A3 | `SELECT jobkey, positionkey, vacancies, closingdate, status, isactive FROM job ORDER BY jobkey;` | 1 1 2 2030-12-31 OPEN 1 · 2 2 1 2026-01-31 CLOSED 1 · 3 1 1 NULL OPEN 0 |
| A4 | `SELECT documentkey, candidateid, doctype, isactive, deletedby, LENGTH(storedname) FROM candidate_document ORDER BY documentkey;` | 1 C1 CV 1 NULL 32 · 2 C1 SSC 1 NULL 32 · 3 C1 PROFESSIONAL 1 NULL 32 · 4 C1 CV 0 U3 32 |
| A5 | The Candidates list query: `CandidateDaoImpl.selectcandidate` with `where c.isactive=1 order by length(c.candidateid), c.candidateid` appended (copy it from the Java string; the `SLOT_TYPES` part is `'CV', 'SSC', 'HSC', 'BSC', 'MASTERS', 'PHD'`) | C1: jobstatus OPEN, cvcount 1, slotcount 2, professionalcount 1 · C2: jobstatus NULL, 0, 0, 0 |
| A6 | `SELECT TABLE_NAME, ENGINE, TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('candidate','job','candidate_document');` | `InnoDB` three times, and the same collation for `candidate` and `candidate_document` (open question #28) |

## B. Migration 004 on a database from before it
Build a scratch database from the files as they were after Phase 1, then run the migration:
```bash
git show 2c9a549:db/schema.sql > /tmp/schema-before.sql
git show 2c9a549:db/test-seed.sql > /tmp/seed-before.sql
```
Load both into an empty scratch database (for example `rms_mig`), then run `db/migrations/004-candidate-documents-jobs.sql` against it.

| # | Step or query | Expected |
|---|---|---|
| B1 | Before 004: the two collation queries from the top of 004, and `SELECT @@default_storage_engine;` | the same character set and collation pair twice; `InnoDB` |
| B2 | Run 004 | no errors |
| B3 | The check queries at the end of 004 | 0, 0, 0, and `InnoDB` twice |
| B4 | `SHOW COLUMNS FROM candidate;` | the old columns unchanged, then `email` varchar(150), `phone` varchar(30), `source` varchar(20), `applieddate` date, `jobkey` int, all Null YES with no default |
| B5 | `SHOW INDEX FROM candidate_document;` | `PRIMARY` (documentkey) and `candidate_document_candidate` (candidateid, isactive) |
| B6 | Run 004 a second time | fails at once with "Duplicate column name 'email'"; nothing changes (it isn't meant to run twice) |
| B7 | The rollback lines at the end of 004 | `job` and `candidate_document` gone (`SHOW TABLES`), and B4 shows only the old columns |

## C. Phase 2 SQL by hand
On the fresh database from A (run the setup again before this section). These are the DAO statements with the parameters filled in.

| # | Statement(s) | Expected |
|---|---|---|
| C1 | `JobDaoImpl.alljob`, then `openjob` | jobs 1 and 2 with positions SOFTWARE ENGINEER and QA ENGINEER; then only job 1 |
| C2 | `JobDaoImpl.findjobbyid` with jobkey 3 | no row (deleted) |
| C3 | `UPDATE job SET isactive=0 WHERE jobkey=1;` then A5 | C1 keeps jobkey 1 with jobstatus NULL |
| C4 | `DocumentDaoImpl.documentsbycandidate` for C1 | 1, 2, 3 in that order, uploadedbyname Hana Recruiter, Hana Recruiter, Ada Admin |
| C5 | `DocumentDaoImpl.finddocument` with 4; then with 1 after `UPDATE candidate SET isactive=0 WHERE candidateid='C1';` (undo it afterwards) | no row both times |
| C6 | Replace a CV: `START TRANSACTION;` then `replacedocuments` and `savedocument` for C1, CV (`originalname` 'new.pdf', `storedname` 'b0000000000000000000000000000001', `contenttype` 'application/pdf', `filesize` 100, `uploadedby` 'U3'), then `COMMIT;` | the UPDATE changes 1 row (document 1); the insert gets documentkey 5; afterwards `countdocuments` for C1/CV is 1, and document 1 has isactive 0, deletedby U3 |
| C7 | The same as C6 but with an `originalname` of 300 characters, then `ROLLBACK;` after the error | the INSERT fails with error 1406 "Data too long" (strict mode); after `ROLLBACK`, document 1 (or 5) is still active |
| C8 | `countdocuments` for C1/PROFESSIONAL; insert 4 more PROFESSIONAL rows; count again | 1, then 5 (RMS refuses a sixth in code, not in the database) |
| C9 | `deletedocument` for 2 by U1, twice | 1 row, then 0 rows |
| C10 | `unpurgeddocuments` for C1; then `markpurged` for 1 and 4 (by U1, reason 'Retention period ended'), and for 4 again by U9; then `unpurgeddocuments` again | first 1, 2, 3, 4 (with C6–C9 not run); the second `markpurged` on 4 changes 0 rows; then 2, 3. Document 4 keeps deletedby U3; document 1 gets deletedby U1, purgedby U1 and the reason |
| C11 | A5 after C10 | C1: cvcount 0, slotcount 1 |

## D. Synthetic test files
Make them in a scratch folder (bash; nothing real):
```bash
printf '%%PDF-1.4\n%% synthetic test file\n' > cv.pdf
printf '\x89PNG\r\n\x1a\n synthetic' > cert.png
printf '\xff\xd8\xff\xe0 synthetic' > ssc.jpg
printf '<html>not a pdf</html>' > fake.pdf
printf 'plain text' > notes.txt
{ printf '%%PDF-1.4\n'; head -c 5300000 /dev/zero; } > big.pdf
{ printf '%%PDF-1.4\n'; head -c 9000000 /dev/zero; } > huge.pdf
cp cv.pdf '..%2F..%2Fescape.pdf'
```

## E. Browser checks (Tomcat 11, `RMS_DOC_DIR` set to the scratch folder)
Deploy the WAR on the `db/local` database, with `RMS_DOC_DIR` pointing at an empty scratch folder the Tomcat user can write.

| # | Steps | Expected |
|---|---|---|
| E1 | Start Tomcat once **without** `RMS_DOC_DIR` | it starts; the log has one ERROR "RMS_DOC_DIR is not set…"; C1's profile shows "Document storage isn't configured", no upload form; a document link answers 503. Then set it and restart |
| E2 | `test.admin`: Recruitment › Jobs; add a job (position QA ENGINEER, 3 vacancies, no closing date, Open); edit it to Closed; delete it | each step back on the list; the deleted job is gone; a bad vacancies value ("0", "1000") shows the message |
| E3 | Add a candidate with job "Job 1" and position QA ENGINEER | saved with position SOFTWARE ENGINEER (the job's) |
| E4 | Edit C2: e-mail "nope", then a future applied date | "Please enter a valid e-mail address…", then "The applied date can't be in the future." |
| E5 | Candidates list | C1 "CV ✓ · 2/6", C2 "No CV · 0/6"; the names link to the profiles |
| E6 | C1's profile: download `carla-cv.pdf` (a seed row without a file) | 404; the log has "Document 1 has no stored file" with no file name |
| E7 | C2's profile: upload `cv.pdf` as CV | "CV uploaded."; a file appears as `RMS_DOC_DIR/xx/<32 hex>`; the list shows "CV ✓ · 1/6" |
| E8 | Upload `cert.png` as CV again (Replace) | "CV replaced. The previous file is kept…"; both files are still in the folder; the profile lists only the new one |
| E9 | Download it | the browser saves `cert.png` (not shown inline); response headers have `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`, `Content-Type: image/png` |
| E10 | Upload `fake.pdf`, `notes.txt`, `ssc.jpg` with the kind SSC renamed to `ssc.png`, and an empty file | "The file's content isn't a PDF, JPG or PNG file." · "Only PDF, JPG and PNG files can be uploaded." · "The file's content isn't…" · "Please choose a file."; nothing new in the folder |
| E11 | Upload `big.pdf` (just over 5 MB) | back on the profile with "The file is larger than 5 MB."; nothing saved. `huge.pdf` (9 MB): the same message, or a reset connection if Tomcat's `maxSwallowSize` is the default 2 MB (open question #28) |
| E12 | Upload `..%2F..%2Fescape.pdf` | stored under a random name inside the folder only; shown with its sanitised name; nothing outside `RMS_DOC_DIR` |
| E13 | Professional certificates: upload without a title; with year 1949; then 5 valid ones; then a sixth | "Please enter the certificate's title…" · "The year must be between 1950 and …" · 5 listed · "This candidate already has 5 professional certificates…" |
| E14 | Delete one certificate (confirm prompt) | "Professional certificate deleted."; its file is still in the folder |
| E15 | Permanently delete documents without a reason; then with "Retention period ended" (confirm prompt) | "Please give the reason…"; then "n files deleted permanently." with n counting the replaced and deleted ones too; the folder holds no files of C2; the rows have `purgedby` U1 and the reason (SQL) |
| E16 | `test.hr`: C2's profile | upload, replace and delete shown; no "Permanently delete documents" card; `POST /purgedocuments` (for example replayed from the Super Admin's form) answers 403 |
| E17 | `test.manager` (Hiring Manager): Jobs and C1's profile | lists and profile read-only (no Add, Edit, Upload, Delete); downloads work |
| E18 | `test.interviewer`: open `/viewjoblist`, `/viewcandidate?candidateid=C1`, `/downloaddocument/1` | 403 each (assigned profiles come in Phase 3) |
| E19 | Every new form (job, candidate, upload, delete, permanent delete) | carries a hidden `_csrf` field |
| E20 | The Tomcat log after E1–E19 | no file names, titles or reasons; only keys and counts |
| E21 | `SmokeTest` with `RMS_BASE_URL` and `RMS_SMOKE_USER=test.admin` | passes, including `jobAndProfilePagesRender` |

## F. Rollback check
| # | Steps | Expected |
|---|---|---|
| F1 | With 004 applied, deploy the Phase 1 WAR (commit `2c9a549`) and open Candidates, Positions, Users and Roles | all work: that WAR doesn't read the new columns or tables |
| F2 | Back on the Phase 2 WAR | the jobs, links and documents from E are still there |
