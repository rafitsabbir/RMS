-- RMS pre-release checks: one read-only run that answers the owner's database questions before `dev` is released
-- (docs/release-dev.md, "Before the release" step 2; docs/open-questions.md #12, #13, #16, #19, #27-#30).
--
-- WHO:    the owner or ops, on the production RMS database (or a fresh copy of it), BEFORE any migration.
--         Claude/automation never runs this against a shared or production database.
-- HOW:    mysql --force --table <rms database> < pre-release-checks.sql > checks.txt
--         (--force: a check that fails, for example a missing table or no right on mysql.user, prints its error
--         and the run goes on; the error is itself an answer). Send checks.txt to the developers.
-- EFFECT: none. The session and the transaction are READ ONLY, so any write is refused; only SELECT, SHOW and
--         information_schema are used.
-- OUTPUT: structure, counts, lengths and single-letter codes only. No user names, passwords, e-mails, phone numbers,
--         candidate names or hostnames are read out. SHOW CREATE TABLE prints the table definitions (no data).
-- WORKS ON: MySQL 5.7, 8.x and 9.x, on the schema from before the migrations (db/migrations/) as well as after.

SET SESSION TRANSACTION READ ONLY;
START TRANSACTION READ ONLY;

-- ---------------------------------------------------------------------------------------------------------------
SELECT '#16 server version, modes and character set' AS check_;
-- Plans the upgrade steps to 9.7 LTS (docs/mysql-upgrade.md): 5.7 -> 8.0 -> 8.4 -> 9.7.
SELECT VERSION() AS version, @@version_comment AS edition, @@sql_mode AS sql_mode,
	@@character_set_server AS server_charset, @@collation_server AS server_collation,
	@@character_set_database AS db_charset, @@collation_database AS db_collation;

-- ---------------------------------------------------------------------------------------------------------------
SELECT '#30 time zone (startat, uploadedat and the activity log use the server clock)' AS check_;
SELECT @@global.time_zone AS global_zone, @@session.time_zone AS session_zone, @@system_time_zone AS system_zone,
	NOW() AS server_now;

-- ---------------------------------------------------------------------------------------------------------------
SELECT '#19 #27 tables: engine (expect InnoDB), collation, row estimate' AS check_;
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION, TABLE_ROWS
	FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME;

SELECT '#19 #28 #29 columns of the RMS tables, in table order (compare with db/schema.sql)' AS check_;
SELECT TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, EXTRA, COLLATION_NAME
	FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
	ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT '#19 #33 indexes and keys' AS check_;
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME
	FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
	ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

SELECT '#19 #29 table definitions (decision D for migration 005: SHOW CREATE TABLE marks)' AS check_;
SHOW CREATE TABLE marks;
SHOW CREATE TABLE candidate;
SHOW CREATE TABLE users;
SHOW CREATE TABLE admin;
SHOW CREATE TABLE position;
SHOW CREATE TABLE language;

-- ---------------------------------------------------------------------------------------------------------------
SELECT '#27 who becomes Super Admin (migration 002: isinterviewer N -> SUPER_ADMIN, Y -> INTERVIEWER)' AS check_;
SELECT isinterviewer, isactive, COUNT(*) AS users FROM admin GROUP BY isinterviewer, isactive
	ORDER BY isinterviewer, isactive;

SELECT '#27 password column: a new {bcrypt} password needs 68 characters' AS check_;
SELECT CHARACTER_MAXIMUM_LENGTH AS password_column_length FROM information_schema.COLUMNS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password';
SELECT MAX(CHAR_LENGTH(password)) AS longest_stored, SUM(password LIKE '{%}%') AS already_prefixed,
	COUNT(*) AS users FROM users;

SELECT '#13 #27 duplicate userids and usernames (expect 0 each); users and admin rows that do not pair up' AS check_;
SELECT (SELECT COUNT(*) FROM (SELECT userid FROM users GROUP BY userid HAVING COUNT(*) > 1) d) AS dup_users_userid,
	(SELECT COUNT(*) FROM (SELECT username FROM users GROUP BY username HAVING COUNT(*) > 1) d) AS dup_users_username,
	(SELECT COUNT(*) FROM (SELECT userid FROM admin GROUP BY userid HAVING COUNT(*) > 1) d) AS dup_admin_userid,
	(SELECT COUNT(*) FROM users u WHERE NOT EXISTS (SELECT 1 FROM admin a WHERE a.userid = u.userid)) AS users_without_admin,
	(SELECT COUNT(*) FROM admin a WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.userid = a.userid)) AS admin_without_users;

