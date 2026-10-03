-- RMS migration 006: interview schedule (Phase 4 of the roles plan, 2026-10-03).
--
-- WHEN:   once per RMS database, after 005, BEFORE deploying a WAR that contains "Phase 4: interview schedule and
--         dashboard". Without it the schedule pages fail with "Table 'interview_schedule' doesn't exist"; the
--         home page still opens, but without its dashboard (the failure is logged as a WARN).
-- WHO:    the owner or ops. Claude/automation never runs this against a shared or production database.
-- EFFECT: adds one new, empty table. No existing table or row is changed.
-- WORKS ON: MySQL 5.7 and 8.x.
--
-- TIME ZONE: startat is a DATETIME, so MySQL stores exactly the date and time typed on the form, with no time
-- zone. RMS compares it with NOW() ("upcoming interviews"), which is the MySQL server's time zone. So enter every
-- time as the wall-clock time in the server's time zone (check it: SELECT @@global.time_zone, @@session.time_zone,
-- NOW();). The pages show the stored value unchanged. If users work in another zone, say so in the user guide.
--
-- BEFORE (on a copy first):
--   1. Collations: the new table takes the database default, and candidateid and interviewerid are compared with
--      candidate.candidateid and admin.userid. Run check 5 of migration 005; if the pair differs from the database
--      default, add CHARACTER SET ... COLLATE ... to candidateid, interviewerid, createdby and updatedby below, or
--      MySQL answers "Illegal mix of collations".
--
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE.
--
-- isactive is for hiding a row without deleting it. RMS 1.0 never sets it to 0: cancelling an interview sets
-- status = 'CANCELLED' and keeps the row visible.

CREATE TABLE interview_schedule (
	schedulekey INT NOT NULL AUTO_INCREMENT,
	candidateid VARCHAR(50) NOT NULL,
	interviewerid VARCHAR(50) NOT NULL,
	startat DATETIME NOT NULL,
	location VARCHAR(200) NULL,
	status VARCHAR(10) NOT NULL DEFAULT 'SCHEDULED',
	createdby VARCHAR(50) NOT NULL,
	createdat DATETIME NOT NULL,
	updatedby VARCHAR(50) NULL,
	updatedat DATETIME NULL,
	isactive TINYINT NOT NULL DEFAULT 1,
	PRIMARY KEY (schedulekey),
	KEY interview_schedule_candidate (candidateid, isactive),
	KEY interview_schedule_interviewer (interviewerid, isactive, startat)
) ENGINE=InnoDB;

-- Checks:
--   SELECT COUNT(*) FROM interview_schedule;     -- expect 0
--   SHOW CREATE TABLE interview_schedule;
--
-- Rollback (only with a WAR from before Phase 4; this forgets every interview that was scheduled):
--   DROP TABLE interview_schedule;
