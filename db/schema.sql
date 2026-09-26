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
--   * Types, lengths, keys and NULL rules: guessed. No unique index on names,
--     which keeps the current duplicate behaviour (G33).
-- Used only by the Testcontainers DAO tests (src/test/java/rms/dao).

CREATE TABLE users (
	userid VARCHAR(50) NOT NULL,
	username VARCHAR(100) NOT NULL,
	password VARCHAR(100) NOT NULL,
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
