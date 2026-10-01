# Phase 1 Release Runbook

Purpose: How to release Phase 1 (Spring 5.3.39 on Java 8) from `dev` to production: what ships, what users notice, the owner checks, the ops steps, the checks after deploy and the rollback. It includes the rehearsal that proved those steps.
Last updated: 2026-10-01 (written and rehearsed on Tomcat 9 with MySQL 5.7)
Read this when: you're preparing, doing or signing off the Phase 1 release, or rolling it back.

No credentials, hostnames or JNDI definitions belong in this file. Each environment's values stay in its container.

## What ships
Compared with `master` (`e5705c0`, the 2020 code), `dev` adds the following. Details are in [upgrade-status.md](upgrade-status.md) and [gaps.md](gaps.md).
- **Stack (G24, Phase 1):**
  - Spring 5.3.39 (was 4.3.0).
  - MySQL Connector/J 8.2.0 (was 5.1.36).
  - SLF4J 2.0 with Logback 1.3.
  - jQuery 3.7.1, Bootstrap 3.4.1 and 4.6.2, and DataTables 1.13.11, loaded from CDNs with SRI hashes.
  - It still needs Java 8 and a Servlet 3.1 container.
- **Build:** Java 1.8 class files and pinned plugins (G35). `target/` is no longer committed (G29).
- **Fixes:**
  - G11: pages need a login, and admin pages need the admin role. G12 was fixed with it.
  - G27 and G38: user data is shown as text.
  - G40: the results table fits its area.
- **No database change:** no table, column or data changes.
  - No SQL string in the DAOs changed (`git diff master..dev -- src/main/java/rms/dao`). The only DAO change is `System.out` → `log.warn`.
  - The `db/` scripts are for local and test databases only.

