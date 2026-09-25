# RMS — Detailed Flow Traces

Detailed request traces that support the summary in [CLAUDE.md](../CLAUDE.md). Every step was confirmed by reading the source files named here.

## Flow 1: Login (`POST /welcome`)
Evidence: `src/main/java/rms/controller/LoginController.java`, `rms/service/LoginServiceImpl.java`, `rms/dao/LoginDaoImpl.java`, `src/main/webapp/WEB-INF/jsp/login.jsp`, `main.jsp`.

```mermaid
sequenceDiagram
  participant B as Browser (login.jsp)
  participant C as LoginController.doLogin
  participant S as LoginServiceImpl
  participant D as LoginDaoImpl
  participant DB as MySQL
  B->>C: POST /welcome (username, password)
  C->>S: checkLogin(u, p)
  S->>D: checkUser(u, p)
  D->>DB: select userid from users where username=? and password=?
  alt userid found
    C->>S: getUserInfo(userid)
    S->>D: getUserInfo
    D->>DB: select * from admin where userid=?
    C->>C: session.setAttribute("user", UserInfo)
    C-->>B: view "main" (menu depends on isinterviewer Y/N)
  else no row (EmptyResultDataAccessException → null)
    C-->>B: view "login" + errorMessage "Invalid login!"
  end
```
- `GET /login` invalidates the session and shows `login.jsp`. The Logout link in `main.jsp` points to `login`.
- `main.jsp` shows the admin menu when `isinterviewer` is `N` and the interviewer menu when it is `Y`.

## Flow 2: Position maintenance (Language is identical)
Evidence: `rms/controller/PositionController.java`, `rms/service/PositionServiceImpl.java`, `rms/dao/PositionDaoImpl.java`, `WEB-INF/jsp/createposition.jsp`, `viewposition.jsp`. For Language: `LanguageController`, `LanguageServiceImpl`, `LanguageDaoImpl`, table `language`.

```mermaid
sequenceDiagram
  participant B as Browser
  participant C as PositionController
  participant D as PositionDaoImpl (via PositionServiceImpl)
  participant DB as MySQL position
  B->>C: GET /createposition
  C-->>B: createposition.jsp (empty PositionInfo)
  B->>C: POST /saveposition (positionname, positionkey)
  alt positionkey > 0
    C->>D: updatePosition
    D->>DB: UPDATE position SET positionname (UPPER + trim)
  else new
    C->>D: addPosition
    D->>DB: SELECT positionname (duplicate check)
    alt exists
      D->>D: System.out "already exist!" (no user feedback)
    else not found
      D->>DB: INSERT position (isActive=1, positionname)
    end
  end
  C-->>B: redirect:/viewpositionlist
  B->>C: GET /viewpositionlist
  C->>D: getAllPosition
  D->>DB: SELECT ... WHERE isactive=1
  C-->>B: viewposition.jsp (DataTable, Update/Delete links)
  B->>C: GET /deleteposition/{key}
  C->>D: deletePosition
  D->>DB: DELETE FROM position (hard delete)
```

## Flow 3: Candidate evaluation results (`GET /adminviewmarks`)
Evidence: `rms/controller/MarksController.java`, `rms/service/MarksServiceImpl.java`, `rms/dao/MarksDaoImpl.java` (`getAllMarksByAdmin`, `MarksMapper`), `WEB-INF/jsp/viewmarks.jsp`.

```mermaid
flowchart LR
  A["main.jsp admin menu: Candidate Status"] --> B[MarksController.adminViewMarks]
  B --> C[MarksServiceImpl.getAllMarksByAdmin]
  C --> D[MarksDaoImpl.getAllMarksByAdmin]
  D --> E[("marks + candidate + position + language, admin subquery")]
  E --> F["MarksMapper, maps by column index"]
  F --> G["viewmarks.jsp: 10 scores, Total = sum, status S/R/other"]
```
- The 10 criteria (`MarksInfo`) are work experience, technical knowledge, leadership, decision making, problem solving, stress tolerance, educational background, communication skill, attitude and personality.
- The total is the unweighted sum of the 10 scores (`viewmarks.jsp`, `getFullmarks`).
- The status column shows an image: `S` → `happy.jpg`, `R` → `sad.jpg`, anything else → `new.jpg` (`viewmarks.jsp`).
