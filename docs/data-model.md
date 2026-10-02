# Data Model

Purpose: The database tables and columns RMS uses, as seen in the SQL in the code.
Last updated: 2026-10-02 (generated candidate IDs). 2026-10-02 (candidate is now written: `CandidateDaoImpl`, `CandidateInfo`, G1). 2026-10-01 (Spring Security 7 replaces AuthInterceptor). 2026-10-01 (small fixes batch: duplicates among active rows, soft delete and reactivation, active-only lookups, unused model fields removed). 2026-09-26
Read this when: you're changing SQL, adding a column or table, or fixing a data-mapping bug.

- **Database:** MySQL (`pom.xml`, Connector/J 8.2.0 since Phase 1; 5.1.36 before).
- **Access:** only through `NamedParameterJdbcTemplate` with named parameters (`rms/dao/*DaoImpl.java`).
- **Schema file:** `db/schema.sql` is **inferred** from the DAO SQL (Phase 0, 2026-09-26), not exported from production. Names come from the SQL; types, lengths, keys and NULL rules are guesses. It exists for the Testcontainers DAO tests; `db/test-seed.sql` is synthetic test data. Replace it with a DDL-only export when the owner supplies one (open question #19).
- **Not in the repo:** the production DDL, migration scripts, and stored procedures. No SQL calls a procedure.
- **Column lists:** these are only what the SQL and `RowMapper`s reference, not the full table definitions.

| Table | Columns referenced | Used by | Evidence |
|---|---|---|---|
| `users` | `userid`, `username`, `password` | Login: read by username; Spring Security checks the password in Java (since 2026-10-01; before, the password was compared in SQL) | `LoginDaoImpl` (`loginsbyusername`) |
| `admin` | `userid`, `isactive`, `username`, `firstname`, `lastname`, `email`, `phone`, `designation`, `isinterviewer` (`Y`/`N`) | User profile, interviewer name in the marks query | `LoginDaoImpl.UserMapper`, `MarksDaoImpl` |
| `position` | `positionkey`, `positionname`, `isactive` | Position master | `PositionDaoImpl` |
| `language` | `languagekey`, `languagename`, `isactive` | Language master | `LanguageDaoImpl` |
| `candidate` | `candidateid`, `firstname`, `lastname`, `positionkey`, `languagekey`, `candidatestatus` | Candidate management (add, list, edit; `candidatestatus` only read, G9), candidate results | `CandidateDaoImpl`, `MarksDaoImpl` (`getallmarksbyadmin`) |
| `marks` | `candidateid`, `interviewerid`, plus score columns read by position (`m.*`) | Candidate results (read-only) | `MarksDaoImpl.MarksMapper` |

## Key entities (`rms/model`)
| Model | Fields | Evidence |
|---|---|---|
| `UserInfo` (`Serializable`, kept in the session) | `userid`, `username`, `firstname`, `lastname`, `email`, `phone`, `designation`, `isinterviewer`, `isactive` (set; only `toString` reads it; open question #20). `password` was removed (G21) | `UserInfo.java:6-18` |
| `PositionInfo` | `positionkey`, `positionname`. `isactive` was removed (G21): the lookups filter on it in SQL | `PositionInfo.java` |
| `LanguageInfo` | `languagekey`, `languagename`. `isactive` was removed (G21) | `LanguageInfo.java` |
| `CandidateInfo` | `candidateid`, `firstname`, `lastname`, `positionkey`, `languagekey`, `candidatestatus` (read only), plus `positionname` and `languagename` from the joins for display | `CandidateInfo.java` |
| `MarksInfo` | `interviewerid`, `candidateid`, `position`, `language`, `isactive` (set, never read; open question #20), the 10 scores (`workexp`, `techknowledge`, `leadership`, `decision`, `probsolving`, `stress`, `education`, `comskill`, `attitude`, `personality`), `candidatestatus` | `MarksInfo.java` |

## Data rules in code
- Position and language names are stored in uppercase and trimmed (`PositionDaoImpl.java:60-61,78-79`, `LanguageDaoImpl.java:70-71,104-105`).
- **Candidates** (`CandidateDaoImpl`, since 2026-10-02): RMS generates the ID as `C` plus the next number after the highest `C<number>` ID (up to 9 digits; other formats are ignored), and retries up to 3 times on a duplicate key. The names are trimmed and stored as typed, not upper-cased. The list sorts by ID length, then text, so C2 comes before C10. Add and edit never write `candidatestatus`, so a new candidate has it NULL (G9). There is no delete (open question #26).
- **Duplicates:** add and update refuse a name used by another **active** row. The check is `select count(*) … where name=… and isactive=1` (and `key<>…` on update): `PositionDaoImpl.java:25-26`, `LanguageDaoImpl.java:27-28`. Existing duplicate rows no longer make the check throw (G33). Names are compared with `=`, so the collation decides about accents and case (open question #22). There's no unique index in the inferred schema (G22, G33).
- **Active flag:** new rows get `isActive = 1`. Lists and `findById` return only rows with `isactive = 1` (`PositionDaoImpl.java:30,32`, `LanguageDaoImpl.java:30-31`), and so does the update (`and isactive=1`, `PositionDaoImpl.java:29`, `LanguageDaoImpl.java:32`). A row whose `isactive` is NULL is neither listed nor reactivated (open question #23).
- **Soft delete:** delete sets `isactive = 0` and keeps the row (`PositionDaoImpl.java:31`, `LanguageDaoImpl.java:33`; owner decision 2026-10-01, G17). The Candidate Status query joins candidates to `position` and `language` without an `isactive` filter, so deleted ones still show (`MarksDaoImpl.java:22-24`, `MarksDaoImplTest.deletedPositionAndLanguageStillShowWithTheirCandidates`).
- **Reactivation:** add first tries `update … set isactive=1, name=:name where name=:name and isactive=0 order by key limit 1` and inserts only if no row changed (`PositionDaoImpl.java:27,85-89`, `LanguageDaoImpl.java:29,77-81`). It stores the typed (upper-cased) name, which matters under a case- or accent-insensitive collation. The old row keeps its key, so candidates that pointed at it point at the re-added name.
- **Rename onto a deleted name** is allowed: the update check counts active rows only. That can leave a deleted and an active row with the same name (open question #24).
- **`UPDATE … ORDER BY … LIMIT` is MySQL-specific** (reactivation SQL above), like the 3-argument `concat()` below.
- The relationships `candidate.positionkey` → `position` and `candidate.languagekey` → `language` are joins in `MarksDaoImpl` and left joins in `CandidateDaoImpl` (so its list keeps a candidate whose position or language is missing). Whether real foreign keys exist is unknown.
- `MarksMapper` reads result columns by position (1–5 and 8–18), so the query's column order matters (`MarksDaoImpl`). For the mapping to work, `marks` must have 13 columns, with `isactive` first and the 10 scores in columns 4–13 (`workexp` … `personality`); columns 2–3 aren't read. `db/schema.sql` follows this, and `MarksDaoImplTest` pins it.
- **The SQL is MySQL-specific:** it uses the 3-argument `concat()` and comma-style joins (`MarksDaoImpl.java:19-25`).
- **No date or time columns:** the row mappers read columns only with `getInt`/`getString` (`LoginDaoImpl.java:33-47,50-54`, `MarksDaoImpl.java:40-55`, `PositionDaoImpl.java:47-48`, `LanguageDaoImpl.java:49-50`).
