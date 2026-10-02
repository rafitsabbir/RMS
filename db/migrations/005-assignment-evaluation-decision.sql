-- RMS migration 005: interviewer assignment, evaluations with comments, and decisions (Phase 3 of the roles plan,
-- 2026-10-02).
--
-- STATUS: DRAFT until the owner has compared it with the real marks table (decision D). The marks table in
--         db/schema.sql is inferred from the code; its real column names, types and keys are unknown (G22).
-- WHEN:   once per RMS database, after 004, BEFORE deploying a WAR that contains "Phase 3: assignment, evaluation
--         and decision". Without it Candidate Status, the profile and the evaluation pages fail with
--         "Unknown column 'm.markkey'" (or 'c.decisionreason', or "Table ... doesn't exist").
-- WHO:    the owner or ops. Claude/automation never runs this against a shared or production database.
-- EFFECT: adds 14 columns at the END of marks (existing rows get a generated markkey and NULLs), three nullable
--         columns to candidate, and two new, empty tables. No existing data is changed or removed.
-- WORKS ON: MySQL 5.7 and 8.x. (LOCK IN SHARE MODE, used by the new WAR, works on both.)
--
-- BEFORE (all on a copy first):
--   1. SHOW CREATE TABLE marks;
--      Expect the 13 columns isactive, interviewerid, candidateid, workexp, techknowledge, leadership, decision,
--      probsolving, stress, education, comskill, attitude, personality, in that order, and:
--        - no AUTO_INCREMENT column (a table can have only one; if there is one, markkey can't be added: stop and
--          tell the developers, who will use that column instead);
--        - no columns named like the ones added below;
--        - ENGINE=InnoDB (saving an evaluation runs in a transaction).
--      Send the output to the developers if anything differs.
--   2. The scale of the existing scores (Phase 3 accepts 1 to 10 for new evaluations only; old rows count in the
--      averages as they are, decision E):
--        SELECT MIN(LEAST(workexp, techknowledge, leadership, decision, probsolving, stress, education, comskill,
--               attitude, personality)), MAX(GREATEST(workexp, techknowledge, leadership, decision, probsolving,
--               stress, education, comskill, attitude, personality)), COUNT(*) FROM marks WHERE isactive = 1;
--   3. Duplicate evaluations (RMS keeps one per interviewer and candidate from now on; existing duplicates all
--      count in the averages, and the interviewer edits the oldest):
--        SELECT candidateid, interviewerid, COUNT(*) FROM marks WHERE isactive = 1
--         GROUP BY candidateid, interviewerid HAVING COUNT(*) > 1;
--   4. Statuses in use (RMS reads S, R and H in any case; anything else shows as Pending):
--        SELECT candidatestatus, COUNT(*) FROM candidate GROUP BY candidatestatus;
--   5. Collations: the new tables take the database default, and their ID columns are compared with
--      candidate.candidateid, admin.userid and marks.interviewerid / candidateid. Compare them:
--        SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME FROM information_schema.COLUMNS
--         WHERE TABLE_SCHEMA = DATABASE() AND ((TABLE_NAME = 'candidate' AND COLUMN_NAME = 'candidateid')
--            OR (TABLE_NAME = 'admin' AND COLUMN_NAME = 'userid')
--            OR (TABLE_NAME = 'marks' AND COLUMN_NAME IN ('interviewerid', 'candidateid')));
--        SELECT @@character_set_database, @@collation_database;
--      If any differ from the database default, add CHARACTER SET ... COLLATE ... (that column's pair) to candidateid,
--      interviewerid, assignedby, unassignedby and decidedby below, or MySQL answers "Illegal mix of collations".
--   6. Also from step 1: the marks table's size (adding markkey rebuilds the table and blocks writes while it runs:
--      plan a quiet moment); whether marks has a PRIMARY KEY (markkey then numbers rows in key order, not insert order,
--      so "the oldest" duplicate in check 3 may differ); no existing indexes named marks_markkey or marks_candidate;
--      on MySQL 5.7, the row format (SHOW TABLE STATUS LIKE 'marks') and innodb_large_prefix, because the index on
--      (candidateid, interviewerid) can fail with "key too long" for long utf8mb4 columns (then drop that index line;
--      it only speeds things up). With replication, SHOW VARIABLES LIKE 'binlog_format': with STATEMENT, a replica may
--      number markkey differently; run it in a maintenance window as the DBA prefers.
--
-- PARTIAL FAILURE: MySQL commits each statement below on its own. If one fails, fix the cause and run the remaining
-- statements only, from the one that failed; running the whole script again fails at once with "Duplicate column".
--
-- EXISTING EVALUATIONS: candidate_interviewer starts empty, so after this every evaluation from before Phase 3
-- counts and shows as "unassigned", and its author can't edit it until HR assigns them. If the owner prefers that
-- each existing evaluation's author is assigned to the candidate, run this optional backfill once after the script
-- (it isn't run by default):
--   INSERT INTO candidate_interviewer (candidateid, interviewerid, assignedby, assignedat, isactive)
--   SELECT DISTINCT m.candidateid, m.interviewerid, 'MIGRATION', NOW(), 1
--     FROM marks m JOIN candidate c ON c.candidateid = m.candidateid AND c.isactive = 1
--    WHERE m.isactive = 1;
--
-- ROLLBACK NOTE: a WAR from before Phase 3 reads marks by column position with m.* (MarksMapper), and the status
-- is the column after the 13 marks columns. With the new marks columns that column is markkey, so an older WAR
-- shows every candidate as Pending on Candidate Status (the scores stay right). Run the rollback lines below
-- before going back to an older WAR.
--
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE.

ALTER TABLE marks
	ADD COLUMN markkey INT NOT NULL AUTO_INCREMENT,
	ADD UNIQUE KEY marks_markkey (markkey),
	ADD COLUMN comments VARCHAR(1000) NULL,
	ADD COLUMN workexpcomment VARCHAR(255) NULL,
	ADD COLUMN techknowledgecomment VARCHAR(255) NULL,
	ADD COLUMN leadershipcomment VARCHAR(255) NULL,
	ADD COLUMN decisioncomment VARCHAR(255) NULL,
	ADD COLUMN probsolvingcomment VARCHAR(255) NULL,
	ADD COLUMN stresscomment VARCHAR(255) NULL,
	ADD COLUMN educationcomment VARCHAR(255) NULL,
	ADD COLUMN comskillcomment VARCHAR(255) NULL,
	ADD COLUMN attitudecomment VARCHAR(255) NULL,
	ADD COLUMN personalitycomment VARCHAR(255) NULL,
	ADD COLUMN createdat DATETIME NULL,
	ADD COLUMN updatedat DATETIME NULL,
	ADD KEY marks_candidate (candidateid, interviewerid);

ALTER TABLE candidate
	ADD COLUMN decisionreason VARCHAR(500) NULL,
	ADD COLUMN decisiondate DATE NULL,
	ADD COLUMN decidedby VARCHAR(50) NULL;

CREATE TABLE candidate_interviewer (
	assignmentkey INT NOT NULL AUTO_INCREMENT,
	candidateid VARCHAR(50) NOT NULL,
	interviewerid VARCHAR(50) NOT NULL,
	assignedby VARCHAR(50) NOT NULL,
	assignedat DATETIME NOT NULL,
	isactive TINYINT NOT NULL DEFAULT 1,
	unassignedby VARCHAR(50),
	unassignedat DATETIME,
	PRIMARY KEY (assignmentkey),
	KEY candidate_interviewer_candidate (candidateid, isactive),
	KEY candidate_interviewer_interviewer (interviewerid, isactive)
) ENGINE=InnoDB;

CREATE TABLE candidate_decision (
	decisionkey INT NOT NULL AUTO_INCREMENT,
	candidateid VARCHAR(50) NOT NULL,
	status CHAR(1) NOT NULL,
	reason VARCHAR(500) NOT NULL,
	decisiondate DATE NOT NULL,
	decidedby VARCHAR(50) NOT NULL,
	decidedat DATETIME NOT NULL,
	PRIMARY KEY (decisionkey),
	KEY candidate_decision_candidate (candidateid)
) ENGINE=InnoDB;

-- Existing decisions have no history row: the history starts with the first decision made in RMS. Existing S and
-- R statuses stay and lock their candidates' evaluations, like new ones.
--
-- Checks:
--   SELECT COUNT(*), COUNT(DISTINCT markkey), MIN(markkey) FROM marks;     -- expect n, n, 1 (n = rows before)
--   SELECT COUNT(*) FROM marks WHERE comments IS NOT NULL OR createdat IS NOT NULL;   -- expect 0
--   SELECT COUNT(*) FROM candidate WHERE decisionreason IS NOT NULL;           -- expect 0
--   SELECT COUNT(*) FROM candidate_interviewer; SELECT COUNT(*) FROM candidate_decision;   -- expect 0 and 0
--   SHOW CREATE TABLE marks;              -- the 13 old columns first and unchanged, the new ones after them
--
-- Rollback (only with a WAR from before Phase 3; this forgets every assignment, every comment, every decision's
-- reason, date and author, and the decision history; scores and statuses stay):
--   DROP TABLE candidate_decision;
--   DROP TABLE candidate_interviewer;
--   ALTER TABLE candidate DROP COLUMN decisionreason, DROP COLUMN decisiondate, DROP COLUMN decidedby;
--   ALTER TABLE marks DROP KEY marks_candidate, DROP KEY marks_markkey, DROP COLUMN markkey, DROP COLUMN comments,
--       DROP COLUMN workexpcomment, DROP COLUMN techknowledgecomment, DROP COLUMN leadershipcomment,
--       DROP COLUMN decisioncomment, DROP COLUMN probsolvingcomment, DROP COLUMN stresscomment,
--       DROP COLUMN educationcomment, DROP COLUMN comskillcomment, DROP COLUMN attitudecomment,
--       DROP COLUMN personalitycomment, DROP COLUMN createdat, DROP COLUMN updatedat;
-- Statuses set to H (On hold) by the new WAR show as Pending on older WARs.
