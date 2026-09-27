# Acceptance Results

Purpose: Results of the acceptance checklist ([modernization-plan.md](../modernization-plan.md), section 6) and the browser check per upgrade phase, with the Phase 0 baseline screenshots that later phases are compared against.
Last updated: 2026-09-27
Read this when: you're signing off a phase, re-running the checklist after an upgrade, or comparing screens with the baseline.

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
| 2 | **Position:** create, duplicate, edit, delete, search, sort | Pass | Pass | "  data analyst " is stored as `DATA ANALYST`. A duplicate is silently skipped: still one row, no message. Edit pre-fills the form, delete removes the row, and DataTables search filters while the name column sorts both ways |
| 3 | **Language:** the same steps | Pass | Pass | As for Position (`rust` → `kotlin`) |
| 4 | **Candidate Status:** names, position, language, 10 scores, total, label | Pass | Pass | Carla Candidate: total 79, Selected (`happy.jpg`). Cody Candidate: total 33, Rejected (`sad.jpg`). Each total is the sum of the 10 scores. The page is pixel-identical between phases (`phase0/09-results-full-width.png`). In the menu view, *Total Score* and *Status* need horizontal scrolling (G40) |
| 5 | **Non-Latin characters** | Fails, same as Phase 0 | Fails, same as Phase 0 | Latin-1 works: "café señor" → `CAFÉ SEÑOR`. Characters outside ISO-8859-1 are stored as HTML entities: "инженер" becomes `&#1080;&#1085;…`, 49 bytes. They only *look* right because the list prints them unescaped (G27), and they aren't uppercased. Same bytes in both phases, so the driver change didn't alter it (G34) |
| 6 | **Unfinished menu items** behave as before | Pass | Pass | All 10 (*Create*/*View* > Candidate, Interviewer, Interview Schedule, Job; *Evaluation*, *Show Evaluation*) return 404, so the embedded area stays blank. Identical in both phases |

**Browser check (G31):**
- **Pages:** every page loaded with no failed requests.
- **Visual comparison:** 21 of the 22 checklist screenshots, and the full-width results page, are pixel-identical between phases. The exception is the non-Latin list, where DataTables 1.13 sets column widths slightly differently.
- **Errors:** SRI raised no errors in Phase 1. The only JavaScript error is on the login page in both phases, because Bootstrap 4 JS loads before jQuery. Phase 0 reports it as "Cannot read properties of undefined (reading 'fn')", Phase 1 as "Bootstrap's JavaScript requires jQuery". Login still works.
- **Bootstrap JS:** on the list and results pages, loading jQuery a second time drops the Bootstrap plugins. No page uses them, so nothing visible breaks.

**Baseline screenshots** (Phase 0, which Phase 1 matches) in `phase0/`:
- **Login:** `01-login`, `02-login-invalid`.
- **Menus:** `03-admin-menu`, `04-interviewer-menu`.
- **Masters:** `05-position-list`, `06-position-search`, `07-language-list`.
- **Candidate Status:** `08-results-in-menu` and `09-results-full-width`.
- **Unfinished item:** `10-unfinished-item` (a blank area).

**Not done:** checks by a domain user, and browsers other than Chromium.
