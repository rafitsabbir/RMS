#!/usr/bin/env bash
# RMS backup (F5, docs/operations.md): the database and the candidate documents (RMS_DOC_DIR) together, as one set
# with checksums. Run it once a day from cron or a systemd timer, as a user that can read RMS_DOC_DIR, at a quiet
# time. Nothing secret is on the command line or in this file: the database login comes from a MySQL option file.
#
# Settings (environment):
#   RMS_BACKUP_DIR        where the sets go (create it with mode 700; keep a copy off the server, see the docs)
#   RMS_DOC_DIR           the documents folder, as given to Tomcat
#   RMS_DB_NAME           the RMS database
#   RMS_MYSQL_DEFAULTS    a MySQL option file (mode 600) with [client] user, password, host and port of an account
#                         with rights on the RMS database only (no global privileges needed; tested with an
#                         account holding all rights on that database)
#   RMS_BACKUP_KEEP_DAYS  optional, default 30: older sets in RMS_BACKUP_DIR are deleted after a good backup
#
# Exit status: 0 when the set is complete and verified, otherwise not 0 (and the incomplete set is removed), so the
# scheduler or monitoring can alert on it.
set -euo pipefail
umask 077

: "${RMS_BACKUP_DIR:?set RMS_BACKUP_DIR}"
: "${RMS_DOC_DIR:?set RMS_DOC_DIR}"
: "${RMS_DB_NAME:?set RMS_DB_NAME}"
: "${RMS_MYSQL_DEFAULTS:?set RMS_MYSQL_DEFAULTS}"
keep="${RMS_BACKUP_KEEP_DAYS:-30}"

stamp="$(date +%Y%m%d-%H%M%S)"
set_dir="$RMS_BACKUP_DIR/rms-$stamp"
mkdir "$set_dir"
trap 'echo "RMS backup FAILED; removing the incomplete set $set_dir" >&2; rm -rf "$set_dir"' ERR

# 1. The database first: one consistent snapshot (InnoDB), with routines, triggers and events. A document uploaded
#    after this point is in the folder copy without its row (harmless); see docs/operations.md, "Consistency"
mysqldump --defaults-extra-file="$RMS_MYSQL_DEFAULTS" --single-transaction --set-gtid-purged=OFF --routines --triggers --events \
	--hex-blob --no-tablespaces --databases "$RMS_DB_NAME" | gzip -9 > "$set_dir/database.sql.gz"
# mysqldump writes this last line only when it finished
gzip -dc "$set_dir/database.sql.gz" | tail -n 1 | grep -q '^-- Dump completed' \
	|| { echo "the database dump is incomplete" >&2; false; }

# 2. The documents, as stored (random file names; the database maps them to candidates)
tar -C "$RMS_DOC_DIR" -czf "$set_dir/documents.tar.gz" .
tar -tzf "$set_dir/documents.tar.gz" > /dev/null

# 3. Checksums, to tell later whether a copy is still intact: sha256sum -c SHA256SUMS in the set's folder
( cd "$set_dir" && sha256sum database.sql.gz documents.tar.gz > SHA256SUMS )
trap - ERR
echo "RMS backup complete: $set_dir"

# 4. Only after a good backup: remove sets older than the retention period (only rms-<date>-<time> folders)
find "$RMS_BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name 'rms-[0-9]*-[0-9]*' -mtime +"$keep" -print \
	-exec rm -rf {} +
