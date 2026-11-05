-- RMS migration 009: interview rounds (F2, owner request 2026-11-05).
--
-- WHEN:   once per RMS database, after 008, BEFORE deploying a WAR that contains the interview rounds. Without it the
--         Interview Schedule, My Evaluations and evaluation pages fail with "Unknown column 'interviewround'".
-- WHO:    the owner or ops. Claude/automation never runs this against a shared or production database.
-- EFFECT: adds one nullable column at the END of marks and one to interview_schedule. No row is changed: existing
--         interviews and evaluations have no round (NULL, shown as "No round") and keep counting as before.
-- WORKS ON: MySQL 5.7, 8.x and 9.x.
--
-- The rounds: FIRST, TECHNICAL, FINAL (rms.model.InterviewRound). NULL or any other text reads as no round. An
-- interviewer keeps one evaluation per candidate and round (rms.dao.MarksDaoImpl.saveMarks), so the same interviewer
-- can evaluate a candidate again in a later round. Candidate Status still averages every active evaluation.
--
-- BEFORE (on a copy first):
--   SHOW CREATE TABLE marks;                -- no column named interviewround yet; it goes at the END (older WARs
--   SHOW CREATE TABLE interview_schedule;   -- read marks by position, G10)
--
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE.

ALTER TABLE marks ADD COLUMN interviewround VARCHAR(12) NULL;

ALTER TABLE interview_schedule ADD COLUMN interviewround VARCHAR(12) NULL;

-- Checks:
--   SELECT COUNT(*) FROM marks WHERE interviewround IS NOT NULL;                -- expect 0
--   SELECT COUNT(*) FROM interview_schedule WHERE interviewround IS NOT NULL;   -- expect 0
--   SHOW CREATE TABLE marks;                                                    -- interviewround is the last column
--
-- Rollback (only with a WAR from before the rounds; this forgets every round given since):
--   ALTER TABLE interview_schedule DROP COLUMN interviewround;
--   ALTER TABLE marks DROP COLUMN interviewround;