## What users will notice
- Every page needs a login. Opening a page while logged out goes to the login page.
- An interviewer who opens an admin page by its URL gets an error page (HTTP 403). Their menu is unchanged, and its two items were already empty (G5).
- Names show exactly as typed. Text that looks like HTML is no longer interpreted.
- Some names saved earlier through the forms now show as codes such as `&#1080;…`: Cyrillic, Bengali, and Latin letters such as ł or č (G34, [open-questions.md](open-questions.md) #21).
- The Candidate Status table fits its area from about a 1235 px wide window (G40).
- After a session expires, the login form shows inside the menu's content area. Logging in there reloads the whole window.

## Before the release (owner)
1. **MySQL version:** `SELECT VERSION();` must return 5.7 or later, because Connector/J 8.2.0 supports servers 5.7+ (#16). 5.7.44 was rehearsed below, and 8.0.46 is what the tests use.
2. **Schema:** confirm that the production DDL matches `db/schema.sql`, or accept the risk (#19). The release itself doesn't change the schema.
3. **Stored names:** run the read-only queries in #21 and note any rows. They'll show as codes after the release.
4. **Log content:** a refused admin page logs the user ID at WARN (#20). Is that acceptable?
5. **CDN access:** users' browsers now load libraries from `cdn.jsdelivr.net`, `code.jquery.com`, `cdn.datatables.net` and `cdnjs.cloudflare.com`. `maxcdn.bootstrapcdn.com` and `ajax.googleapis.com` are no longer used (`git grep` on both branches). If a proxy or firewall allows only listed sites, add `cdn.jsdelivr.net`.
6. **Sign-off:** name a domain user to run the acceptance checklist ([modernization-plan.md](modernization-plan.md) §6).
7. **Backup:** take a routine database backup before the window.
8. **Go:**
   - Merge the `dev` → `master` PR.
   - Build the WAR from `master` on JDK 8 with `./mvnw -B verify`. The output is `target/rmsv2-1.0.1-SNAPSHOT.war` ([build-run.md](build-run.md)).

## Ops steps (Tomcat)
These were rehearsed in this order. Keep everything you remove: it's the rollback.
1. **Record the current state:**
   - the Tomcat version (`bin/version.sh`);
   - the JDK version, which must stay 8 for this release (Phase 2 moves it);
   - the `lib/` listing;
   - the app's context path.
2. **Stop Tomcat.**
3. **Back up** the deployed WAR and the context file that defines `jdbc/springrms`.
4. **Clear out old jars:** move `mysql-connector-java-5.1.*.jar` and any `slf4j-*` or `logback-*` jars out of `lib/`, and keep them with the backup. An old driver left there keeps the pool on 5.1, and 5.1.36 can't connect to MySQL 8.0 at all ([build-run.md](build-run.md)).
5. **Add the new driver:** put `mysql-connector-j-8.2.0.jar` in `lib/`.
   - Download it from Maven Central, and check it against the `.sha1` published next to it.
   - In the rehearsal, a rate-limited download returned a short error page instead of the file. Only the checksum caught it.
6. **Update the `jdbc/springrms` resource:**
   - Set `driverClassName="com.mysql.cj.jdbc.Driver"`.
   - Add `sslMode=REQUIRED` (or `VERIFY_CA`) to the URL, if `SHOW VARIABLES LIKE 'have_ssl'` returns `YES`. Without TLS on the server, `REQUIRED` can't connect.
   - Don't add `allowPublicKeyRetrieval=true` without the owner's sign-off.
   - Leave everything else as it is.
7. **Deploy:** replace the WAR at the same context path, and clear Tomcat's `work/` directory for the app.
8. **Start Tomcat.** `logs/catalina.out` must show no SEVERE or ERROR lines.

## After deploy
1. **Quick check** (about 5 minutes, by an admin):
   - Log in as an admin: the menu, name, role and e-mail show.
   - *View > Position* and *View > Language* list the data, and *Candidate Status* shows the scores.
   - Log out, then open `/viewpositionlist` directly: you land on the login page.
   - Log in as an interviewer: the menu shows Evaluation, Show Evaluation and Logout.
2. **Optional:** run `SmokeTest` from a dev machine with `RMS_BASE_URL` and an account the owner designates ([build-run.md](build-run.md)).
   - Keep the account's credentials in environment variables only.
   - It only logs in and reads pages.
3. **Acceptance:** the domain user runs the checklist ([modernization-plan.md](modernization-plan.md) §6) and compares it with [acceptance/README.md](acceptance/README.md). Item 5 (non-Latin names) is known to fail (G34).
4. **After the merge to `master`:** note in [upgrade-status.md](upgrade-status.md) which GitHub alerts closed (#7).
5. **Update [upgrade-status.md](upgrade-status.md):** Phase 1 is in production. That unlocks Phase 2.

## Rollback
- **Quick:** stop Tomcat, put the backed-up WAR back, clear `work/`, and start.
  - The new driver and resource settings can stay, because the old app only uses the JNDI DataSource.
  - In the rehearsal, the old app on the new driver passed the whole checklist exactly like the Phase 0 baseline.
- **Full:** if the driver itself is suspected, also restore the backed-up jars and context file. That returns to the state recorded in step 1.
- **Database:** no rollback is needed, because the release changes no schema or data.

## Rehearsal (2026-10-01)
This ran in a scratch environment, with a throwaway database. Nothing real was touched.
- **Starting point, like production today:**
  - Tomcat 9.0.122 on JDK 8u504, with Connector/J 5.1.36 in `lib/` and `com.mysql.jdbc.Driver`.
  - MySQL 5.7.44, loaded from `db/schema.sql` and `db/test-seed.sql`.
  - The Phase 0 WAR, built from `d813e71`: the 2020 app code with build fixes.
  - Login, the lists and Candidate Status worked.
- **Release:** the ops steps above, in order, with the `dev` WAR. Results:
  - `SmokeTest` passed 3 of 3.
  - The full acceptance checklist was identical to the latest run on MySQL 8.0: all 52 checks, and all 22 screenshots byte-identical. Only item 5 fails (G34). This was Phase 1's first run on MySQL 5.7.
  - The 15 access checks (G11) and the 20 escaping checks (G27/G38) passed.
  - The app's database connection used TLSv1.2.
  - The logs had no SEVERE or ERROR lines.
- **Quick rollback:** the Phase 0 WAR on the new driver, with a fresh database.
  - All 52 checks gave the same results and details as the Phase 0 baseline of 2026-09-27.
  - 21 of 22 screenshots were byte-identical. The non-Latin list shows the same rows, with the same stored bytes, but with different DataTables column widths.
- **Not covered:** the real container, its JDK and Tomcat versions, the real MySQL server and its TLS setup, real CDN access, and Windows browsers.
