# Acceptance Results

Purpose: Results of the acceptance checklist ([modernization-plan.md](../modernization-plan.md), section 6) and the browser check per upgrade phase, with the Phase 0 baseline screenshots that later phases are compared against.
Last updated: 2026-10-03 (reports-activity.md added). 2026-10-02 (roles plan Phase 3: its checks are in [roles-phase3.md](roles-phase3.md), not run yet). 2026-10-02 (roles plan Phase 2: its checks are in [roles-phase2.md](roles-phase2.md), not run yet). 2026-10-02 (roles plan Phase 1: its checks are in [roles-phase1.md](roles-phase1.md), not run yet). 2026-10-02 (candidate delete check added). 2026-10-02 (checks for candidate management, G1; not run yet). 2026-10-01 (UI redesign noted: no run covers it yet). 2026-10-01 (small fixes batch: results, intended checklist differences, and notes on behaviour that changed since the older runs). 2026-10-01 (Phase 3 on Tomcat 11, rerun after the code review; merged into `dev`). 2026-09-30
Read this when: you're signing off a phase, re-running the checklist after an upgrade, or comparing screens with the baseline.

**Roles plan (2026-10-02 onward):** each phase has its own file with the manual SQL checklist and browser steps: [roles-phase1.md](roles-phase1.md) (roles, Users and Roles, passwords; not run yet, DB-verified NO) , [roles-phase2.md](roles-phase2.md) (candidate profile, documents, jobs) and [roles-phase3.md](roles-phase3.md) (assignment, evaluation, decision; migration 005 a draft); none run yet, DB-verified NO.

The sections are chronological and keep their results as history. Some behaviour they describe changed on 2026-10-01 (silent duplicate skip, GET delete links, `GET /` showing `index.jsp`, the login-page JavaScript error). The last section, *Small fixes batch*, lists what a checklist run should expect now. None of the runs below covers the 2026-10-01 UI redesign (`bb569c4`, `f826830`, `0aaf836`): their screenshots and page details (the `<object>` menu, Bootstrap 3/4, the status images) show the old UI. The redesign still needs its own run ([upgrade-status.md](../upgrade-status.md)). They also predate Spring Security 7 (2026-10-01): their access checks ran against the interim `AuthInterceptor`, and their logins posted no CSRF token.

## Run of 2026-09-27: Phase 0 baseline vs Phase 1
**What ran:**
- **Phase 0:** commit `d813e71`, the last one before Phase 1 (Spring 4.3.0, Connector/J 5.1.36, the old CDN versions), on MySQL 5.7.44 with the `com.mysql.jdbc.Driver` JNDI pool.
- **Phase 1:** `dev` at `a7a1369` (Spring 5.3.39, Connector/J 8.2.0, jQuery 3.7.1, DataTables 1.13.11), on MySQL 8.0.46 with `com.mysql.cj.jdbc.Driver` and `sslMode=REQUIRED`.

**Common setup:**
- Tomcat 9.0.122 on JDK 8u504.
- Databases loaded from `db/schema.sql` and `db/test-seed.sql`, with utf8mb4 as in `db/local`.
- Logins `test.admin` and `test.interviewer`.
- A scripted Chromium (Playwright 1.56.1) at 1366×800 clicked through the menu like a user. Menu pages load into the `<object>` in `main.jsp`.

**Limits:**
- **CDN files:** the CDN hosts weren't reachable from the machine, so each CDN request was answered with the same file from the library's npm package. For Phase 1 those bytes match every SRI hash ([tech-stack.md](../tech-stack.md)), so the browser's integrity checks really ran. CDN availability and CORS headers weren't tested.
- **Browsers:** only Chromium on Linux. Fonts differ from a Windows desktop.

