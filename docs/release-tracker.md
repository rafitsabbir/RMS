# Release Tracker: `dev` to production

Purpose: the ordered list of what is left before and after releasing `dev`, with who does each item and its state. Work goes one item at a time, top to bottom. The feature freeze of 2026-10-20 was lifted by the owner on 2026-11-05: the features below (F1–F11) are built now, before the release.
Last updated: 2026-11-05 (freeze lifted: features F1–F11 built before the release, in the order below). 2026-11-05 (items 6 and 8 done: the owner's decisions; `RMS_SECURE_COOKIE` and migration 003 in the runbook). 2026-11-05 (recommended next steps added: items 6–8 made explicit, item 9 runs migration 003, items 11–16 after go-live in order). 2026-11-05 (item 4 done). 2026-10-20 (items 1–3 done). 2026-10-20 (list created; feature freeze)
Read this when: you ask "what's next?" for the release, or finish an item and mark it done.

**Deadline:** before 2027-03-31 (end of Tomcat 9 support). **Runbook:** [release-dev.md](release-dev.md). **Background:** [upgrade-status.md](upgrade-status.md).

States: **Done** · **In progress** · **Pending** · **Waiting** (blocked on another item or a person).

| # | Item | Who | State | Result / evidence |
|---|---|---|---|---|
| 1 | Re-rehearse the whole release with migration 008: MySQL 5.7 → 8.0 → 8.4 → 9.7, migrations 001–008, the container, the checks after deploy, and the rollback | Claude | **Done** 2026-10-20 | Every step passed: upgrade to 9.7.2, migrations 001–008 clean, `SmokeTest` 19/19 and 7 browser checks on the migrated data, rollback restored the old app ([release-dev.md](release-dev.md), *Second rehearsal*) |
| 2 | Acceptance checks for the features since the last sign-off (calendar invites, duplicate warning, filters, hiring report, hiring stages), for the domain user | Claude | **Done** 2026-10-20 | [acceptance/features-2026-10.md](acceptance/features-2026-10.md): 7 sections with a test candidate and clean-up; dry-run on a trial 26/26 (calendar update and cancel need a real calendar app: the domain user checks them) |
| 3 | Release notes for users: what's new and what changes for them | Claude | **Done** 2026-10-20 | [release-notes.md](release-notes.md): changes for everyone, the four roles, what is new per role, what is not in this release |
| 4 | Refresh the Word handbooks (`docs/handbooks/`, last updated 2026-10-03) | Claude | **Done** 2026-11-05 | All three regenerated for `dev` ([handbooks/README.md](handbooks/README.md)): BA guide with hiring stages, calendar files, duplicates, filters and the hiring report (process diagram redrawn); IT guide with MySQL 9.7, Connector/J 9.7.0, 13 tables, migration 008, `/health`, DB timeouts, CSP, the container and the runbook; developer handbook with the new controllers, `WorkflowTest`, CI and 590 tests. New `src/make-docx.py` builds them without PowerShell |
| 5 | Run `db/checks/pre-release-checks.sql` on production (read-only) and send `checks.txt` and `errors.txt` | Owner | Waiting | [db/checks/README.md](../db/checks/README.md) |
| 6 | Decide: migration 003 (hash the old plain-text passwords, G13: recommended, the biggest security gain), Tomcat 11 or container, JDK, HTTPS (recommended: passwords and sessions otherwise cross the network unencrypted), the release date; confirm the stage rules ([open-questions.md](open-questions.md) #32) | Owner | **Done** 2026-11-05 | Migration 003: yes, at the release. Tomcat 11 on a server (not the container). The company's standard JDK 21. HTTPS: yes. Release window February–March 2027 (exact date to fix). Stage rules and migration 008's starting stages: keep as built ([open-questions.md](open-questions.md) #17, #25, #32) |
| 7 | With item 5's output: replace the inferred `db/schema.sql` with the real structure and rerun the DAO tests ([gaps.md](gaps.md) G22), finalise migration 005, code G28 (length check) and G33 (unique index on Position and Language names), plan the MySQL upgrade steps | Claude | Waiting on 5 | |
| 8 | Settings that follow item 6: the Secure flag on the session cookie behind HTTPS; the migration 003 steps in the runbook | Claude | **Done** 2026-11-05 | `RMS_SECURE_COOKIE=true` makes the session cookie always Secure (`WebInitializer`, `WebInitializerTest`, [config.md](config.md)); `deploy/tomcat/rms.xml` has a commented `RemoteIpValve` for HTTPS ended at a proxy; [release-dev.md](release-dev.md): decisions recorded, migration 003 as ops step 4b, HTTPS in step 6, HTTPS checks after deploy |
| 9 | The release itself, following [release-dev.md](release-dev.md) on Tomcat 11, with HTTPS and migration 003 (step 4b), in February–March 2027 | Ops | Waiting on 5, 7, the features F1–F11 and a new rehearsal | |
| 10 | Acceptance by the domain user on production (item 2's checks) and sign-off | Domain user | Waiting on 9 | |
| 11–16 | Moved to the feature list below (F1–F5, F12) on 2026-11-05 | — | — | |

## Features before the release (owner decision 2026-11-05)
Built one at a time, each in its own pull request into `dev` with tests and docs. Every new migration joins the release (runbook step 4), so the release is rehearsed again after the last feature (item 1 again).

Decisions: e-mails go to staff only, never to candidates; sign-in with a company account (F12) is skipped until the company names an identity provider; data retention is a setting that is off by default, and nothing is deleted without a Super Admin's confirmation.

| # | Feature | State | Result / evidence |
|---|---|---|---|
| F1 | E-mail notifications to staff: interviewer assigned; interview scheduled, moved or cancelled (with the calendar file); evaluation submitted (to HR). Sent only when a mail server is configured | **Done** 2026-11-05 | `NotificationServiceImpl` (background thread; never breaks the action), `MailSettings` (`RMS_MAIL_*`, `RMS_APP_URL`); a real SMTP delivery tested with Mailpit (`MailDeliveryTest`); marking an interview Done sends nothing ([business-flows/reports-activity.md](business-flows/reports-activity.md), [config.md](config.md)) |
| F2 | Interview rounds (e.g. first, technical, final), each with its own evaluations | **Done** 2026-11-05 | Rounds on interviews and evaluations (migration 009), one evaluation per interviewer, candidate and round, a tab per round on the form, averages per round ([business-flows/evaluation.md](business-flows/evaluation.md)); the browser workflow test covers two rounds. The list of rounds is open question #33 |
| F3 | Several roles per user (e.g. Hiring Manager and Interviewer with one account) | **Done** 2026-11-05 | "Also interviews" on Super Admin, HR and Hiring Manager (migration 010): `ROLE_INTERVIEWER` besides their role, assignable as interviewers, the Interviews menu; a change signs the user out ([business-flows/users.md](business-flows/users.md)); covered by `SecurityConfigTest` and the browser workflow |
| F4 | Bulk import of candidates from CSV, with a preview, per-row errors and the duplicate check | **Done** 2026-11-05 | Candidates → Import: template, preview of every line (same checks as the form via `CandidateRules`, duplicates in RMS and in the file), then import; Excel's CSV variants read by `CsvReader` ([business-flows/candidates.md](business-flows/candidates.md)); covered in Chrome by the workflow test |
| F5 | Backups (database and documents together, with a restore test) and `/health` in monitoring: ops documentation | Pending | |
| F6 | Pipeline per job: candidates per stage for each job, and when it was filled | Pending | |
| F7 | Candidate search across names, e-mail, phone and ID | Pending | |
| F8 | Offer tracking: offer date, salary band, start date, accepted or declined | Pending | |
| F9 | Interviewer workload: interviews and open evaluations per interviewer per week | Pending | |
| F10 | Home-page reminders: overdue evaluations, candidates too long in one stage | Pending | |
| F11 | Data retention: a configurable period (off by default); a Super Admin sees who is due and confirms the clean-up of personal data and documents | Pending | |
| F12 | Sign-in with a company account (single sign-on) | Skipped | Until the company names an identity provider |

Dependabot's weekly updates go in as they pass CI.
