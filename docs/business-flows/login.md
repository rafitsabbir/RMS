# Flow: Login / Session

Purpose: Traces the login, role-based menu and logout.
Last updated: 2026-10-01 (Spring Security 7.0.7 replaces the interim interceptor and the controller's login: form login, roles, CSRF tokens, POST logout, password check in Java). 2026-10-01 (UI redesign: menu and Log out in `layout.tag`, full-page login after expiry). 2026-10-01 (small fixes batch: login redirects to `/home`, `GET /` and `/home`, users without an `admin` row refused, cookie-only sessions, serializable user). 2026-09-30 (G11 login check; G27 profile escaping)
Read this when: you're fixing login or logout, the role-based menu, access rules or session handling.

Evidence: `rms/config/SecurityConfig.java`, `rms/config/SecurityInitializer.java`, `rms/config/PlainTextPasswordEncoder.java`, `rms/service/LoginServiceImpl.java`, `rms/service/RmsUserDetails.java`, `rms/dao/LoginDaoImpl.java`, `rms/controller/LoginController.java`, `WEB-INF/jsp/login.jsp`, `main.jsp`, `WEB-INF/tags/layout.tag`, `WEB-INF/web.xml`. Tests: `rms/config/SecurityConfigTest`, `rms/service/LoginServiceImplTest`, `rms/controller/LoginControllerTest`, `rms/dao/LoginDaoImplTest`, `rms/SmokeTest`.

```mermaid
sequenceDiagram
  participant B as Browser (login.jsp)
  participant F as Spring Security filters
  participant S as LoginServiceImpl
  participant D as LoginDaoImpl
  participant DB as MySQL
  B->>F: GET /login (LoginController; form:form adds the _csrf field)
  B->>F: POST /welcome (username, password, _csrf)
  F->>F: CsrfFilter: token missing or wrong → 403 (or /login?expired if the session expired)
  F->>S: loadUserByUsername(u)
  S->>D: findLogins(u)
  D->>DB: select userid, username, password from users where username=?
  alt exactly one users row
    S->>D: getUserInfo(userid)
    D->>DB: select * from admin where userid=?
    S-->>F: RmsUserDetails (role from isinterviewer; profile may be null)
    F->>F: password check in Java ({bcrypt} or exact plain text)
    alt password matches and the admin row exists
      F->>F: new session id, SecurityContext in the session, password erased
      F->>F: LoginSuccessHandler: session "user" = UserInfo
      F-->>B: 302 /home
    else wrong password, or no admin row (WARN with the userid)
      F-->>B: 302 /login?error ("Invalid login!")
    end
  else no row, or several rows (WARN with the userids)
    F-->>B: 302 /login?error ("Invalid login!")
  end
```

## Notes
- **Filter registration:** `SecurityInitializer` registers Spring Security's filter chain for every request (`AbstractSecurityWebApplicationInitializer`). `WebInitializer` still creates the application context, and component scanning of `rms` picks up `SecurityConfig`.
- **Access rules** (`SecurityConfig.appFilterChain`, `SecurityConfig.java:64-87`):
  - `/login` and `/welcome` are public (`:69`). `/` and `/home` need a login (`:70`). Every other path needs `ROLE_ADMIN` (`:72`), so a new page is admin-only unless a rule above `anyRequest()` says otherwise. Interviewer pages (G5) need such a rule.
  - JSP forwards, includes and error dispatches are allowed, because the request that led to them was already checked (`:67-68`).
  - `/resources/**` has its own chain: public, and without Spring Security's no-cache headers, so browsers keep caching CSS, JS and images (`resourceFilterChain`, `:55-60`).
  - **Logged out** (no session, logged out, or expired): a redirect to `/login`. **Logged in without `ROLE_ADMIN`** (an interviewer, or `isinterviewer` NULL or anything but N/Y): HTTP 403 on every page except `/` and `/home`, logged at WARN with the userid and path (`refuse`, `:131-145`). The interviewer menu shows only Home and disabled "Coming soon" entries (G5).
  - Spring Security's firewall answers URLs with `;` (for example `;jsessionid=`) with HTTP 400. Sessions are cookie-only since G41, so the app never makes such URLs.
- **Roles** (`RmsUserDetails.rolesOf`, `RmsUserDetails.java:41-50`): `isinterviewer` `N` (any case) is `ROLE_ADMIN`, `Y` is `ROLE_INTERVIEWER`, anything else, including NULL, no role (G16).
- **Password check (G13):** in Java, not in SQL. `SecurityConfig.passwordEncoder` (`:94-100`) checks a stored `{bcrypt}…` value with bcrypt, and any other stored value as a legacy plain-text password with an exact, constant-time comparison (`PlainTextPasswordEncoder`). That comparison also runs one throwaway bcrypt check, because Spring Security checks an unknown username against a bcrypt hash: without it a known user would answer in microseconds and an unknown one in tens of milliseconds, which would tell which usernames exist. Nothing is re-hashed or written back; migrating the stored passwords to bcrypt is the owner's call. **Behaviour changes** (open question #25):
  - The comparison is exact. Before, MySQL compared it under the column's collation, which on the MySQL defaults ignores case (and, with PAD SPACE collations, trailing spaces). `SecurityConfigTest.passwordMustMatchExactly` pins the new behaviour.
  - An empty password never matches, even against an empty stored one (Spring Security's `AbstractValidatingPasswordEncoder`).
  - A username on two or more `users` rows can't log in; the WARN names the userids (`LoginServiceImpl.java:31-38`). Before, each row's password worked.
  - A stored plain-text password that happens to start with `{bcrypt}` would be read as a hash.
- **Users without an `admin` row (G26):** refused after the password check, as before, with a WARN naming the userid (`SecurityConfig.authenticationProvider`, `:104-117`). `authenticationProvider` is the only provider bean, so Spring Security adds no default provider that could let such a user in (`SecurityConfigTest.userWithoutAdminRowIsRefused`).
- **After login:** `LoginSuccessHandler` puts the `UserInfo` into the session as `user`, which the JSPs and `LoginController.home` read, and always redirects to `/home` (`:166-181`). The page that led to the login isn't remembered (`NullRequestCache`, `:84`).
- **Failed login (G30):** `SecurityConfig.loginFailed` (`SecurityConfig.java:119-128`) redirects to `/login?error`, where `LoginController.loginPage` shows "Invalid login!" (`LoginController.java:24-38`). A refresh no longer re-posts the credentials. Unlike Spring'"'"'s default handler it keeps nothing in the session; the default would keep the exception, which can carry the typed password (`SecurityConfigTest.failedLoginKeepsNothingInTheSession`). A database error during the login (`InternalAuthenticationServiceException`, logged at ERROR by Spring Security) goes to `/login?unavailable`: "Sign-in isn'"'"'t available right now. Please try again later." Before Spring Security it was HTTP 500.
- **Login page:** `GET /login` shows `login.jsp`. It no longer logs out (G32). `/login?expired` shows "Your session expired. Please sign in again.": a form posted after its session expired carries a CSRF token from that session, and a login form left open in a second tab carries one from before a later login. Spring Security refuses both, and `refuse` sends the browser there instead of answering 403. Any `POST /welcome` with a missing or wrong token goes there (`SecurityConfig.java:133-138`).
- **Logout:** a POST to `/logout` with the CSRF token, from the "Log out" form in the top bar (`layout.tag:115`). It invalidates the session and redirects to `/login` (`SecurityConfig.java:80-82`). `GET /logout` is no page (404 for an admin) and doesn't log out.
- **CSRF (G32):** every POST needs the session's token. Spring's `<form:form>` adds the hidden `_csrf` field by itself; every form in the JSPs and `layout.tag` uses it. A POST without the token, or with a wrong one, gets 403 with a WARN, except the login post and posts from expired sessions, which go back to `/login?expired`.
- **Other behaviour changes with Spring Security** (all intended; none changes what a user can reach):
  - A failed login while already logged in leaves the existing login in place. The old `doLogin` invalidated the session on any failure.
  - The username is trimmed before the lookup (`UsernamePasswordAuthenticationFilter`).
  - A logged-out POST without a token to a page other than `/welcome` gets 403, not a redirect to `/login`. `GET /welcome` is 404, not 405.
  - Every view of the login page has a session, because the form's CSRF token lives in it. The old `GET /login` invalidated the session.
  - Pages get Spring Security's default headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (fine since the UI redesign removed the `<object>` frame), and on HTTPS requests `Strict-Transport-Security: max-age=31536000 ; includeSubDomains`. Whether HSTS with `includeSubDomains` suits the production hostname is for ops (open question #14).
  - Redirects to `/login` stay relative, as before (Spring Security 7 favours relative URIs; `SecurityConfigTest.redirectsToLogin`).
  - There's no login rate limit or lockout (G42).
- **Role-based menu:** the sidebar in `layout.tag` shows the admin entries when `isinterviewer` is `N` (`layout.tag:65-90`) and the interviewer entries when it is `Y` (`:91-98`); the role is `fn:toUpperCase` of the column (`:15`), so NULL matches neither and shows only Home (G16). The top bar shows the user's initials, name and designation; the home page (`main.jsp`) greets the user and shows designation and e-mail (`main.jsp:15-20`). All of it goes through `<c:out>` (G27). The menu reads the session's `user`, not Spring Security's roles; both come from the same `admin` row.
- **Session id and cookie:**
  - The id changes at login (Spring Security's session fixation protection), so an id set before login is worthless (`SecurityConfigTest.validLoginRedirectsToHomeWithProfileInANewSession`).
  - The session holds the `SecurityContext` with the `RmsUserDetails`, whose password is erased after the login (`RmsUserDetails.eraseCredentials`; `SecurityConfigTest.passwordIsNotKeptAfterLogin`), plus the `user` attribute and the CSRF token.
  - Sessions are tracked by cookie only (`web.xml`, G41). The cookie is `JSESSIONID`, `HttpOnly`, `SameSite=Lax`. It isn't `Secure`; see open question #14.
  - `UserInfo` and `RmsUserDetails` are `Serializable` (G36). Tomcat 11's default doesn't persist sessions. Persisting them is an ops choice, with the data-protection note in [build-run.md](../build-run.md).
- **Caching:** pages behind the login get Spring Security's `Cache-Control: no-cache, no-store, max-age=0, must-revalidate` (`SecurityConfigTest.pagesBehindTheLoginAreNotCached`).
- **Not yet verified in a container:** the change was tested with MockMvc against the real `SecurityConfig` (31 tests) but not yet on Tomcat 11: the filter registration, the `_csrf` field in `login.jsp` and in the `layout.tag` logout form, and the updated `SmokeTest` (10 tests) still need a deployed run ([upgrade-status.md](../upgrade-status.md)).
- **Known gaps** (details in [gaps.md](../gaps.md)):
  - G11: fixed with Spring Security 7 on 2026-10-01 (was an interim interceptor since 2026-09-30).
  - G12: shared field, fixed 2026-09-30 (the field and `doLogin` are gone).
  - G13: partly fixed. The check is in Java and `{bcrypt}` rows work, but the stored passwords are still plain text until the owner migrates them.
  - G16: fixed 2026-10-01 (null-safe role checks).
  - G25: fixed 2026-10-01 (`/` → `/home`).
  - G26: fixed 2026-10-01 (no `admin` row → "Invalid login!"); now in `SecurityConfig`.
  - G27: fixed 2026-09-30 (output escaped with `<c:out>`).
  - G30: fixed. Since Spring Security a failed login redirects too.
  - G32: fixed 2026-10-01 (CSRF tokens; logout is a POST).
  - G36: fixed 2026-10-01 (`UserInfo` is `Serializable`).
  - G41: fixed 2026-10-01 (cookie-only sessions).
