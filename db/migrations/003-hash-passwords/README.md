# Migration 003: hash the legacy passwords

`HashPasswords.java` replaces every plain-text password in `users.password` with a `{bcrypt}` hash (G13).

**Status:** written and compile-checked on 2026-10-02, but **not run anywhere**. It changes every user's stored password, so it needs the owner's approval first ([docs/open-questions.md](../../../docs/open-questions.md) #25). Claude and automation never run it against a shared or production database.

## Why a Java program, not SQL
MySQL has no bcrypt function, so SQL alone can't produce a hash that RMS checks. The program uses the same `BCryptPasswordEncoder` as RMS (`SecurityConfig.passwordEncoder`). It isn't part of the WAR; it runs once, by hand, with the WAR's libraries.

## What it does
- **Default (dry run):** it reads `users` and prints only counts: rows to hash, and rows left alone. It never prints userids, usernames, passwords or the connection settings.
- **With `--apply`:** it hashes the plain-text rows in one transaction. A row changed by someone else while it runs is left alone.
- **Left alone:**
  - rows starting with `{`: already `{bcrypt}`, or a plain-text password that starts with a brace (#25);
  - empty or NULL rows: these can't log in today either;
  - passwords over 72 bytes: bcrypt reads only 72 bytes.
- **Not changed:** usernames, `users.mustchangepassword`, the `admin` table. Users keep their passwords; only the stored form changes.
- **Stops first** if `users.password` is shorter than 68 characters (see `../002-user-roles.sql`).

## Before you run it
1. **Deploy a WAR that checks `{bcrypt}` first.** That means `dev` from the Spring Security commit (2026-10-01) or later. The WARs on `master` and `release/phase1` compare passwords in SQL and can't read hashes. After this migration, **nobody can sign in on those WARs.**
2. **Take a backup of the `users` table** (for example `mysqldump <database> users`). It holds the plain-text passwords, so keep it as securely as the database itself and delete it once the migration is confirmed.
3. **Check that `users` is InnoDB** (`SHOW TABLE STATUS LIKE 'users'`, column `Engine`). The "one transaction" holds only on InnoDB; on MyISAM a failure halfway leaves some rows hashed and some not (each hashed row still works with the new WAR).
4. **Answer the #25 checks**: case-sensitive passwords, duplicate usernames, empty passwords, and passwords starting with `{`.

## How to run it
From the repo root, after `./mvnw -B verify` built `target/rmsv2-1.0.1-SNAPSHOT/`. Set the three environment variables in your own shell, without writing them into a file:
- `RMS_DB_URL`, for example `jdbc:mysql://<host>:3306/<database>`
- `RMS_DB_USER`
- `RMS_DB_PASSWORD`

The account needs only `SELECT` and `UPDATE` on `users`, and `SELECT` on `information_schema`.

Dry run, which changes nothing (bash; on Windows use `;` instead of `:` in the class path):
```bash
java -cp "target/rmsv2-1.0.1-SNAPSHOT/WEB-INF/lib/*" db/migrations/003-hash-passwords/HashPasswords.java
```

Apply, after checking the dry run's counts:
```bash
java -cp "target/rmsv2-1.0.1-SNAPSHOT/WEB-INF/lib/*" db/migrations/003-hash-passwords/HashPasswords.java --apply
```

## Check afterwards
```sql
SELECT COUNT(*) FROM users WHERE password NOT LIKE '{bcrypt}%';   -- only the rows it left alone
SELECT LENGTH(password), COUNT(*) FROM users GROUP BY LENGTH(password);   -- the hashed rows are 68
```
Then sign in once with a known test account.

## Rollback
A hash can't be turned back into the password. To roll back, restore `users.password` from the backup taken before, for example by loading the dump into a scratch table and copying the column back by `userid`. Passwords changed or reset after the migration are lost by a rollback, and those users must have their password reset again.
