# Data Model

Purpose: The database tables and columns RMS uses, as seen in the SQL in the code.
Last updated: 2026-09-25
Read this when: you're changing SQL, adding a column or table, or fixing a data-mapping bug.

- **Database:** MySQL (`pom.xml`, Connector/J 5.1.36).
- **Access:** only through `NamedParameterJdbcTemplate` with named parameters (`rms/dao/*DaoImpl.java`).
- **Not in the repo:** schema/DDL files, migration scripts, and stored procedures. No SQL calls a procedure.
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
- `MarksMapper` reads result columns by position (1–5 and 8–18), so the query's column order matters (`MarksDaoImpl`).
