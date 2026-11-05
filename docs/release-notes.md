# RMS: What's New

Purpose: the release notes for the people who use RMS: what is new and what changes for them when the new version (`dev`) goes live. Plain language; the technical details are in [release-dev.md](release-dev.md).
Last updated: 2026-11-05 (import from CSV). 2026-11-05 (staff who also interview). 2026-11-05 (interview rounds). 2026-11-05 (e-mails to staff). 2026-11-05 (passwords all encrypted at the release, HTTPS). 2026-10-20 (written for release-tracker item 3)
Read this when: you announce the release to users, or a user asks what changed.

The new RMS replaces the version from 2020. Your data stays: candidates, scores, positions and languages are all kept. Below: what changes for everyone, then what is new for each role.

## For everyone
- **A new look.** A menu on the left with the pages your role can use, and a home page with the figures that matter to you. It also works on a phone.
- **Your password still works.** Sign in as before. From the release on, every password is stored encrypted, the existing ones included. Everyone can change their own password under **Change password**.
- **Secure connection.** RMS opens over `https://`; an old `http://` bookmark takes you there.
- **Five wrong passwords lock the sign-in** for that user name, from that computer, for 15 minutes.
- **Your role decides what you see.** If a page isn't for your role, RMS says so instead of showing it.
- **English only.** Names, reasons, comments, notes and locations accept English letters only. Other scripts are refused with a message instead of being stored as codes.
- **Signed out when your account changes.** If a Super Admin changes your role or deactivates you, your next click goes back to the sign-in page.

## Roles
There are four roles. A Super Admin, HR user or Hiring Manager can **also interview** (one account, new): a Super Admin ticks "Also interviews" under Users and Roles, and that person gets My Evaluations and My Schedule too and can be assigned to candidates.

The four roles: On the first day, the old administrators are **Super Admins** and the old interviewers are **Interviewers**. A Super Admin then gives the **HR** and **Hiring Manager** roles under **Users and Roles**.

| Role | Can |
|---|---|
| Super Admin | everything HR can, plus Users and Roles, the Activity Log, and permanently deleting a deleted candidate's files |
| HR | candidates, documents, jobs, positions, languages, interviewers, decisions, hiring stages, the interview schedule, reports |
| Hiring Manager | read everything HR sees and download the reports; changes nothing |
| Interviewer | My Evaluations (score assigned candidates), My Schedule, the profiles of assigned candidates |

## New for HR and Super Admins
- **Candidates:**
  - Contact details, source, applied date and an optional job are recorded.
  - IDs (C1, C2, …) are given automatically.
  - Deleting a candidate keeps their scores.
- **Documents:** upload a CV and other documents (PDF, JPG or PNG, up to 5 MB) on the candidate's profile. The list shows who still has no CV.
- **Jobs:** open and closed job openings, linked to a position. The Jobs page shows each job's pipeline (new): how many candidates are at each stage, how many were rejected, and when the job was filled; click a number to see those candidates.
- **Interviewers and evaluations:**
  - Assign interviewers to a candidate.
  - Candidate Status shows the average of each of the 10 criteria.
  - Record **Selected**, **Rejected** or **On hold** with a reason. Every decision is kept.
- **Interview schedule:**
  - Schedule interviews with an assigned interviewer, at a time that hasn't passed.
  - The same interviewer can't be booked twice for the same minute.
- **Interview rounds (new):** give an interview a round (First, Technical or Final interview). Interviewers evaluate each round separately, and the candidate's evaluations page shows the averages per round; Candidate Status still averages them all. Older interviews and evaluations show "No round".
- **Hiring stages (new):** every candidate moves through Applied → Screening → Interview → Offer → Hired, or Withdrawn.
  - Move a candidate on their profile's **Pipeline** card, with an optional note.
  - RMS keeps the stage in step with the decision:
    - Selected moves the candidate to Offer.
    - On hold or Rejected moves Offer or Hired back to Interview.
    - Scheduling an interview moves Applied or Screening to Interview.
  - A Rejected candidate's pipeline is closed until the decision changes.
- **Import from Excel (new):** Candidates → **Import**: download the template, fill one candidate per line in Excel, save as CSV and upload it. RMS shows every line first (ready, possible duplicate, or what is wrong) and adds nothing until you press Import. Lines with a problem are skipped; possible duplicates only if you tick the box. Up to 500 candidates per file.
- **Offers (new):** on a Selected candidate's profile, record the offer: date, salary band, start date, and whether it was accepted, declined or withdrawn. An accepted offer moves the candidate to Hired. Hiring Managers see the offers but not the salary band.
- **Possible duplicates (new):** adding a candidate whose e-mail, phone number or full name is already in RMS shows the existing candidate first. Choose **Check again** after correcting, or **Save anyway** if it is someone else.
- **Search (new):** the search box at the top of every page finds candidates by name, e-mail, phone or ID (Super Admin, HR, Hiring Manager).
- **Filters (new):** on Candidates, filter by job, stage, status, source, applied dates, "No CV yet" or "Not evaluated yet".
  - Bookmark a filtered list.
  - **Download these as CSV** exports exactly what is shown.

## New for everyone who reads reports (Super Admin, HR, Hiring Manager)
- **Reports:** CSV downloads that open in Excel: candidates, Candidate Status, the interview schedule, and the new **Hiring Report**.
- **Hiring report (new):** on the Reports page:
  - how many days from application to Selected or Rejected;
  - candidates per stage, per job and per source;
  - evaluations still to do per interviewer.
- **Home page:** candidates by decision and by stage (click a stage to see its candidates), evaluations still to do, upcoming interviews, open jobs, and candidates without a CV.
- **Note for spreadsheet users:** the candidates CSV now has a **Stage** column before Status. A spreadsheet that reads the columns by position needs adjusting.

## New for Interviewers
- **My Evaluations:** your assigned candidates. Score each on the 10 criteria with comments, and change it until a final decision is made. With interview rounds, the form has a tab per round and opens at the round of your interview: evaluate each round on its own tab.
- **My Schedule:** your interviews.
  - **Calendar (new)** downloads the interview as a file for Outlook, Google Calendar or Apple Calendar.
  - Download it again after a change, so your calendar updates the event.
  - Nobody is e-mailed.

## E-mails (new)
RMS e-mails staff about their work, once the IT team has connected it to a mail server:
- **Interviewers:** when you are assigned to a candidate; when an interview of yours is scheduled, changed or cancelled. The interview mails carry the calendar file, so one click updates your calendar.
- **HR:** when an interviewer saves an evaluation. The mail says who evaluated whom; the scores stay in RMS.
- **Candidates** are never e-mailed by RMS.
- Make sure your profile has your work e-mail address (a Super Admin can set it under Users and Roles).

## Not in this release
- **Times are the server's local time.** This includes the calendar files.

## Questions
Ask your RMS administrator (a Super Admin). Problems after the release go to the team that runs RMS.
