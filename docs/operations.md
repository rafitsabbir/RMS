# Operations: Backups and Monitoring

Purpose: How ops keep RMS safe and watched once it runs in production: the daily backup of the database and the documents, the restore, and what to monitor.
Last updated: 2026-11-05 (F5: written; the backup script and a restore tested on a trial with MySQL 9.7)
Read this when: you set up the production server, plan backups or alerts, or have to restore RMS.

## Backup
RMS keeps its data in two places that belong together: the **database** and the **documents folder** (`RMS_DOC_DIR`). A document row without its file gives 404 on download; a file without its row is never shown. Back them up together, once a day, and keep copies off the server.

**The script:** [`deploy/backup/rms-backup.sh`](../deploy/backup/rms-backup.sh) makes one set per run in `RMS_BACKUP_DIR/rms-<date>-<time>/`:
- `database.sql.gz`: `mysqldump --single-transaction` of the RMS database (one consistent InnoDB snapshot, with routines, triggers and events), checked for its "Dump completed" line;
- `documents.tar.gz`: the documents folder as stored;
- `SHA256SUMS`: checksums, so a copy can be checked later (`sha256sum -c SHA256SUMS`).

It exits non-zero and removes the incomplete set when anything fails, so the scheduler can alert. After a good set it deletes sets older than `RMS_BACKUP_KEEP_DAYS` (30 by default) in that folder. Settings and the account it needs are in the script's header; the database login comes from a MySQL option file (mode 600), never the command line.

**Schedule:** daily at a quiet time, e.g. a cron line `15 2 * * * <user> /opt/rms/rms-backup.sh` with the environment variables set for that user, or a systemd timer. Then copy the new set off the server (another host, object storage) and keep at least 30 daily sets there; the backup folder holds personal data, so the copy needs the same protection as the database.

**Consistency:** the database is dumped first, then the folder. A document uploaded in between is in the folder copy without its row (never shown, harmless). A document permanently deleted in between (Deleted Candidates, Super Admin only) would have a row without a file; run the backup when nobody purges.

**Tested (2026-11-05, trial, MySQL 9.7.2):** the script made a complete set with an account holding all rights on the RMS database and no global privilege (as the trial's application account); the checksums verified; the dump loaded into an empty MySQL 9.7 gave the same row counts (candidates, evaluations, interviews, activity log) and the document came back byte for byte. Two findings are built into the script and these notes:
- `--set-gtid-purged=OFF` is needed: without it MySQL 9.7's `mysqldump --single-transaction` asks for the `RELOAD` or `FLUSH_TABLES` privilege, which a data-only account doesn't have.
- `mysqldump` 9.x prints "SELECT command denied … column_masking_policy" when the account can't read that `mysql` system table; the dump still completes and holds every RMS table (the script checks the last line).

## Restore
1. Stop Tomcat (users off).
2. Check the set: `sha256sum -c SHA256SUMS` in its folder.
3. Database (admin account): `gzip -dc database.sql.gz | mysql --defaults-extra-file=<admin option file>`. The dump contains `CREATE DATABASE` and `USE` for the RMS database; its tables are replaced.
4. Documents: move the current folder aside, create an empty one with the same owner and mode, then `tar -xzf documents.tar.gz -C "$RMS_DOC_DIR"`.
5. Start Tomcat; check `GET /rms/health` (`ok`), sign in, open a candidate with a CV and download it.

Practise it: restore the latest set onto a test server once a quarter, and after any change to the server or the script.

## Monitoring
| What | How | Alert when |
|---|---|---|
| RMS is up and reaches its database | `GET https://<rms host>/rms/health`, no login, every minute; `ok` with HTTP 200, `unavailable` with 503 ([build-run.md](build-run.md)) | 3 checks in a row are not 200 |
| The backup ran | the script's exit status (cron mail, systemd `OnFailure=`), and the age of the newest `rms-*` folder | a run fails, or the newest set is older than 26 hours |
| Disk space | the volumes holding `RMS_DOC_DIR`, the backup folder, MySQL's data directory and Tomcat's logs | over 80 % used |
| Errors in the log | Tomcat's output (`catalina.out` or the journal): RMS logs through Logback to the console ([config.md](config.md)) | ERROR lines; WARN "Notification … failed" means the mail server refused (F1) |
| Certificate | the HTTPS certificate's expiry date | under 21 days left |
| Sign-in attempts | INFO "Login locked" lines from `LoginThrottle` | many in a short time (someone guessing passwords) |

The log never holds passwords, document names or e-mail addresses; user and candidate IDs only ([conventions.md](conventions.md), Logging).
