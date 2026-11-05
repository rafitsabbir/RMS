-- RMS migration 011: offer tracking (F8, owner request 2026-11-05).
--
-- WHEN:   once per RMS database, after 010, BEFORE deploying a WAR that contains it. Without it the candidate profile
--         fails for staff with "Table 'candidate_offer' doesn't exist".
-- WHO:    the owner or ops. Claude/automation never runs this against a shared or production database.
-- EFFECT: one new, empty table. No existing table or row is changed.
-- WORKS ON: MySQL 5.7, 8.x and 9.x.
--
-- An offer belongs to a Selected candidate: offer date, salary band (free text, shown to Super Admin and HR only,
-- never logged), start date, status PENDING / ACCEPTED / DECLINED / WITHDRAWN (rms.model.OfferStatus), when it was
-- answered, and a note. A candidate can have several offers over time; at most one waits for an answer
-- (rms.service.OfferServiceImpl). Rows are never deleted by RMS.
--
-- BEFORE (on a copy first): candidateid and the user columns take the database default collation, like
-- candidate_stage (migration 008); if candidate.candidateid or admin.userid has another collation, add
-- CHARACTER SET ... COLLATE ... to those columns below (the query in migration 008's notes shows them).
--
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE.

CREATE TABLE candidate_offer (
	offerkey INT NOT NULL AUTO_INCREMENT,
	candidateid VARCHAR(50) NOT NULL,
	offerdate DATE NOT NULL,
	salaryband VARCHAR(100) NULL,
	startdate DATE NULL,
	status VARCHAR(10) NOT NULL,
	respondeddate DATE NULL,
	note VARCHAR(500) NULL,
	createdby VARCHAR(50) NOT NULL,
	createdat DATETIME NOT NULL,
	updatedby VARCHAR(50) NULL,
	updatedat DATETIME NULL,
	isactive TINYINT NOT NULL DEFAULT 1,
	PRIMARY KEY (offerkey),
	KEY candidate_offer_candidate (candidateid, isactive, offerkey)
) ENGINE=InnoDB;

-- Checks:
--   SELECT COUNT(*) FROM candidate_offer;   -- expect 0
--   SHOW CREATE TABLE candidate_offer;
--
-- Rollback (only with a WAR from before F8; this forgets every offer):
--   DROP TABLE candidate_offer;
