# Business Flows — Index

Purpose: The business capabilities RMS implements, and which code implements each one.
Last updated: 2026-10-02 (candidate management, G1, with soft delete). 2026-10-01 (Spring Security 7: login, roles, logout). 2026-10-01 (UI redesign: menu in `layout.tag`). 2026-10-01 (entry points `GET /`, `GET /home`; deletes are POST). 2026-09-25
Read this when: you're working on a feature and need to find its flow file. Open only the module file you need.

- RMS is a recruitment management system (`README.md`).
- It has two roles, chosen by `admin.isinterviewer` (the menu in `WEB-INF/tags/layout.tag`):
  - **Admin (`N`):** manages candidates, positions and languages, and views candidate results.
  - **Interviewer (`Y`):** meant to enter scores, but that flow is partial (see [open-questions.md](../open-questions.md)).

## Confirmed capabilities
| Capability | Description | Entry Point | Key Classes | DB Tables | Detail file |
|---|---|---|---|---|---|
| Login / session | Checks the username and password (Spring Security), loads the profile, enforces the role on every page, and shows the menu for the user's role | `GET /` (→ `/home`), `GET /login`, `POST /welcome` (→ `GET /home` or `/login?error`), `POST /logout` | `SecurityConfig`, `LoginController`, `LoginServiceImpl`, `RmsUserDetails`, `LoginDaoImpl`, `LoginInfo`, `UserInfo` | `users`, `admin` | [login.md](login.md) |
| Position master | Create, list, rename and delete (soft, `POST`) job positions | `/createposition`, `/saveposition`, `/viewpositionlist`, `/updateposition/{k}`, `POST /deleteposition/{k}` | `PositionController`, `PositionServiceImpl`, `PositionDaoImpl`, `PositionInfo` | `position` | [masters.md](masters.md) |
| Language master | Create, list, rename and delete (soft, `POST`) the languages/skills candidates are assessed on | `/createlanguage`, `/savelanguage`, `/viewlanguagelist`, `/updatelanguage/{k}`, `POST /deletelanguage/{k}` | `LanguageController`, `LanguageServiceImpl`, `LanguageDaoImpl`, `LanguageInfo` | `language` | [masters.md](masters.md) |
| Candidate management | Add, list, edit and delete (soft, `POST`) candidates, each with a position and a language; IDs generated as C1, C2, … | `/viewcandidatelist`, `/createcandidate`, `POST /savecandidate`, `/updatecandidate?candidateid=…`, `POST /deletecandidate` | `CandidateController`, `CandidateServiceImpl`, `CandidateDaoImpl`, `CandidateInfo` | `candidate` (reads `position`, `language`) | [candidates.md](candidates.md) |
| Candidate results (admin, read-only) | Lists every candidate's 10 scores, the total and the S/R status | `GET /adminviewmarks` | `MarksController`, `MarksServiceImpl`, `MarksDaoImpl`, `MarksInfo` | `marks`, `candidate`, `position`, `language`, `admin` | [evaluation.md](evaluation.md) |

## Business ↔ Tech map
| Capability | Controller | Service | DAO | Model | View(s) |
|---|---|---|---|---|---|
| Login / session | `rms.config.SecurityConfig` (Spring Security), `rms.controller.LoginController` | `rms.service.LoginServiceImpl` | `rms.dao.LoginDaoImpl` | `rms.model.LoginInfo`, `rms.model.UserInfo` | `login.jsp`, `main.jsp` (home), `layout.tag` (menu on every page) |
| Position master | `rms.controller.PositionController` | `rms.service.PositionServiceImpl` | `rms.dao.PositionDaoImpl` | `rms.model.PositionInfo` | `createposition.jsp`, `viewposition.jsp` |
| Language master | `rms.controller.LanguageController` | `rms.service.LanguageServiceImpl` | `rms.dao.LanguageDaoImpl` | `rms.model.LanguageInfo` | `createlanguage.jsp`, `viewlanguage.jsp` |
| Candidate management | `rms.controller.CandidateController` | `rms.service.CandidateServiceImpl` | `rms.dao.CandidateDaoImpl` | `rms.model.CandidateInfo` | `createcandidate.jsp`, `viewcandidate.jsp` |
| Candidate results | `rms.controller.MarksController` | `rms.service.MarksServiceImpl` | `rms.dao.MarksDaoImpl` | `rms.model.MarksInfo` | `viewmarks.jsp` |

Partial flows (interviewers, jobs, schedules, interviewer score entry) show as disabled "Coming soon" menu entries; they are listed in [open-questions.md](../open-questions.md).
