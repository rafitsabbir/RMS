-- RMS migration 001: candidate.isactive, for the candidate soft delete (G1; owner decision 2026-10-02).
--
-- WHEN:   once per RMS database, BEFORE deploying a WAR that contains the candidate delete
--         (commit "G1: soft-delete candidates" on dev and later). Without the column the Candidates pages
--         fail with "Unknown column 'c.isactive'"; Candidate Status and the other pages don't use it.
-- WHO:    the owner or ops, after checking it against the real DDL (SHOW CREATE TABLE candidate; G22).
--         Claude/automation never runs this against a shared or production database.
-- EFFECT: every existing candidate stays active (DEFAULT 1). No data is changed or removed.
-- WORKS ON: MySQL 5.7 and 8.x.

ALTER TABLE candidate ADD COLUMN isactive TINYINT NOT NULL DEFAULT 1;

-- Check: SELECT isactive, COUNT(*) FROM candidate GROUP BY isactive;   -- expect only 1 right after
--
-- Rollback (only after rolling back to a WAR without the candidate delete; this forgets which
-- candidates were deleted, so they show again):
--   ALTER TABLE candidate DROP COLUMN isactive;
