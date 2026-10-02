-- RMS migration 002: user roles (admin.role) and the forced password change (users.mustchangepassword),
-- for Users and Roles, Change password and the four roles (Phase 1 of the roles plan, 2026-10-02).
--
-- WHEN:   once per RMS database, BEFORE deploying a WAR that contains "Phase 1: roles, users and password
--         security". Without the columns every login fails with "Sign-in isn't available right now"
--         (Unknown column 'role' / 'u.mustchangepassword').
-- WHO:    the owner or ops, after checking it against the real DDL (SHOW CREATE TABLE users; SHOW CREATE TABLE
--         admin; G22). Claude/automation never runs this against a shared or production database.
-- EFFECT: adds two columns and fills admin.role from the legacy admin.isinterviewer:
--         N -> SUPER_ADMIN, Y -> INTERVIEWER. Rows with any other isinterviewer value (NULL included) keep a NULL
--         role and still have no role, as today. No password, name or flag is changed, nothing is removed.
-- WORKS ON: MySQL 5.7 and 8.x. ROLE is a keyword in MySQL 8 but not a reserved one, so it needs no quotes.
-- SAFE UPDATES: the two UPDATEs have no key in WHERE. In MySQL Workbench (safe updates on) run
--         SET SQL_SAFE_UPDATES = 0; first, or use the mysql command-line client.
--
-- BEFORE: users.password must hold a {bcrypt} hash: 8 + 60 = 68 characters. RMS now stores every new or reset
-- password that way. Check the column's length (and note its character set and collation):
--   SELECT CHARACTER_MAXIMUM_LENGTH, CHARACTER_SET_NAME, COLLATION_NAME, IS_NULLABLE
--     FROM information_schema.COLUMNS
--    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password';
-- Only if the length is below 68, widen it, repeating the NOT NULL / NULL, CHARACTER SET and COLLATE the
-- query showed (MODIFY replaces the whole column definition), for example:
--   ALTER TABLE users MODIFY password VARCHAR(100) NOT NULL;

ALTER TABLE admin ADD COLUMN role VARCHAR(20) NULL;
ALTER TABLE users ADD COLUMN mustchangepassword TINYINT NOT NULL DEFAULT 0;

UPDATE admin SET role = 'SUPER_ADMIN' WHERE UPPER(isinterviewer) = 'N';
UPDATE admin SET role = 'INTERVIEWER' WHERE UPPER(isinterviewer) = 'Y';

-- Checks:
--   SELECT role, isinterviewer, COUNT(*) FROM admin GROUP BY role, isinterviewer;
--     expect SUPER_ADMIN/N and INTERVIEWER/Y (any case of n/y), and NULL only beside other isinterviewer values
--   SELECT mustchangepassword, COUNT(*) FROM users GROUP BY mustchangepassword;   -- expect only 0
--   SELECT COUNT(*) FROM admin WHERE isactive = 1 AND role = 'SUPER_ADMIN';          -- expect at least 1
--
-- After this, RMS writes isinterviewer from the role, so a rollback WAR still works: SUPER_ADMIN -> N,
-- HR, HIRING_MANAGER and INTERVIEWER -> Y (rms.model.Role). Y, not NULL: the released WAR's main.jsp fails
-- with HTTP 500 on a NULL isinterviewer.
--
-- Rollback (only after rolling back to a WAR without the roles; this forgets every role and every pending
-- forced password change). Users added or reset by the new WAR have {bcrypt} passwords, which the WARs from
-- before Spring Security (master, release/phase1) can't check: those users can't sign in there. Older WARs
-- also ignore admin.isactive, so deactivated users can sign in again, and HR and Hiring Manager users get the
-- interviewer menu (no admin pages).
--   ALTER TABLE admin DROP COLUMN role;
--   ALTER TABLE users DROP COLUMN mustchangepassword;