| # | Checklist item | Phase 0 | Phase 1 | Notes |
|---|---|---|---|---|
| 1 | **Login:** admin and interviewer menus, name, role and e-mail; wrong password; logout | Pass | Pass | Admin menu: *Candidate Status*, *Create*, *View*. Interviewer menu: *Evaluation*, *Show Evaluation*. The header shows the seed profile. A wrong password shows "Invalid login!". *Logout* returns to the login form |
| 2 | **Position:** create, duplicate, edit, delete, search, sort | Pass | Pass | "  data analyst " is stored as `DATA ANALYST`. A duplicate is silently skipped: still one row, no message (before G14, fixed 2026-10-01). Edit pre-fills the form, delete removes the row (a GET link then; since G17 a POST button that soft-deletes it), and DataTables search filters while the name column sorts both ways |
| 3 | **Language:** the same steps | Pass | Pass | As for Position (`rust` → `kotlin`) |
| 4 | **Candidate Status:** names, position, language, 10 scores, total, label | Pass | Pass | Carla Candidate: total 79, Selected (`happy.jpg`). Cody Candidate: total 33, Rejected (`sad.jpg`). Each total is the sum of the 10 scores. The page is pixel-identical between phases (`phase0/09-results-full-width.png`). In this run, the menu view needed horizontal scrolling to reach *Total Score* and *Status* (G40, fixed 2026-09-30; see below) |
| 5 | **Non-Latin characters** | Fails, same as Phase 0 | Fails, same as Phase 0 | Latin-1 works: "café señor" → `CAFÉ SEÑOR`. Characters outside ISO-8859-1 are stored as HTML entities: "инженер" becomes `&#1080;&#1085;…`, 49 bytes. They only *look* right because the list prints them unescaped (G27), and they aren't uppercased. Same bytes in both phases, so the driver change didn't alter it (G34) |
| 6 | **Unfinished menu items** behave as before | Pass | Pass | All 10 (*Create*/*View* > Candidate, Interviewer, Interview Schedule, Job; *Evaluation*, *Show Evaluation*) return 404, so the embedded area stays blank. Identical in both phases |

**Browser check (G31):**
- **Pages:** every page loaded with no failed requests.
- **Visual comparison:** 21 of the 22 checklist screenshots, and the full-width results page, are pixel-identical between phases. The exception is the non-Latin list, where DataTables 1.13 sets column widths slightly differently.
- **Errors:** SRI raised no errors in Phase 1. The only JavaScript error is on the login page in both phases, because Bootstrap 4 JS loads before jQuery. Phase 0 reports it as "Cannot read properties of undefined (reading 'fn')", Phase 1 as "Bootstrap's JavaScript requires jQuery". Login still works. (Fixed 2026-10-01, G31.)
- **Bootstrap JS:** on the list and results pages, loading jQuery a second time drops the Bootstrap plugins. No page uses them, so nothing visible breaks. (Fixed 2026-10-01, G31.)

**Baseline screenshots** (Phase 0, which Phase 1 matches) in `phase0/`:
- **Login:** `01-login`, `02-login-invalid`.
- **Menus:** `03-admin-menu`, `04-interviewer-menu`.
- **Masters:** `05-position-list`, `06-position-search`, `07-language-list`.
- **Candidate Status:** `08-results-in-menu` and `09-results-full-width`.
- **Unfinished item:** `10-unfinished-item` (a blank area).

**Not done:** checks by a domain user, and browsers other than Chromium.

## G40 fix (2026-09-30): Candidate Status fits the menu area
- **Change:** `viewmarks.jsp` now links `resources/css/viewmarks.css`.
  - Text is 12 px, side padding 4 px, and the table uses `table-condensed`.
  - Status icons are 40 px, and rows are 52 px instead of about 116.
  - DataTables' grid rows no longer stick out 15 px, which had also caused a horizontal scrollbar.
  - Columns, headers, data and the icons themselves are unchanged.
- **Measured** in Chromium from the menu, with the same setup as above on `dev` plus the fix:

  | Window | Available width | Table width | Horizontal scroll |
  |---|---|---|---|
  | 1024 px | 800 px | 1008 px (its minimum) | yes, still out of scope |
  | 1280 px | 1053 px | 1053 px | no |
  | 1366 px | 1139 px | 1139 px | no |
  | 1920 px | 1687 px | 1687 px | no |

  Before the fix, the minimum was 1327 px. It now fits from about a 1235 px window.
- **Re-run of the whole checklist:**
  - All 52 checks give the same results and details as the Phase 1 run above, including item 4's scores, totals and labels. Only item 5 fails (G34).
  - No new console errors.
  - Every screenshot except the Candidate Status page is pixel-identical to that run.
- **New reference:** `phase0/08-results-in-menu` and `09-results-full-width` no longer match pixel for pixel, by design. For later phases, compare the Candidate Status page with `after-g40/results-in-menu-1366.png` and `after-g40/results-in-menu-1280.png`.

## G11 fix (2026-09-30): pages need a login, admin pages need the admin role
- **Change:** `AuthInterceptor`, registered in `WebConfig.addInterceptors`, checks every page except `/login`, `/welcome` and `/resources/**`.
  - Logged out, it redirects to `/login`.
  - A user whose `isinterviewer` isn't `N` gets HTTP 403.
  - After the code review, the same change also made the login form post with `target="_top"` and fixed G12 (the logged-in user was shared across concurrent logins).
  - Details in [gaps.md](../gaps.md) G11 and G12.
- **Setup:** the same as the G40 run: Tomcat 9.0.122 on JDK 8, MySQL 8.0 from `db/local/docker-compose.yml` with the seed data, and Chromium with the npm CDN stand-in.
- **Re-run of the whole checklist,** on the final build and a freshly seeded database:
  - All 52 checks give the same results and details as the G40 run. Only item 5 fails (G34).
  - Console and HTTP errors are unchanged.
  - All 22 screenshots are byte-identical.
  - One earlier run had different DataTables column widths on `10-position-after-edit` with identical data. A rerun on a fresh seed was byte-identical again, so this was the harness's timing, not the change.
  - The unfinished menu items still end in 404: they have no handler, so the check never runs for them.
- **New access checks** (15, all pass):

  | Who | Request | Result |
  |---|---|---|
  | Logged out | `/viewpositionlist`, `/adminviewmarks`, `/deleteposition/1` | redirect to `/login`; in the browser the login page shows, and position 1 isn't deleted |
  | Logged out | `/login`, `/resources/css/login.css` | 200, and the login page is styled |
  | Logged out | `POST /saveposition` | redirect to `/login`, and no row written |
  | Admin | the admin pages, directly and from the menu | 200, as before |
  | Admin, after Logout | `/adminviewmarks` | redirect to `/login` |
  | Admin, session cookie gone | a menu item | the content area shows the login form (`after-g11/session-expired-in-menu.png`) |
  | Admin, session cookie gone | logging in from that form | the whole window loads the menu page (`target="_top"`), with nothing nested in the content area |
  | Interviewer | the interviewer menu | unchanged: Evaluation, Show Evaluation, Logout |
  | Interviewer | `/viewpositionlist`, `/viewlanguagelist`, `/createposition`, `/adminviewmarks`, `/deleteposition/1`, `POST /saveposition` | 403 each (`after-g11/interviewer-403.png`), and no row changed or deleted |

- **Also checked:**
  - `SmokeTest` passes 3 of 3, including the new logged-out redirect.
  - Tomcat logged no errors. Each refused request logged one WARN line with the servlet path, and no `jsessionid` appeared in the log.
  - `GET /` still shows `index.jsp` ("Hello World!", G25). Tomcat serves it directly, not through Spring, so there's nothing to protect there. (Changed 2026-10-01: `index.jsp` is deleted and `/` redirects to `/home`.)
- **Observations, not changed:**
  - The 403 is Tomcat's default error page, and it names the Tomcat version (`after-g11/interviewer-403.png`). Hiding that is container configuration (ops).

## G27/G38 fix (2026-09-30): user data is shown as text
- **Change:**
  - The position and language lists, the Candidate Status page and the menu header print names through `<c:out>`.
  - The interviewer menu no longer puts the userid into the score-entry URL.
  - Details in [gaps.md](../gaps.md) G27 and G38.
- **Setup:** the same as the G11 run.
- **Re-run of the whole checklist** on a freshly seeded database, compared with the final G11 run. Results and details are the same except:
  - **Item 5:** "инженер" and "প্রকৌশলী" now show as their stored codes (`&#1080;&#1085;…`, `&#2474;&#2509;…`).
    - The Bengali check now fails too. It passed before only because the unescaped codes rendered as letters, and upper-casing leaves Bengali unchanged.
    - Item 5 fails as before (G34), and "café señor" → "CAFÉ SEÑOR" still passes.
    - This was accepted as the cost of escaping. Open question #21 asks whether production has such names.
  - **Interviewer menu:** *Evaluation* and *Show Evaluation* load `/MarksController` and `/MarksController?param=VIEW` instead of `…?user=U2…`. Both still end in 404.
  - **Errors:** console errors are unchanged, and HTTP errors differ only in those two URLs.
  - **Screenshots:** 21 of the 22 are byte-identical. The exception is `19-non-latin`.
- **New escaping checks** (20):
  - **Payloads entered through the forms:** `<IMG SRC=X ONERROR=ALERT(1)>`, which stays valid after the DAO upper-cases it, and `R&D "Q" 'A'`.
  - **Payloads written straight into the throwaway database:**
    - `<img src=x onerror="window.__xss='…'">` as a position, a language, a candidate's first name and the interviewer's first name;
    - a `<script>` in the interviewer's e-mail;
    - `R&D <i>lead</i>` as the designation;
    - a NULL last name.
  - **Results:** all 20 checks fail on `dev` without the fix (after G11) and pass on the fix:

  | Page | Without the fix | With the fix |
  |---|---|---|
  | Position and language lists | the payload images render and their `onerror` scripts run | every name shows as its exact text, and no element is injected (`after-g27/position-list-escaped.png`) |
  | Edit forms | the payload row can't be found as text | the input shows the payload as text (Spring's `form:input` already escaped it) |
  | Candidate Status | the candidate, position and language payloads run | shown as text; the status icons are unchanged |
  | Interviewer header | the image and the `<script>` run, and the NULL last name prints "null" | shown as text; the NULL last name is blank |
  | Interviewer menu | `?user=U2` in both score-entry URLs | no userid; both still 404 |

  No dialog opened, and no payload caused a script error.
- **Also checked:** `SmokeTest` passes 3 of 3, and Tomcat logged no errors.
- **Observation, not changed:** long profile values run past the right edge of the header box. That's the header's fixed layout; escaped markup is just longer text.
- **Code review follow-ups** (same day, all Low):
  - **Changes:**
    - The menu's `spring:url` values that go into JavaScript strings are HTML- and JavaScript-escaped. The page source now has `\/rms\/viewlanguagelist…`, which JavaScript reads as `/rms/viewlanguagelist…`.
    - The login error prints through `<c:out>`, and still reads "Invalid login!".
    - `SmokeTest` checks that the menu page has no `?user=`.
  - **Results:**
    - `SmokeTest` passes 3 of 3 on the fix. Against the build without the fix, the new check fails.
    - The whole checklist on a fresh seed is identical to the G27 run: all 52 checks, and all 22 screenshots byte-identical, including the menu navigation through the escaped URLs.
    - The 20 escaping checks pass again.
  - **Crafted context path:** a login POST to `/rms;'-x-'/welcome` (and to `/rms;x/welcome`) gets 404. Spring logs "No mapping", so no page renders with such a context path on this stack.
  - **Encoding probe** (G34, open question #21): the stored bytes were checked in the database.
    - "łódź" and "čeština" store ł, ź and č as `&#322;`, `&#378;` and `&#269;`.
    - "š", "€", "–", curly quotes, "Š" and "œ" are stored as control characters U+0080–U+009F. The list sends them back as windows-1252 bytes, so they still look right.

## Phase 3 (2026-10-01): Spring 7, Jakarta, Tomcat 11, JDK 21
- **What ran:** the Phase 3 WAR from branch `claude/tech-stack-review-lq0a2k`, merged into `dev` the same day.
  - Spring 7.0.9 on Jakarta EE 11, Jakarta Tags 3.0, Java 21 bytecode.
  - Tomcat 11.0.26 on OpenJDK 21.0.10, with Connector/J 8.2.0 in `lib/` and `com.mysql.cj.jdbc.Driver` with `sslMode=REQUIRED`.
  - MySQL 8.0 from `db/local/docker-compose.yml`, freshly seeded before each run.
  - The same Chromium harness and CDN stand-in as before.
- **First run:** saving "café señor" failed with HTTP 400. Tomcat 11's default `conf/web.xml` decodes requests as UTF-8, and the ISO-8859-1 form's `é` byte is invalid UTF-8. With `request-character-encoding` set to ISO-8859-1 in the app's `web.xml`, everything below passed (G34, G37).
- **Whole checklist:** the same results and details as the latest Tomcat 9 run (`dev` after G27), and all 22 screenshots byte-identical. Only item 5 fails (G34), as before.
- **Also checked:**
  - `SmokeTest` passes 3 of 3.
  - The 15 access checks (G11) and the 20 escaping checks (G27/G38) pass.
  - Stored bytes for Latin-1 and windows-1252 test names match Tomcat 9 exactly.
  - Tomcat and the app logged no SEVERE or ERROR lines.
  - A login POST to the crafted context path `/rms;'-x-'/welcome` now gets 200 (Spring 5.3 returned 404), but the page contains no trace of the crafted path.
  - Stopping Tomcat wrote no session file and no warning about the non-`Serializable` `UserInfo` (G36).
- **Rerun after the code review (same day):** the review fixes changed `pom.xml`, `web.xml` (`metadata-complete="true"`) and the tests, so everything ran again on a fresh database.
  - `SmokeTest` passes 4 of 4, including the new ISO-8859-1 login post of "café".
  - The checklist again matches the Tomcat 9 run, with all 22 screenshots byte-identical.
  - The 15 access checks and 20 escaping checks pass, the stored bytes are unchanged, and the logs have no SEVERE or ERROR lines.
  - The same WAR with the `request-character-encoding` element removed failed the new smoke test with HTTP 400, so the test guards the pin.
- **Not covered:** MySQL 5.7 with Phase 3 (the driver is the same as in the Phase 1 rehearsal), the real container, and a release rehearsal with rollback.

## Small fixes batch (2026-10-01)
- **What ran:** the four commits of the batch on top of `dev` (branch `claude/tech-stack-review-lq0a2k`). The results below were reported by the batch author; they weren't re-run when this section was written.
  - Scratch Tomcat 11.0.26 on JDK 21, with the context copied from `deploy/tomcat/rms.xml` and the `db/local` MySQL 8.0 database.
  - `./mvnw -B verify` with Docker on JDK 21: 85 tests, 79 pass, 6 smoke skipped ([build-run.md](../build-run.md)).
- **Owner decisions the batch implements:** soft delete for Position and Language (open question #5), and sessions by cookie only (G41).

  | Check | Result |
  |---|---|
  | `SmokeTest` against the deployed WAR | 6/6 |
  | Acceptance checklist in Chromium | 56 checks (52 in the earlier runs); only the 2 known G34 failures (item 5, non-Latin names) |
  | Access checks (G11) | 15 pass |
  | Escaping checks (G27/G38) | 20 pass |
  | Candidate Status table (G40) | 1139 px in its 1139 px area, after the DataTables Bootstrap 3 switch |
  | Logs | no SEVERE or ERROR lines |
  | Redirects | logged out, `/rms` → `/rms/` → `/rms/login` (three 302s) |
  | Session | the id changes at login; no `jsessionid` on the login page, in URLs or in logs; the cookie is `JSESSIONID=…; Path=/rms; HttpOnly; SameSite=Lax` |

- **Intended differences from the earlier runs** (a checklist run should now expect these):

  | Item | Earlier runs | Now |
  |---|---|---|
  | 1 Login | the menu is the response to `POST /welcome`; `GET /` shows "Hello World!" | a good login answers 302 to `/home`, and the menu is `GET /home`, so a refresh doesn't re-post. `/` goes to `/home`, or to the login when logged out. A wrong password still renders the login page at `/welcome` with "Invalid login!". A user without an `admin` row gets the same message |
  | 2 Position | a duplicate is silently skipped; a blank name is saved; Delete is a link that removes the row | a duplicate (or an update onto another active row's name) shows the form again with the typed value and "Position NAME already exists."; a blank name shows "Please enter a position name."; Delete is a button (a POST form) that soft-deletes, so the row leaves the list; adding the deleted name again brings it back; opening or saving a deleted row's form gives 404 |
  | 3 Language | as Position | as Position, with "Language NAME already exists." and "Please enter a language name." |
  | 4 Candidate Status | names, scores, total, label | unchanged. Candidates of a deleted position or language still show (pinned by `MarksDaoImplTest`, not by a browser check) |
  | 5 Non-Latin | fails (G34) | unchanged (G34); the DataTables sort arrows are now ASCII-escaped in `resources/css/datatables.css` |
  | 6 Unfinished menu items | 404, blank area | unchanged |
  | Page errors | "Bootstrap's JavaScript requires jQuery" on the login page | gone on every page (G31); Bootstrap JS loads (4.6.2 on login, 3.4.1 on the list pages) |
  | List and results layout | the DataTables length and search controls wrap | they sit on one line (G31); the results table still fits its area (G40) |

- **Screenshots:** the login, list and create pages differ from the earlier screenshots by design (Delete button, DataTables layout, no page error). A pixel comparison for this batch wasn't reported, so the `phase0/` and `after-g40/` screenshots remain the references for Candidate Status only.
- **Not covered:** MySQL 5.7 with this batch, the real container, the production collation (open question #22) and a browser other than Chromium. The crafted-context-path login (G27) was reported to get a 302 to `/home`; it wasn't re-run for this section.

## Candidate management (G1, 2026-10-02): checks for the next run
Not run yet: this machine has no Tomcat or Docker ([upgrade-status.md](../upgrade-status.md)). The next checklist run, together with the UI redesign and Spring Security, should add:

| Check | Expected |
|---|---|
| Menu and home | Admins see *Candidates* under *Recruitment* and a Candidates tile; it is no longer under *Coming soon*. Interviewers don't see it, and `/viewcandidatelist` gives them 403 |
| List | C1 Carla Candidate (SOFTWARE ENGINEER, JAVA, Selected) and C2 Cody Candidate (QA ENGINEER, PYTHON, Rejected) from the seed; DataTables search and sort work |
| Add | The form has no ID field. "Dana", "Doe", a position and a language → back on the list as C3, Pending; the next one is C4. A blank name, or no position or language, shows the matching message with the typed values kept. Deleted positions and languages aren't offered |
| Edit | The ID is read-only; changing a name, position or language saves. Delete a position used by a candidate, then edit that candidate: the position shows as "NAME (deleted)" and saving keeps it |
| Older IDs | Insert test rows with IDs such as `A+B`, `X&Y`, `two words` and `<b>` straight into the local test database; each Edit link opens the right candidate, the list shows the ID as text, and a new candidate still gets the next C-number |
| Order | With C2 and C10 present, the list and the ID column sort C2 before C10 |
| Delete | Delete asks for confirmation, then C2 leaves the Candidates list; `/updatecandidate?candidateid=C2` gives 404; Candidate Status still shows Cody Candidate's scores; the next new candidate does not get C2. Before the run, a local database made before 2026-10-02 needs `db/migrations/001-candidate-isactive.sql` |
| Layout | The two dropdowns and the first and last name fields line up on desktop and stack on a phone width |
