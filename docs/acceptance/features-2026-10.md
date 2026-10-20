# New Features of October 2026: Acceptance Checks

Purpose: The browser checks a domain user follows to sign off the features added on `dev` on 2026-10-20: interview calendar files, the possible-duplicate warning, the Candidates filters, the hiring report and the hiring stages.
Last updated: 2026-10-20 (written for release-tracker item 2; dry-run on a trial: every step and expected text as written, 26 checks in Chromium, D3 and D4 not automated because they need a real calendar app)
Read this when: you sign off the release ([release-tracker.md](../release-tracker.md) item 10), or you test these features on a trial.

**Where:** on the released system right after the release, or on a trial (`deploy/docker-compose.yml`, synthetic data). On production, use **one test candidate** named "Test Acceptance" and delete them at the end (step G). Never change a real candidate's stage or decision for a check.

**Who:** a Super Admin or HR user, a Hiring Manager, and an Interviewer. Write the result in the last column: ✓, or what you saw instead.

**Already checked automatically** on every change: the same flows run in Chrome in CI (`WorkflowTest`), and each feature has its own tests ([build-run.md](../build-run.md)). These checks are the human sign-off: do the pages read well, and do they match how you work?

## A. Test candidate (HR)
| # | Step | Expected | Result |
|---|---|---|---|
| A1 | Candidates → **Add candidate**: first name "Test", last name "Acceptance", a position and language, e-mail `acceptance@example.test`, phone `000 123 456`, applied today → Save | Back on Candidates; the new ID (C…) is listed with Stage **Applied** | |
| A2 | Open the candidate | The profile has a **Pipeline** card: steps Applied, Screening, Interview, Offer, Hired; "Now at **Applied**"; a Move form | |

## B. Possible duplicates (HR)
| # | Step | Expected | Result |
|---|---|---|---|
| B1 | Add another candidate with any name and the e-mail `ACCEPTANCE@example.test` (other case) → Save | The form comes back with "This may be a candidate RMS already has", listing Test Acceptance with "same e-mail"; nothing is saved | |
| B2 | The link in the warning | Opens Test Acceptance's profile in a new tab | |
| B3 | Press **Enter** in a field | The warning shows again: Enter means **Check again**, never Save anyway | |
| B4 | Change the e-mail to something unused, clear the phone, give another name → **Check again** | Saved, back on Candidates | |
| B5 | Delete that second candidate (it was only for this check) | It leaves the list | |
| B6 | Try "Test" "Acceptance" again with no e-mail → Save | Warning: "same name". Choose **Cancel** (don't save) | |

## C. Hiring stages (HR, then the other roles)
| # | Step | Expected | Result |
|---|---|---|---|
| C1 | Test Acceptance → Move to **Screening**, note "Phone call" → Move | "Moved to Screening."; Now at Screening; the history shows Applied → Screening, your name, the note | |
| C2 | Move to **Offer** | Refused: "Offer needs the decision Selected. Record Selected first." | |
| C3 | Assign an interviewer (Interviewers card), then Interview Schedule → **Schedule interview** for Test Acceptance, a time tomorrow | Saved; the profile now says **Now at Interview** ("Interview scheduled" in the history) | |
| C4 | Evaluations and decision → record **Selected** with a reason | The message ends "The candidate moves to Offer."; the profile says Now at Offer | |
| C5 | Move to **Hired**, note "Contract signed" | Now at Hired | |
| C6 | Record **On hold** | "moves to Interview": Offer and Hired always mean Selected | |
| C7 | Record **Rejected** | The profile shows "Rejected at Interview"; the Move form is greyed out with "Change the decision first" | |
| C8 | Sign in as the **Hiring Manager**, open Test Acceptance | The pipeline and history show; no Move form | |
| C9 | Sign in as the assigned **Interviewer**, open the candidate from My Evaluations | The step bar shows; no history, no form | |
| C10 | Two tabs on the same profile as HR: move in the first, then try a move in the second | The second says the stage "was changed meanwhile"; nothing is overwritten | |

## D. Calendar invites (Interviewer, then staff)
| # | Step | Expected | Result |
|---|---|---|---|
| D1 | As the Interviewer: My Schedule → **Calendar** on the interview from C3 | A file `interview-C…-<date>.ics` downloads | |
| D2 | Open it with your calendar (Outlook, Google Calendar or Apple Calendar) | One event: "Interview: Test Acceptance (C…)", the time as shown in RMS, one hour long, the location; nobody is invited and no e-mail is sent | |
| D3 | As HR, change the interview's time; the Interviewer downloads the file again and opens it | The calendar updates the same event, it doesn't add a second one. Google Calendar may add a copy instead: note what you see | |
| D4 | As HR, cancel the interview; download and open again | The event shows as cancelled (or is removed), depending on the calendar | |
| D5 | As the Hiring Manager: Interview Schedule | A **Calendar** column; no Edit or Cancel | |

## E. Candidates filters (HR or Hiring Manager)
| # | Step | Expected | Result |
|---|---|---|---|
| E1 | Candidates → Filter: Stage **Interview** → Apply | Only candidates at Interview; "Showing n of m candidates"; the address bar holds the filter (it can be bookmarked) | |
| E2 | Add Status **Rejected** → Apply | Test Acceptance is listed (Rejected at Interview) | |
| E3 | **Download these as CSV** | The file holds exactly the listed candidates, with a **Stage** column before Status | |
| E4 | Clear; try "No CV yet", "Not evaluated yet", a source, an applied-date range, a job | Each narrows the list as its name says | |
| E5 | Home page (staff): **Candidates by stage** | Six tiles; clicking one opens the list filtered to that stage | |

## F. Hiring report (staff)
| # | Step | Expected | Result |
|---|---|---|---|
| F1 | Reports | Under the downloads: Days to Selected and to Rejected, **Candidates per stage**, per job, per source, and evaluations still to do per interviewer | |
| F2 | Compare a few numbers with the Candidates list (e.g. Rejected per job) | They match the list | |
| F3 | **Hiring Report** download | A CSV with the same figures | |
| F4 | As the Interviewer, try to open `/reports` | Refused (403) | |

## G. Clean up (HR)
| # | Step | Expected | Result |
|---|---|---|---|
| G1 | Delete Test Acceptance (Candidates → Delete) | It leaves the list, the report and the dashboard counts | |
| G2 | (Super Admin) Activity Log | The checks are logged: candidate added, stage changed, interview scheduled, decisions, report downloads; no notes or reasons in the log | |

**Sign-off:** name, date, and the rows that were not ✓ with what was seen.
