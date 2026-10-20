# Release Tracker: `dev` to production

Purpose: the ordered list of what is left before and after releasing `dev` (feature freeze from 2026-10-20), with who does each item and its state. Work goes one item at a time, top to bottom.
Last updated: 2026-10-20 (items 1–3 done). 2026-10-20 (list created; feature freeze)
Read this when: you ask "what's next?" for the release, or finish an item and mark it done.

**Deadline:** before 2027-03-31 (end of Tomcat 9 support). **Runbook:** [release-dev.md](release-dev.md). **Background:** [upgrade-status.md](upgrade-status.md).

States: **Done** · **In progress** · **Pending** · **Waiting** (blocked on another item or a person).

| # | Item | Who | State | Result / evidence |
|---|---|---|---|---|
| 1 | Re-rehearse the whole release with migration 008: MySQL 5.7 → 8.0 → 8.4 → 9.7, migrations 001–008, the container, the checks after deploy, and the rollback | Claude | **Done** 2026-10-20 | Every step passed: upgrade to 9.7.2, migrations 001–008 clean, `SmokeTest` 19/19 and 7 browser checks on the migrated data, rollback restored the old app ([release-dev.md](release-dev.md), *Second rehearsal*) |
| 2 | Acceptance checks for the features since the last sign-off (calendar invites, duplicate warning, filters, hiring report, hiring stages), for the domain user | Claude | **Done** 2026-10-20 | [acceptance/features-2026-10.md](acceptance/features-2026-10.md): 7 sections with a test candidate and clean-up; dry-run on a trial 26/26 (calendar update and cancel need a real calendar app: the domain user checks them) |
| 3 | Release notes for users: what's new and what changes for them | Claude | **Done** 2026-10-20 | [release-notes.md](release-notes.md): changes for everyone, the four roles, what is new per role, what is not in this release |
| 4 | Refresh the Word handbooks (`docs/handbooks/`, last updated 2026-10-03) | Claude | Pending | |
| 5 | Run `db/checks/pre-release-checks.sql` on production (read-only) and send `checks.txt` and `errors.txt` | Owner | Waiting | [db/checks/README.md](../db/checks/README.md) |
| 6 | Decide: migration 003 (hash passwords), Tomcat 11 or container, JDK, HTTPS, the release date; confirm the stage rules ([open-questions.md](open-questions.md) #32) | Owner | Waiting | |
| 7 | With item 5's output: finalise migration 005, correct `db/schema.sql`, code G28 (length check) and G33 (unique index), plan the MySQL upgrade steps | Claude | Waiting on 5 | |
| 8 | Settings that follow item 6 (e.g. the secure session cookie behind HTTPS) | Claude | Waiting on 6 | |
| 9 | The release itself, following [release-dev.md](release-dev.md) | Ops | Waiting on 1–8 | |
| 10 | Acceptance by the domain user on production (item 2's checks) and sign-off | Domain user | Waiting on 9 | |
| 11 | After go-live: interview rounds, several roles per user, bulk import, e-mail notifications (owner to choose) | Owner, then Claude | After 10 | |