SELECT '#27 #26 ID forms: new IDs continue after the highest U<n> and C<n>' AS check_;
SELECT 'users.userid' AS id_column, SUM(userid REGEXP '^U[0-9]+$') AS u_number_form,
	SUM(NOT userid REGEXP '^U[0-9]+$') AS other_form FROM users
UNION ALL SELECT 'admin.userid', SUM(userid REGEXP '^U[0-9]+$'), SUM(NOT userid REGEXP '^U[0-9]+$') FROM admin
UNION ALL SELECT 'marks.interviewerid', SUM(interviewerid REGEXP '^U[0-9]+$'), SUM(NOT interviewerid REGEXP '^U[0-9]+$') FROM marks
UNION ALL SELECT 'candidate.candidateid', SUM(candidateid REGEXP '^C[0-9]+$'), SUM(NOT candidateid REGEXP '^C[0-9]+$') FROM candidate;

-- ---------------------------------------------------------------------------------------------------------------
SELECT '#12 NULL role and NULL status (each counts as no role / Pending)' AS check_;
SELECT (SELECT COUNT(*) FROM admin WHERE isinterviewer IS NULL) AS admin_null_isinterviewer,
	(SELECT COUNT(*) FROM candidate WHERE candidatestatus IS NULL) AS candidate_null_status;

SELECT '#29 statuses in use (S, R, H are known; anything else shows as Pending)' AS check_;
SELECT candidatestatus, COUNT(*) AS candidates FROM candidate GROUP BY candidatestatus ORDER BY candidatestatus;

SELECT '#29 marks.isactive values (only 1 counts in the new WAR)' AS check_;
SELECT isactive, COUNT(*) AS evaluations FROM marks GROUP BY isactive ORDER BY isactive;

SELECT '#29 score scale of the active evaluations (new ones are 1 to 10, decision E)' AS check_;
SELECT MIN(LEAST(workexp, techknowledge, leadership, decision, probsolving, stress, education, comskill, attitude,
		personality)) AS lowest,
	MAX(GREATEST(workexp, techknowledge, leadership, decision, probsolving, stress, education, comskill, attitude,
		personality)) AS highest,
	COUNT(*) AS evaluations FROM marks WHERE isactive = 1;

SELECT '#29 duplicate active evaluations: interviewer and candidate pairs with more than one row' AS check_;
SELECT COUNT(*) AS duplicate_pairs, COALESCE(MAX(n), 0) AS most_rows_for_one_pair FROM
	(SELECT COUNT(*) AS n FROM marks WHERE isactive = 1 GROUP BY interviewerid, candidateid HAVING COUNT(*) > 1) d;

SELECT '#29 evaluations whose interviewer or candidate has no row' AS check_;
SELECT (SELECT COUNT(*) FROM marks m WHERE NOT EXISTS (SELECT 1 FROM admin a WHERE a.userid = m.interviewerid)) AS no_interviewer,
	(SELECT COUNT(*) FROM marks m WHERE NOT EXISTS (SELECT 1 FROM candidate c WHERE c.candidateid = m.candidateid)) AS no_candidate;

-- ---------------------------------------------------------------------------------------------------------------
SELECT 'upgrade: accounts per authentication plugin (docs/mysql-upgrade.md; needs SELECT on mysql.user)' AS check_;
-- mysql_native_password must be gone before 8.4 (move each account while on 8.0). Counts only, no names.
SELECT plugin, COUNT(*) AS accounts FROM mysql.user WHERE user NOT LIKE 'mysql.%' GROUP BY plugin ORDER BY plugin;

SELECT 'upgrade: settings 8.4 or 9.x no longer accept (also check the server configuration file)' AS check_;
SHOW GLOBAL VARIABLES WHERE Variable_name IN ('innodb_log_file_size', 'innodb_log_files_in_group',
	'query_cache_type', 'query_cache_size', 'default_authentication_plugin', 'expire_logs_days',
	'log_bin', 'binlog_format', 'lower_case_table_names', 'innodb_file_per_table');

COMMIT;
SELECT 'end of the RMS pre-release checks' AS check_;
