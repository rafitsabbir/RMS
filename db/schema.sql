-- RMS schema (MySQL)
--
-- INFERRED from the DAO SQL, NOT verified against production.
-- Replace with a DDL-only export (mysqldump --no-data) when the owner supplies one
-- (docs/open-questions.md, G22).
--
-- What is confirmed by code and what is guessed:
--   * Table and column names: confirmed by the SQL in rms/dao/*DaoImpl.java.
--   * positionkey / languagekey are AUTO_INCREMENT: the inserts omit them.
--   * marks column ORDER: m.* fills result columns 5-17; MarksMapper reads 5 and 8-17
--     by position (MarksDaoImpl.java:44-55), so marks must have exactly 13 columns in this order.
--     Columns 6-7 are not read; interviewerid/candidateid are assumed there.
--   * candidate.isactive is NEW (2026-10-02, candidate soft delete): production needs
--     db/migrations/001-candidate-isactive.sql before a WAR with the candidate delete.
--   * admin.role and users.mustchangepassword are NEW (2026-10-02, roles and password security):
--     production needs db/migrations/002-user-roles.sql before a WAR with the roles. users.password
--     must hold 68 characters ({bcrypt} plus a 60-character hash).
--   * candidate.email, phone, source, applieddate and jobkey, and the tables job and candidate_document are NEW
--     (2026-10-02, candidate profile, documents and jobs): production needs
--     db/migrations/004-candidate-documents-jobs.sql before a WAR with them.
--   * Types, lengths, keys and NULL rules: guessed. No unique index on names,
--     which keeps the current duplicate behaviour (G33).
-- Used only by the Testcontainers DAO tests (src/test/java/rms/dao).

CREATE TABLE users (
	userid VARCHAR(50) NOT NULL,
	username VARCHAR(100) NOT NULL,
	password VARCHAR(100) NOT NULL,
	mustchangepassword TINYINT NOT NULL DEFAULT 0,
	PRIMARY KEY (userid)
);

CREATE TABLE admin (
	userid VARCHAR(50) NOT NULL,
	username VARCHAR(100),
	isactive TINYINT NOT NULL DEFAULT 1,
	firstname VARCHAR(100),
	lastname VARCHAR(100),
	email VARCHAR(150),
	phone VARCHAR(30),
	designation VARCHAR(100),
	isinterviewer CHAR(1),
	role VARCHAR(20),
	PRIMARY KEY (userid)
);

CREATE TABLE position (
	positionkey INT NOT NULL AUTO_INCREMENT,
	positionname VARCHAR(100) NOT NULL,
	isactive TINYINT NOT NULL DEFAULT 1,
	PRIMARY KEY (positionkey)
);

CREATE TABLE language (
	languagekey INT NOT NULL AUTO_INCREMENT,
	languagename VARCHAR(100) NOT NULL,
	isactive TINYINT NOT NULL DEFAULT 1,
	PRIMARY KEY (languagekey)
);

CREATE TABLE candidate (
	candidateid VARCHAR(50) NOT NULL,
	firstname VARCHAR(100),
	lastname VARCHAR(100),
	positionkey INT,
	languagekey INT,
	candidatestatus CHAR(1),
	isactive TINYINT NOT NULL DEFAULT 1,
	email VARCHAR(150),
	phone VARCHAR(30),
	source VARCHAR(20),
	applieddate DATE,
	jobkey INT,
	PRIMARY KEY (candidateid)
);

CREATE TABLE marks (
	isactive TINYINT NOT NULL DEFAULT 1,
	interviewerid VARCHAR(50) NOT NULL,
	candidateid VARCHAR(50) NOT NULL,
	workexp INT NOT NULL DEFAULT 0,
	techknowledge INT NOT NULL DEFAULT 0,
	leadership INT NOT NULL DEFAULT 0,
	decision INT NOT NULL DEFAULT 0,
	probsolving INT NOT NULL DEFAULT 0,
	stress INT NOT NULL DEFAULT 0,
	education INT NOT NULL DEFAULT 0,
	comskill INT NOT NULL DEFAULT 0,
	attitude INT NOT NULL DEFAULT 0,
	personality INT NOT NULL DEFAULT 0
);

-- A job opening built on a position (Phase 2 of the roles plan). status is OPEN or CLOSED, set by HR; a
-- candidate may be linked to one job (candidate.jobkey). Soft delete (isactive).
CREATE TABLE job (
	jobkey INT NOT NULL AUTO_INCREMENT,
	positionkey INT NOT NULL,
	vacancies INT NOT NULL DEFAULT 1,
	closingdate DATE,
	status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
	isactive TINYINT NOT NULL DEFAULT 1,
	PRIMARY KEY (jobkey)
) ENGINE=InnoDB;

-- A candidate's uploaded document (Phase 2). The file itself is in RMS_DOC_DIR under storedname (32 random hex
-- characters), never under originalname, which is only shown. doctype is a rms.model.DocumentType name.
-- isactive=0: replaced or deleted (deletedby/at), and the file is kept; purgedat set: the file was removed by
-- a Super Admin's permanent delete (purgedby, purgereason). Rows are never deleted.
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
