# Local RMS database

Everything needed to build a local MySQL database for RMS on any PC: the database, the tables (DDL) and the seed data (DML).

## What you get, and its limits
- **Database:** `rms_local` (utf8mb4), with the six tables `users`, `admin`, `position`, `language`, `candidate` and `marks`.
- **Schema:** `../schema.sql`, which is **inferred** from the DAO SQL, not exported from production ([docs/open-questions.md](../../docs/open-questions.md) #19, G22).
- **Data:** `../test-seed.sql`, which is **synthetic** test data. There are no real people or real data.
- **Target:** only a local or throwaway MySQL server. Never run these scripts against a shared or production server.

`db/schema.sql` and `db/test-seed.sql` are the single source of truth; the DAO tests load the same two files (`MySqlContainerSupport.java`). To change the schema or seed, edit those files, not copies. This folder only wraps them.

## Files
| File | Purpose |
|---|---|
| `00-create-database.sql` | Drops and recreates `rms_local` (**destructive**) |
| `setup-local-db.ps1` | Windows: runs `00-create-database.sql` → `../schema.sql` → `../test-seed.sql`, then prints row counts |
| `setup-local-db.sh` | The same for bash (Linux, macOS, Git Bash) |
| `docker-compose.yml` | Optional throwaway `mysql:8.0` that builds itself from `../schema.sql` and `../test-seed.sql` |

## Option A: an existing local MySQL (5.7 or 8.x)
The `mysql` client must be on `PATH`. The script prompts for the password, unless `RMS_DB_PASSWORD` is set.

Windows (PowerShell), from the repo root:
```powershell
powershell -ExecutionPolicy Bypass -File db\local\setup-local-db.ps1
```

bash, from the repo root:
```bash
./db/local/setup-local-db.sh
```

Optional environment variables:
- `RMS_DB_HOST`: default `127.0.0.1`
- `RMS_DB_PORT`: default `3306`
- `RMS_DB_USER`: default `root`
- `RMS_DB_PASSWORD`: passed to `mysql` through `MYSQL_PWD`, never on the command line
- `RMS_DB_ALLOW_REMOTE=true`: needed for any host other than the loopback address

Run it again at any time to reset the database to the seed state.

## Option B: Docker (no MySQL install)
Set `RMS_DB_PASSWORD` in your shell first. Pick your own local value; don't commit it. Then, from `db/local/`:
```bash
docker compose up -d
```
MySQL runs on `127.0.0.1:3306` (or `RMS_DB_PORT`) with the database `rms_local` already loaded. The load happens on the first start only. To reset it, run `docker compose down -v` and then `docker compose up -d` again.

## Expected result
Row counts after loading: `users` 2, `admin` 2, `position` 3, `language` 3, `candidate` 2 and `marks` 2.

## Using it with the app
- **Seed logins:** `test.admin` (the admin role) and `test.interviewer` (`isinterviewer = 'Y'`). Their throwaway passwords are in `../test-seed.sql`. They are also the values to use for `RMS_SMOKE_USER` and `RMS_SMOKE_PASSWORD` in `SmokeTest`.
- **App connection:** in your **local** servlet container (for example Tomcat 9), point the JNDI DataSource `jdbc/springrms` at `rms_local` on your local server, with driver `com.mysql.cj.jdbc.Driver`. Keep that container config out of the repo ([CLAUDE.md](../../CLAUDE.md) rule), and see [docs/build-run.md](../../docs/build-run.md).
