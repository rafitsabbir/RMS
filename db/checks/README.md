# Pre-release checks

`pre-release-checks.sql` answers the owner's database questions before `dev` is released, in one read-only run ([docs/release-dev.md](../../docs/release-dev.md), *Before the release*, step 2). It replaces collecting the check queries from the migration headers and [docs/open-questions.md](../../docs/open-questions.md) by hand.

## Who and when
- The owner or ops, on the production RMS database or a fresh copy of it, **before** any migration runs.
- Claude and automation never run it against a shared or production database (CLAUDE.md).
- Any account with SELECT on the RMS database works. The last section also reads `mysql.user`, which needs the admin account. Without that right, only that section is refused, and the rest still answers.

## How
```
mysql --force --table <rms database> < pre-release-checks.sql > checks.txt 2> errors.txt
```
- **`--force`:** a check that fails (a missing table, no right on `mysql.user`) prints its error to `errors.txt`, and the run goes on. The error is itself an answer.
- **Send back:** `checks.txt` and `errors.txt`. Look at `errors.txt` first: a denied check names the account and the host it connected from, so remove them if they shouldn't leave the team.
- **Credentials:** pass them as you usually do (an option file or the password prompt), never in the file or on a shared screen.

## What it does and doesn't read
- **Writes nothing:** the session and the transaction are `READ ONLY`, so even a mistaken write is refused ("Cannot execute statement in a READ ONLY transaction"). It uses only `SELECT`, `SHOW` and `information_schema`.
- **Reads out:**
  - the server version, modes, character sets and time zone;
  - the tables' engines, columns, keys and definitions (`SHOW CREATE TABLE`, no data);
  - counts, lengths and single-letter codes.
- **Never reads out:**
  - user names, passwords, e-mail addresses, phone numbers or candidate names;
  - the names or hosts of database accounts (only a count per authentication plugin).

## The sections and the questions they answer
| Section | Answers | Used for |
|---|---|---|
| Server version, modes, character set | #16 | the upgrade steps to 9.7 LTS ([mysql-upgrade.md](../../docs/mysql-upgrade.md)) |
| Time zone | #30 | interview times, upload times and the activity log use the server clock |
| Tables, columns, indexes, `SHOW CREATE TABLE` | #19, #28, #29 (decision D), G22, G28, G33 | `db/schema.sql` against the real DDL; migration 005 stays a draft until `marks` is known |
| Who becomes Super Admin; password column; duplicates; ID forms | #13, #26, #27 | migration 002, new passwords (68 characters), new IDs |
| NULL role and status | #12 | users with no menu; Pending badges |
| Statuses, `marks.isactive`, score scale, duplicate evaluations, orphans | #29 | Candidate Status averages after migration 005 |
| Accounts per authentication plugin; removed settings | [mysql-upgrade.md](../../docs/mysql-upgrade.md) | `mysql_native_password` must go before 8.4; settings 9.x refuses |

## Tested
- On throwaway containers only (2026-10-20):
  - MySQL 5.7.44 with the schema from before the migrations;
  - MySQL 9.7.2 with the current schema and seed;
  - 9.7 as an account with SELECT on the RMS database only. Only the `mysql.user` section was refused.
- No errors otherwise; a write in the same session was refused; no seed name, password or e-mail appeared in the output.
- `src/test/java/rms/dao/PreReleaseChecksTest.java` repeats this on every build with Docker.
