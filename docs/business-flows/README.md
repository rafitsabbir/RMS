# Business Flows — Index

Purpose: The business capabilities RMS implements, and which code implements each one.
Last updated: 2026-09-25
Read this when: you're working on a feature and need to find its flow file. Open only the module file you need.

- RMS is a recruitment management system (`README.md`).
- It has two roles, chosen by `admin.isinterviewer` (`main.jsp`):
  - **Admin (`N`):** manages positions and languages, and views candidate results.
  - **Interviewer (`Y`):** meant to enter scores, but that flow is partial (see [open-questions.md](../open-questions.md)).

## Confirmed capabilities
| Capability | Description | Entry Point | Key Classes | DB Tables | Detail file |
|---|---|---|---|---|---|
| Login / session | Checks the username and password, loads the profile, and shows the menu for the user's role | `POST /welcome`, `GET /login` | `LoginController`, `LoginServiceImpl`, `LoginDaoImpl`, `UserInfo` | `users`, `admin` | [login.md](login.md) |
| Position master | Create, list, rename and delete job positions | `/createposition`, `/saveposition`, `/viewpositionlist`, `/updateposition/{k}`, `/deleteposition/{k}` | `PositionController`, `PositionServiceImpl`, `PositionDaoImpl`, `PositionInfo` | `position` | [masters.md](masters.md) |
| Language master | Create, list, rename and delete the languages/skills candidates are assessed on | `/createlanguage`, `/savelanguage`, `/viewlanguagelist`, `/updatelanguage/{k}`, `/deletelanguage/{k}` | `LanguageController`, `LanguageServiceImpl`, `LanguageDaoImpl`, `LanguageInfo` | `language` | [masters.md](masters.md) |
| Candidate results (admin, read-only) | Lists every candidate's 10 scores, the total and the S/R status | `GET /adminviewmarks` | `MarksController`, `MarksServiceImpl`, `MarksDaoImpl`, `MarksInfo` | `marks`, `candidate`, `position`, `language`, `admin` | [evaluation.md](evaluation.md) |

## Business ↔ Tech map
| Capability | Controller | Service | DAO | Model | View(s) |
|---|---|---|---|---|---|
| Login / session | `rms.controller.LoginController` | `rms.service.LoginServiceImpl` | `rms.dao.LoginDaoImpl` | `rms.model.UserInfo` | `login.jsp`, `main.jsp` |
| Position master | `rms.controller.PositionController` | `rms.service.PositionServiceImpl` | `rms.dao.PositionDaoImpl` | `rms.model.PositionInfo` | `createposition.jsp`, `viewposition.jsp` |
| Language master | `rms.controller.LanguageController` | `rms.service.LanguageServiceImpl` | `rms.dao.LanguageDaoImpl` | `rms.model.LanguageInfo` | `createlanguage.jsp`, `viewlanguage.jsp` |
| Candidate results | `rms.controller.MarksController` | `rms.service.MarksServiceImpl` | `rms.dao.MarksDaoImpl` | `rms.model.MarksInfo` | `viewmarks.jsp` |

Partial flows (candidates, interviewers, jobs, schedules, interviewer score entry) are listed in [open-questions.md](../open-questions.md).
