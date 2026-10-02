# Local RMS database

Everything needed to build a local MySQL database for RMS on any PC: the database, the tables (DDL) and the seed data (DML).

## What you get, and its limits
- **Database:** `rms_local` (utf8mb4), with the ten tables `users`, `admin`, `position`, `language`, `candidate`, `marks`, `job`, `candidate_document`, `candidate_interviewer` and `candidate_decision`.
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
Row counts after loading: `users` 5, `admin` 5, `position` 3, `language` 3, `candidate` 2, `marks` 4, `job` 3, `candidate_document` 4, `candidate_interviewer` 4 and `candidate_decision` 3. The document rows are metadata only: there are no files, so downloading them answers 404. For uploads, set `RMS_DOC_DIR` for your local Tomcat to an empty scratch folder ([docs/build-run.md](../../docs/build-run.md), "Document storage").

## Databases made before a schema change
`../schema.sql` always has the current columns, so a fresh setup needs nothing else. A database built earlier needs the numbered scripts in `../migrations/` that came after it, in order, for example:

```bash
mysql -h 127.0.0.1 -u root -p rms_local < db/migrations/001-candidate-isactive.sql
```

| Script | Since | Needed for |
|---|---|---|
| `001-candidate-isactive.sql` | 2026-10-02 | the Candidates pages (soft delete adds `candidate.isactive`) |
| `002-user-roles.sql` | 2026-10-02 | every login (roles add `admin.role` and `users.mustchangepassword`) |
| `004-candidate-documents-jobs.sql` | 2026-10-02 | the Candidates, Jobs and document pages (candidate profile fields, `job`, `candidate_document`). `003-hash-passwords/` is optional: a Java program that hashes the legacy plain-text passwords, see its README |
| `005-assignment-evaluation-decision.sql` | 2026-10-02 | Candidate Status, evaluations, decisions and the profile (new `marks` columns at the end, decision columns on `candidate`, `candidate_interviewer`, `candidate_decision`). A draft for production; fine for a local database built before Phase 3 |

Re-running the setup script instead also works, but it wipes `rms_local` and reloads the seed.

## Using it with the app
- **Seed logins**, one per role: `test.admin` (Super Admin), `test.hr` (HR), `test.manager` (Hiring Manager), `test.interviewer` (Interviewer), plus `test.former` (an inactive Interviewer, who can't sign in). Their throwaway passwords are in `../test-seed.sql`; `test.admin` and `test.interviewer` are plain-text rows, the others `{bcrypt}`. Use `test.admin` for `RMS_SMOKE_USER` and `RMS_SMOKE_PASSWORD` in `SmokeTest` (the Users and Roles check needs a Super Admin).
- **App connection:** in your **local** servlet container (Tomcat 11 on JDK 21 for the current code; Tomcat 9 for Phase 1 builds), point the JNDI DataSource `jdbc/springrms` at `rms_local` on your local server, with driver `com.mysql.cj.jdbc.Driver`. Keep that container config out of the repo ([CLAUDE.md](../../CLAUDE.md) rule), and see [docs/build-run.md](../../docs/build-run.md).
