# Data Model

Purpose: The database tables and columns RMS uses, as seen in the SQL in the code.
Last updated: 2026-09-26
Read this when: you're changing SQL, adding a column or table, or fixing a data-mapping bug.

- **Database:** MySQL (`pom.xml`, Connector/J 5.1.36).
- **Access:** only through `NamedParameterJdbcTemplate` with named parameters (`rms/dao/*DaoImpl.java`).
- **Schema file:** `db/schema.sql` is **inferred** from the DAO SQL (Phase 0, 2026-09-26), not exported from production. Names come from the SQL; types, lengths, keys and NULL rules are guesses. It exists for the Testcontainers DAO tests; `db/test-seed.sql` is synthetic test data. Replace it with a DDL-only export when the owner supplies one (open question #19).
- **Not in the repo:** the production DDL, migration scripts, and stored procedures. No SQL calls a procedure.
- **Column lists:** these are only what the SQL and `RowMapper`s reference, not the full table definitions.

| Table | Columns referenced | Used by | Evidence |
|---|---|---|---|
| `users` | `userid`, `username`, `password` | Login check | `LoginDaoImpl` (`listallusers`) |
| `admin` | `userid`, `isactive`, `username`, `firstname`, `lastname`, `email`, `phone`, `designation`, `isinterviewer` (`Y`/`N`) | User profile, interviewer name in the marks query | `LoginDaoImpl.UserMapper`, `MarksDaoImpl` |
| `position` | `positionkey`, `positionname`, `isactive` | Position master | `PositionDaoImpl` |
| `language` | `languagekey`, `languagename`, `isactive` | Language master | `LanguageDaoImpl` |
| `candidate` | `candidateid`, `firstname`, `lastname`, `positionkey`, `languagekey`, `candidatestatus` | Candidate results (read-only) | `MarksDaoImpl` (`getallmarksbyadmin`) |
| `marks` | `candidateid`, `interviewerid`, plus score columns read by position (`m.*`) | Candidate results (read-only) | `MarksDaoImpl.MarksMapper` |

## Key entities (`rms/model`)
| Model | Fields | Evidence |
|---|---|---|
| `UserInfo` | `userid`, `username`, `password`, `firstname`, `lastname`, `email`, `phone`, `designation`, `isinterviewer`, `isactive` | `UserInfo.java` |
| `PositionInfo` | `positionkey`, `positionname`, `isactive` | `PositionInfo.java` |
| `LanguageInfo` | `languagekey`, `languagename`, `isactive` | `LanguageInfo.java` |
| `MarksInfo` | `interviewerid`, `candidateid`, `position`, `language`, `isactive`, the 10 scores (`workexp`, `techknowledge`, `leadership`, `decision`, `probsolving`, `stress`, `education`, `comskill`, `attitude`, `personality`), `candidatestatus` | `MarksInfo.java` |

## Data rules in code
- Position and language names are stored in uppercase and trimmed. A new name that already exists is silently skipped (`PositionDaoImpl.addPosition`, `LanguageDaoImpl.addLanguage`).
- New position and language rows get `isActive = 1`. Lists show only rows with `isactive = 1`. Deletes remove the row instead of setting `isactive = 0` (`*DaoImpl`).
- The relationships `candidate.positionkey` → `position` and `candidate.languagekey` → `language` are joins in `MarksDaoImpl`. Whether real foreign keys exist is unknown.
- `MarksMapper` reads result columns by position (1–5 and 8–18), so the query's column order matters (`MarksDaoImpl`). For the mapping to work, `marks` must have 13 columns, with `isactive` first and the 10 scores in columns 4–13 (`workexp` … `personality`); columns 2–3 aren't read. `db/schema.sql` follows this, and `MarksDaoImplTest` pins it.
- **The SQL is MySQL-specific:** it uses the 3-argument `concat()` and comma-style joins (`MarksDaoImpl.java:19-25`).
- **No date or time columns:** the row mappers read columns only with `getInt`/`getString` (`LoginDaoImpl.java:33-41`, `MarksDaoImpl.java:40-55`, `PositionDaoImpl.java:40-41`, `LanguageDaoImpl.java:42-43`).