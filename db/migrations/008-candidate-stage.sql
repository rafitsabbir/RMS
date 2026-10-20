-- RMS migration 008: hiring stages (owner decision 2026-10-20: the standard stages, linked to the decision).
--
-- WHEN:   once per RMS database, after 007, BEFORE deploying a WAR that contains the hiring stages. Without it the
--         Candidates pages fail with "Unknown column 'c.stage'".
-- WHO:    the owner or ops. Claude/automation never runs this against a shared or production database.
-- EFFECT: adds one nullable column to candidate and fills it, and one new, empty history table. No other column or
--         row is changed or removed.
-- WORKS ON: MySQL 5.7, 8.x and 9.x.
--
-- The stages: APPLIED, SCREENING, INTERVIEW, OFFER, HIRED, WITHDRAWN (rms.model.HiringStage). NULL or any other text
-- reads as APPLIED. Rejected and On hold stay decisions (candidatestatus), not stages. The backfill below puts a
-- Selected candidate at OFFER (Selected moves a candidate to Offer from now on) and everyone else at APPLIED; HR moves
-- them on by hand from the candidate profile. The history starts with the first change made in RMS.
--
-- BEFORE (on a copy first):
--   1. SHOW CREATE TABLE candidate;   -- no column named stage yet
--   2. The history table's candidateid and changedby take the database default collation, like candidate_decision
--      (migration 005). candidateid is compared with candidate.candidateid and changedby with admin.userid; if either
--      has another collation, add CHARACTER SET ... COLLATE ... to that column below, or the profile's history query
--      fails with "Illegal mix of collations":
--        SELECT TABLE_NAME, COLUMN_NAME, COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
--          AND ((TABLE_NAME = 'candidate' AND COLUMN_NAME = 'candidateid') OR (TABLE_NAME = 'admin' AND COLUMN_NAME = 'userid'));
--        SELECT @@collation_database;
--   3. SAFE UPDATES: the backfill UPDATE below has no key in its WHERE. MySQL Workbench (safe updates on) refuses it
--      with error 1175; run SET SQL_SAFE_UPDATES = 0; first in that session (the mysql command line is not affected).
--
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE.

ALTER TABLE candidate ADD COLUMN stage VARCHAR(12) NULL;

UPDATE candidate SET stage = CASE WHEN UPPER(TRIM(candidatestatus)) = 'S' THEN 'OFFER' ELSE 'APPLIED' END
	WHERE stage IS NULL;

CREATE TABLE candidate_stage (
	stagekey INT NOT NULL AUTO_INCREMENT,
	candidateid VARCHAR(50) NOT NULL,
	fromstage VARCHAR(12) NULL,
	tostage VARCHAR(12) NOT NULL,
	note VARCHAR(500) NULL,
	changedby VARCHAR(50) NOT NULL,
	changedat DATETIME NOT NULL,
	PRIMARY KEY (stagekey),
	KEY candidate_stage_candidate (candidateid, stagekey)
) ENGINE=InnoDB;

-- Checks:
--   SELECT stage, COUNT(*) FROM candidate GROUP BY stage;   -- only OFFER and APPLIED, no NULL
--   SELECT COUNT(*) FROM candidate_stage;                   -- expect 0
--   SHOW CREATE TABLE candidate_stage;
--
-- Rollback (only with a WAR from before the hiring stages; this forgets every stage and its history):
--   DROP TABLE candidate_stage;
--   ALTER TABLE candidate DROP COLUMN stage;
