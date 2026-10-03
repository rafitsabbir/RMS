-- RMS schema (MySQL)
--
-- INFERRED from the DAO SQL, NOT verified against production.
-- Replace with a DDL-only export (mysqldump --no-data) when the owner supplies one
-- (docs/open-questions.md, G22).
--
-- What is confirmed by code and what is guessed:
--   * Table and column names: confirmed by the SQL in rms/dao/*DaoImpl.java.
--   * positionkey / languagekey are AUTO_INCREMENT: the inserts omit them.
--   * marks column ORDER: until Phase 3, MarksMapper read m.* by position (5 and 8-17), so the first 13 columns
--     must stay in this order for older WARs. Since Phase 3 RMS reads marks by column name.
--   * candidate.isactive is NEW (2026-10-02, candidate soft delete): production needs
--     db/migrations/001-candidate-isactive.sql before a WAR with the candidate delete.
--   * admin.role and users.mustchangepassword are NEW (2026-10-02, roles and password security):
--     production needs db/migrations/002-user-roles.sql before a WAR with the roles. users.password
--     must hold 68 characters ({bcrypt} plus a 60-character hash).
--   * candidate.email, phone, source, applieddate and jobkey, and the tables job and candidate_document are NEW
--     (2026-10-02, candidate profile, documents and jobs): production needs
--     db/migrations/004-candidate-documents-jobs.sql before a WAR with them.
--   * marks.markkey, comments, the 10 per-criterion comments, createdat and updatedat (added at the END of marks),
--     candidate.decisionreason, decisiondate and decidedby, and the tables candidate_interviewer and
--     candidate_decision are NEW (2026-10-02, assignment, evaluation and decision): production needs
--     db/migrations/005-assignment-evaluation-decision.sql, checked against SHOW CREATE TABLE marks first.
--   * interview_schedule is NEW (2026-10-03, interview schedule and dashboard): production needs
--     db/migrations/006-interview-schedule.sql before a WAR with it.
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
	decisionreason VARCHAR(500),
	decisiondate DATE,
	decidedby VARCHAR(50),
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
	personality INT NOT NULL DEFAULT 0,
	-- New 2026-10-02 (roles plan Phase 3, migration 005), all at the end so the old column order stays
	markkey INT NOT NULL AUTO_INCREMENT,
	comments VARCHAR(1000),
	workexpcomment VARCHAR(255),
	techknowledgecomment VARCHAR(255),
	leadershipcomment VARCHAR(255),
	decisioncomment VARCHAR(255),
	probsolvingcomment VARCHAR(255),
	stresscomment VARCHAR(255),
	educationcomment VARCHAR(255),
	comskillcomment VARCHAR(255),
	attitudecomment VARCHAR(255),
	personalitycomment VARCHAR(255),
	createdat DATETIME,
	updatedat DATETIME,
	UNIQUE KEY marks_markkey (markkey),
	KEY marks_candidate (candidateid, interviewerid)
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

-- Which interviewers are assigned to a candidate (Phase 3). One active row per candidate and interviewer;
-- unassigning sets isactive=0 (unassignedby/at). The interviewer's evaluation stays and still counts.
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

-- Every decision on a candidate, newest last (Phase 3); candidate.candidatestatus, decisionreason, decisiondate
-- and decidedby hold the latest. status is S (Selected), R (Rejected) or H (On hold).
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

-- An interview of a candidate by an assigned interviewer (Phase 4). startat is a DATETIME with no time zone: the
-- wall-clock time in the MySQL server's time zone, which is what NOW() uses. status is SCHEDULED, DONE or
-- CANCELLED; cancelling keeps the row. isactive is reserved for hiding a row; nothing sets it to 0 yet.
CREATE TABLE interview_schedule (
	schedulekey INT NOT NULL AUTO_INCREMENT,
	candidateid VARCHAR(50) NOT NULL,
	interviewerid VARCHAR(50) NOT NULL,
	startat DATETIME NOT NULL,
	location VARCHAR(200),
	status VARCHAR(10) NOT NULL DEFAULT 'SCHEDULED',
	createdby VARCHAR(50) NOT NULL,
	createdat DATETIME NOT NULL,
	updatedby VARCHAR(50),
	updatedat DATETIME,
	isactive TINYINT NOT NULL DEFAULT 1,
	PRIMARY KEY (schedulekey),
	KEY interview_schedule_candidate (candidateid, isactive),
	KEY interview_schedule_interviewer (interviewerid, isactive, startat)
) ENGINE=InnoDB;

-- The activity log (migration 007): one row per action that matters (evaluations, decisions, interviews, candidates,
-- users, report downloads). No foreign keys, so a row survives its user or record. detail holds codes, never
-- personal data. Nothing in RMS updates or deletes a row.
CREATE TABLE activity_log (
	activitykey BIGINT NOT NULL AUTO_INCREMENT,
	userid VARCHAR(50) NOT NULL,
	action VARCHAR(30) NOT NULL,
	entitytype VARCHAR(20) NULL,
	entityid VARCHAR(50) NULL,
	detail VARCHAR(300) NULL,
	createdat DATETIME NOT NULL,
	PRIMARY KEY (activitykey),
	KEY activity_log_action (action, activitykey),
	KEY activity_log_entity (entitytype, entityid),
	KEY activity_log_user (userid, activitykey)
) ENGINE=InnoDB;
