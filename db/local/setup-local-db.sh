#!/usr/bin/env bash
# Build the local RMS database (rms_local) on a local MySQL server:
#   00-create-database.sql -> ../schema.sql -> ../test-seed.sql -> row counts.
# DESTRUCTIVE: drops and recreates rms_local. See README.md.
#
# Environment (all optional):
#   RMS_DB_HOST (default 127.0.0.1), RMS_DB_PORT (3306), RMS_DB_USER (root)
#   RMS_DB_PASSWORD      if set, passed through MYSQL_PWD; otherwise mysql prompts
#   RMS_DB_ALLOW_REMOTE  set to true to allow a non-loopback host
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
db_dir="$(dirname "$here")"

host="${RMS_DB_HOST:-127.0.0.1}"
port="${RMS_DB_PORT:-3306}"
user="${RMS_DB_USER:-root}"

case "$host" in
	localhost|127.0.0.1|::1) ;;
	*)
		if [ "${RMS_DB_ALLOW_REMOTE:-}" != "true" ]; then
			echo "Refusing non-local host '$host'. Set RMS_DB_ALLOW_REMOTE=true only for a throwaway server." >&2
			exit 1
		fi
		;;
esac

command -v mysql >/dev/null 2>&1 || { echo "mysql client not found on PATH." >&2; exit 1; }

for f in "$here/00-create-database.sql" "$db_dir/schema.sql" "$db_dir/test-seed.sql"; do
	[ -f "$f" ] || { echo "Missing $f" >&2; exit 1; }
done

args=(--protocol=TCP -h "$host" -P "$port" -u "$user" --default-character-set=utf8mb4 --table)
if [ -n "${RMS_DB_PASSWORD:-}" ]; then
	export MYSQL_PWD="$RMS_DB_PASSWORD"
else
	args+=(-p)
fi

echo "Rebuilding rms_local on $host:$port as $user ..."
{
	cat "$here/00-create-database.sql"
	cat "$db_dir/schema.sql"
	cat "$db_dir/test-seed.sql"
	cat <<'SQL'
SELECT 'users' AS tbl, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'admin', COUNT(*) FROM admin
UNION ALL SELECT 'position', COUNT(*) FROM position
UNION ALL SELECT 'language', COUNT(*) FROM language
UNION ALL SELECT 'candidate', COUNT(*) FROM candidate
UNION ALL SELECT 'marks', COUNT(*) FROM marks;
SQL
} | mysql "${args[@]}"

echo "Done. Expected rows: users 2, admin 2, position 3, language 3, candidate 2, marks 2."
