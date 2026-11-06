-- RMS migration 012: data retention (F11, owner request 2026-11-05).
--
-- WHEN:   once per RMS database, after 011, BEFORE deploying a WAR that contains it. Without it the Candidates pages
--         fail with "Unknown column 'anonymizedat'".
-- WHO:    the owner or ops. Claude/automation never runs this against a shared or production database.
-- EFFECT: adds two nullable columns to candidate. No row is changed.
-- WORKS ON: MySQL 5.7, 8.x and 9.x.
--
-- Retention is OFF until RMS_RETENTION_MONTHS is set (docs/config.md). Then a Super Admin sees, under Data
-- retention, the candidates not hired whose last activity is older than that period, and confirms the clean-up:
-- RMS deletes their document files and removes their personal data from these rows (name, e-mail, phone, reasons,
-- notes, comments, salary band, document names), keeps the rows for the counts, and sets anonymizedat/anonymizedby.
--
-- The database account RMS uses still needs only SELECT, INSERT and UPDATE.

ALTER TABLE candidate ADD COLUMN anonymizedat DATETIME NULL;
ALTER TABLE candidate ADD COLUMN anonymizedby VARCHAR(50) NULL;

-- Checks:
--   SELECT COUNT(*) FROM candidate WHERE anonymizedat IS NOT NULL;   -- expect 0
--
-- Rollback (only with a WAR from before F11; the personal data already removed does not come back):
--   ALTER TABLE candidate DROP COLUMN anonymizedby;
--   ALTER TABLE candidate DROP COLUMN anonymizedat;
