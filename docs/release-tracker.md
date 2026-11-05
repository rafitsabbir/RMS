# Release Tracker: `dev` to production

Purpose: the ordered list of what is left before and after releasing `dev` (feature freeze from 2026-10-20), with who does each item and its state. Work goes one item at a time, top to bottom.
Last updated: 2026-11-05 (recommended next steps added: items 6–8 made explicit, item 9 runs migration 003, items 11–16 after go-live in order). 2026-11-05 (item 4 done). 2026-10-20 (items 1–3 done). 2026-10-20 (list created; feature freeze)
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
| 6 | Decide: migration 003 (hash the old plain-text passwords, G13: recommended, the biggest security gain), Tomcat 11 or container, JDK, HTTPS (recommended: passwords and sessions otherwise cross the network unencrypted), the release date; confirm the stage rules ([open-questions.md](open-questions.md) #32) | Owner | Waiting | |
| 7 | With item 5's output: replace the inferred `db/schema.sql` with the real structure and rerun the DAO tests ([gaps.md](gaps.md) G22), finalise migration 005, code G28 (length check) and G33 (unique index on Position and Language names), plan the MySQL upgrade steps | Claude | Waiting on 5 | |
| 8 | Settings that follow item 6: the Secure flag on the session cookie behind HTTPS; the migration 003 steps in the runbook if chosen | Claude | Waiting on 6 | |
| 9 | The release itself, following [release-dev.md](release-dev.md), including migration 003 if item 6 chose it | Ops | Waiting on 1–8 | |
| 10 | Acceptance by the domain user on production (item 2's checks) and sign-off | Domain user | Waiting on 9 | |
| 11 | E-mail notifications (interviewer assigned, interview booked or moved, decision recorded). Needs the owner's choice of mail server and of who is e-mailed (staff only, or candidates too) | Owner, then Claude | After 10 | |
| 12 | Interview rounds (e.g. first, technical, final), each with its own evaluations, on top of the existing stages and schedule | Owner, then Claude | After 10 | |
| 13 | Several roles per user (today one person who is a Hiring Manager and an Interviewer needs two accounts) | Owner, then Claude | After 10 | Worth it if that is common |
| 14 | Bulk import of candidates from CSV or Excel | Owner, then Claude | After 10 | Worth it if candidates arrive in batches |
| 15 | Sign-in through the company's identity provider (single sign-on with Spring Security's OAuth2 support), so RMS keeps no passwords | Owner, then Claude | After 10 | Only if the company has one |
| 16 | Operations: a regular database backup, and `/health` in the monitoring | Ops | After 9 | |

Items 11–16 are in the recommended order; the owner may reorder them. Until the release, no new features (feature freeze); Dependabot's weekly patch updates go in as they pass CI.
