# Flow: Login / Session

Purpose: Traces the login, role-based menu and logout.
Last updated: 2026-09-25
Read this when: you're fixing login or logout, the role-based menu, or session handling.

Evidence: `rms/controller/LoginController.java`, `rms/service/LoginServiceImpl.java`, `rms/dao/LoginDaoImpl.java`, `WEB-INF/jsp/login.jsp`, `main.jsp`.

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

## Notes
- **Login page and logout:** `GET /login` invalidates the session and shows `login.jsp`. The Logout link in `main.jsp` points to `login` (`LoginController.loginPage`).
- **Role-based menu:** `main.jsp` shows the admin menu when `isinterviewer` is `N` and the interviewer menu when it is `Y`, and displays the name, role and email (`main.jsp`).
- **Password check:** the password is compared as plain text in SQL (`LoginDaoImpl.listallusers`).
- **Session use:** the `user` session attribute is set but never checked by other controllers (`rms/controller/*`).
- **Shared field:** `LoginController` keeps `userinfo` in an instance field, which is shared across requests because the controller is a singleton (`LoginController.java`).
- **Known gaps** (details in [gaps.md](../gaps.md)):
  - G11: no auth checks.
  - G12: shared field.
  - G13: plain-text password.
  - G26: HTTP 500 if a user has no `admin` row.
  - G27: unescaped profile output in `main.jsp`.
  - G30: the menu is only reached via POST; the fallback redirect ignores the app's context path; the session isn't renewed at login.
