-- RMS migration 007: activity log (owner request, 2026-10-03).
--
-- WHEN:   once per RMS database, after 006, BEFORE deploying a WAR that contains the activity log. Without it
--         the Activity Log page and its CSV download fail with "Table 'activity_log' doesn't exist"; every other
--         action still works, because a failed log write is only a WARN in the server log.
-- WHO:    the owner or ops. Claude/automation never runs this against a shared or production database.
-- EFFECT: adds one new, empty table. No existing table or row is changed.
-- WORKS ON: MySQL 5.7 and 8.x.
--
-- What is logged (Super Admin reads it under Activity Log): MARKED (an evaluation was saved, with its total),
-- SELECTED, REJECTED and ON_HOLD (decisions), ASSIGNED and UNASSIGNED (interviewers), INTERVIEW_SCHEDULED,
-- INTERVIEW_CHANGED and INTERVIEW_CANCELLED, CANDIDATE_ADDED, CANDIDATE_CHANGED and CANDIDATE_DELETED, USER_ADDED,
-- USER_CHANGED, USER_DEACTIVATED, USER_REACTIVATED and PASSWORD_RESET, REPORT_DOWNLOADED. The detail column holds
-- codes and keys only: never a score list, comment, reason, name, e-mail or password.
--
-- userid is not a foreign key on purpose: an entry stays when its user is later removed. The times are the MySQL
-- server's wall-clock time (NOW()), like the interview schedule (see migration 006).
--
-- BEFORE (on a copy first): nothing to compare; the table has no foreign keys, so no collation clash is possible.
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE (RMS never updates this table).
-- Keep the table small: it grows with use. Archive or purge old rows by hand, outside RMS, if the owner sets a
-- retention period.

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

-- Checks:
--   SELECT COUNT(*) FROM activity_log;     -- expect 0
--   SHOW CREATE TABLE activity_log;
--
-- Rollback (only with a WAR from before the activity log; this forgets every logged action):
--   DROP TABLE activity_log;
