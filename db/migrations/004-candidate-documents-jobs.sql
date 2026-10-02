-- RMS migration 004: candidate profile fields, jobs and candidate documents (Phase 2 of the roles plan, 2026-10-02).
--
-- WHEN:   once per RMS database, after 002 (it doesn't depend on 003), BEFORE deploying a WAR that contains
--         "Phase 2: candidate profile, documents and jobs". Without it the Candidates pages fail with
--         "Unknown column 'c.email'" and the Jobs and document pages with "Table ... doesn't exist".
-- WHO:    the owner or ops, after checking it against the real DDL (SHOW CREATE TABLE candidate; G22).
--         Claude/automation never runs this against a shared or production database.
-- EFFECT: adds five nullable columns to candidate (existing rows get NULLs: no email, phone, source, applied
--         date or job) and two new, empty tables. No existing data is changed or removed.
-- WORKS ON: MySQL 5.7 and 8.x. The new tables take the database's default character set and collation.
--
-- BEFORE:
--   1. The new tables are created as InnoDB (ENGINE=InnoDB below): an upload that replaces a CV writes two rows in
--      one transaction, and locks the candidate's row (SELECT ... FOR UPDATE), which needs candidate to be InnoDB too:
--        SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'candidate';
--      expect InnoDB. On MyISAM the lock does nothing, and the limits hold on one Tomcat only (open question #28).
--   2. candidate_document.candidateid is compared with candidate.candidateid. Note candidate's collation, so the
--      new column can be given the same one if the database default differs (otherwise MySQL may answer
--      "Illegal mix of collations" on the Candidates pages):
--        SELECT CHARACTER_SET_NAME, COLLATION_NAME FROM information_schema.COLUMNS
--         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'candidate' AND COLUMN_NAME = 'candidateid';
--        SELECT @@character_set_database, @@collation_database;          -- expect the same pair
--      If they differ, add CHARACTER SET ... COLLATE ... (the pair from the first query) to the candidateid
--      line of CREATE TABLE candidate_document below.
--   3. Ops: create the document folder and set RMS_DOC_DIR for Tomcat before deploying (docs/config.md).
--
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE: documents are never deleted from the
-- database; a permanent delete removes the file and marks the row. (The row lock, SELECT ... FOR UPDATE, needs
-- SELECT and UPDATE on candidate, which the account already has.)

ALTER TABLE candidate
	ADD COLUMN email VARCHAR(150) NULL,
	ADD COLUMN phone VARCHAR(30) NULL,
	ADD COLUMN source VARCHAR(20) NULL,
	ADD COLUMN applieddate DATE NULL,
	ADD COLUMN jobkey INT NULL;

CREATE TABLE job (
	jobkey INT NOT NULL AUTO_INCREMENT,
	positionkey INT NOT NULL,
	vacancies INT NOT NULL DEFAULT 1,
	closingdate DATE,
	status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
	isactive TINYINT NOT NULL DEFAULT 1,
	PRIMARY KEY (jobkey)
) ENGINE=InnoDB;

CREATE TABLE candidate_document (
	documentkey INT NOT NULL AUTO_INCREMENT,
	candidateid VARCHAR(50) NOT NULL,
	doctype VARCHAR(20) NOT NULL,
	originalname VARCHAR(255) NOT NULL,
	storedname CHAR(32) NOT NULL,
	contenttype VARCHAR(50) NOT NULL,
	filesize INT NOT NULL,
	title VARCHAR(100),
	issuer VARCHAR(100),
	issueyear SMALLINT,
	uploadedby VARCHAR(50) NOT NULL,
	uploadedat DATETIME NOT NULL,
	isactive TINYINT NOT NULL DEFAULT 1,
	deletedby VARCHAR(50),
	deletedat DATETIME,
	purgedby VARCHAR(50),
	purgedat DATETIME,
	purgereason VARCHAR(255),
	PRIMARY KEY (documentkey),
	UNIQUE KEY candidate_document_storedname (storedname),
	KEY candidate_document_candidate (candidateid, isactive)
) ENGINE=InnoDB;

-- Checks:
--   SELECT COUNT(*) FROM candidate WHERE email IS NOT NULL OR phone IS NOT NULL OR source IS NOT NULL
--       OR applieddate IS NOT NULL OR jobkey IS NOT NULL;                       -- expect 0 right after
--   SELECT COUNT(*) FROM job;                                                   -- expect 0
--   SELECT COUNT(*) FROM candidate_document;                                    -- expect 0
--   SHOW INDEX FROM candidate_document;      -- expect PRIMARY, candidate_document_storedname (unique), candidate_document_candidate
--   SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES
--    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('job', 'candidate_document');   -- expect InnoDB twice
--
-- Rollback (only after rolling back to a WAR without Phase 2; this forgets every job, every candidate's email,
-- phone, source, applied date and job link, and every document record). The files in RMS_DOC_DIR stay: without
-- the table nothing points at them any more, so archive or remove that folder separately, as the owner decides.
-- The previous WAR doesn't read any of these columns or tables, so it works with or without this rollback.
--   DROP TABLE candidate_document;
--   DROP TABLE job;
--   ALTER TABLE candidate DROP COLUMN email, DROP COLUMN phone, DROP COLUMN source, DROP COLUMN applieddate,
--       DROP COLUMN jobkey;
