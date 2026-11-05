-- RMS migration 010: several roles per user (F3, owner request 2026-11-05).
--
-- WHEN:   once per RMS database, after 009, BEFORE deploying a WAR that contains it. Without it nobody can sign in
--         (the login reads admin.alsointerviewer) and Users and Roles fails with "Unknown column 'alsointerviewer'".
-- WHO:    the owner or ops. Claude/automation never runs this against a shared or production database.
-- EFFECT: adds one column to admin, 0 for everyone. No row is changed: everyone keeps exactly their role.
-- WORKS ON: MySQL 5.7, 8.x and 9.x.
--
-- What it means: 1 on a Super Admin, HR user or Hiring Manager gives them the Interviewer pages too (My Evaluations,
-- My Schedule) and makes them assignable as an interviewer (rms.model.UserInfo.isAlsointerviewing). It is set under
-- Users and Roles ("Also interviews"), never by this script. On the Interviewer role and on users without a role it
-- has no effect. The role itself (admin.role) and the legacy admin.isinterviewer are not touched.
--
-- BEFORE (on a copy first):
--   SHOW CREATE TABLE admin;   -- no column named alsointerviewer yet
--
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE.

ALTER TABLE admin ADD COLUMN alsointerviewer TINYINT NOT NULL DEFAULT 0;

-- Checks:
--   SELECT alsointerviewer, COUNT(*) FROM admin GROUP BY alsointerviewer;   -- only 0
--
-- Rollback (only with a WAR from before F3; staff who also interviewed lose the Interviewer pages, their evaluations
-- and assignments stay):
--   ALTER TABLE admin DROP COLUMN alsointerviewer;
