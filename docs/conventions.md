# Conventions

Purpose: The coding patterns RMS actually uses, so new code matches existing code.
Last updated: 2026-10-01 (Spring Security 7 replaces AuthInterceptor). 2026-10-01 (UI redesign: the "JSP views and UI" section replaces "Views": layout tag, no scriptlets, rms.js/rms.css, icons, list and form pattern). 2026-10-01 (small fixes batch: save handling with `errorMessage`, state changes are POST, interceptor registration rule, line references, the `characterizes` example, the `main.jsp:35` deviation removed). 2026-10-01 (Phase 3: EL, Jakarta Tags URI, request encoding, Mockito agent; after the review: keep `web.xml`, parameter names). 2026-09-30 (output escaping rule after G27/G38)
Read this when: you're adding or reviewing code, or creating a new screen or module.

## Package & naming
| Convention | Example | Evidence |
|---|---|---|
| Base package `rms`, one package per layer | `rms.{config, controller, service, dao, model}` | `src/main/java/rms/` |
| Service and DAO are an interface plus an `*Impl` class | `PositionService` / `PositionServiceImpl`, `PositionDao` / `PositionDaoImpl` | `rms/service`, `rms/dao` |
| Models named `*Info` | `UserInfo`, `PositionInfo`, `LanguageInfo`, `MarksInfo` | `rms/model` |
| Primary keys named `<entity>key` (int) | `positionkey`, `languagekey` | `PositionInfo`, `LanguageInfo` |
| Lowercase JSP names in the form `create<x>.jsp` / `view<x>.jsp` | `createposition.jsp`, `viewposition.jsp` | `WEB-INF/jsp/` |
| Lowercase URLs without separators | `/createposition`, `/viewpositionlist`, `/saveposition` (POST), `/updateposition/{positionkey}`, `/deleteposition/{positionkey}` (POST) | `PositionController`, `LanguageController` |

## Coding patterns
- **Controllers:** class-level `@RequestMapping("/")`, and `@RequestMapping(value, method = RequestMethod.X)` on methods (no `@GetMapping` or `@PostMapping`). They return `ModelAndView` and inject services with `@Autowired` on a field (`rms/controller/*`).
- **Save handling:** one `save` endpoint does both create and update. A key greater than 0 means update; otherwise insert (`PositionController.java:36-61`, `LanguageController.java:45-70`).
  - **Refusals show the form again:** a blank name, or a business duplicate, returns the create view with the entered object and an `errorMessage` model attribute, which the create JSP prints through `<c:out>` (`PositionController.form`, `:95-101`; `createposition.jsp:20-25`). Only a successful save redirects to the list.
  - **DAO `add*`/`update*` return a boolean** for business duplicates (`false` = refused), not an exception. A missing or deleted key is `null` from the finder, which the controller turns into `ResponseStatusException(NOT_FOUND)`.
  - **State changes are POST.** Saves and deletes use `RequestMethod.POST`; a GET is refused with 405. List pages render Delete as a POST `<form:form>` with a button (`viewposition.jsp:39-43`), not as a link, so it carries the CSRF token (G32).
- **Services:** DAOs are injected with an `@Autowired` setter. The methods only pass calls through (`rms/service/*Impl.java`).
- **DAOs:**
  - SQL lives in `String` fields at the top of the class, using named `:params`.
  - Parameters go in a `HashMap` paramMap.
  - Each DAO has a `private static final class XMapper implements RowMapper<XInfo>`.
  - `NamedParameterJdbcTemplate` is injected with an `@Autowired` setter (`rms/dao/*Impl.java`).
- **JSP views and UI** (since the 2026-10-01 redesign):
  - **Page shell:** every page behind the login wraps its content in `<rms:layout title="…" active="…">` (`WEB-INF/tags/layout.tag`; taglib `<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>`). The tag renders the head, the sidebar menu, the top bar with the user and Log out, and `<main>`. Attributes: `title` (shown as "title - RMS"), `active` (the menu key: `home`, `marks`, `positions` or `languages`), `tables="true"` to load jQuery and DataTables, and `stylesheet` for one extra file in `resources/css` (`viewmarks.jsp:9`). `login.jsp` is the only page with its own `<head>`.
  - **New menu entries** go in `layout.tag`, inside the admin (`role eq 'N'`, `:65`) or interviewer (`role eq 'Y'`, `:91`) block. An unbuilt module is a disabled `nav-link` with a "Soon" label, not a link.
  - **No scriptlets:** JSPs use JSTL and EL only (no JSP has `<% %>` code since `f826830`). URLs are built with `<spring:url>`; the pages set `<spring:url value="/" var="base" htmlEscape="true" />` and build resource paths from `${base}`.
  - **Output escaping:** print user or database text through `<c:out>`, never as raw `${…}` (G27): `<c:out value="${x.name}"/>` (`viewposition.jsp:33`, `viewmarks.jsp:42-44`). Integer keys and scores may print as `${…}`. Spring `form:` tags already escape by default. Never write user data into a JavaScript string or URL; take the user from the session instead (G38).
  - **No inline scripts:** page behaviour lives in `resources/js/rms.js`, loaded by the layout. A `table[data-rms-table]` becomes a DataTable; a form with `data-rms-confirm="…"` asks before it posts (`rms.js:8-26`). Keep `rms.js` and `rms.css` ASCII (G34).
  - **Look:** Bootstrap 5 classes plus the `rms-*` classes and theme tokens in `resources/css/rms.css` (`:root` custom properties). Icons are `<svg class="rms-icon"><use href="${base}resources/img/icons.svg#name"/></svg>`, from the Bootstrap Icons sprite. Page headers use `rms-page-header`, content sits in `card rms-card`.
  - **List pages:** a header with an Add button, then a DataTable with an Actions column holding an Edit link and a Delete POST form with `data-rms-confirm` (`viewposition.jsp:11-50`). **Forms:** a labelled field, the `errorMessage` alert, and Save plus Cancel (`createposition.jsp:20-38`).
  - **EL:** since Phase 3, `web.xml` uses the Servlet 6.1 schema, so EL is on by default (G37 fixed). All 7 JSPs still declare `isELIgnored="false"`; it's harmless.
  - **JSTL:** the taglib URIs are `jakarta.tags.core` and `jakarta.tags.functions` (Jakarta Tags 3.0), not `http://java.sun.com/jsp/jstl/…`.
  - **Request encoding:** `web.xml` pins `request-character-encoding` to ISO-8859-1 to match the JSPs' `pageEncoding`. Change both together (G34). Don't delete `web.xml`: without the pin Tomcat 11 answers Latin-1 form posts with HTTP 400 (`SmokeTest.latin1FormPostIsAccepted`).
  - **Role and status checks are null-safe:** compare `fn:toUpperCase(x)` with `'N'`, `'Y'`, `'S'` or `'R'` in EL (`layout.tag:15`, `main.jsp:13`, `viewmarks.jsp:58,61`); `fn:toUpperCase` of NULL is empty, so NULL matches nothing (G16). In Java, write `"N".equalsIgnoreCase(x.getIsinterviewer())`.
- **Access control:** Spring Security's rules in `SecurityConfig.appFilterChain` (`SecurityConfig.java:64-72`). A new page is admin-only by default, because `anyRequest().hasRole("ADMIN")` covers every path no earlier rule names. A page that any logged-in user, or only interviewers, may open (the interviewer's, G5) needs its own `requestMatchers(…)` rule above `anyRequest()`, and a case in `SecurityConfigTest`. New public paths go next to `/login` (`:67`).
- **Forms and CSRF:** every POST needs Spring Security's CSRF token. Write forms with Spring's `<form:form>`, which adds the hidden `_csrf` field by itself; a plain `<form method="post">` gets 403 (G32). State-changing actions, logout included, are POSTs (`layout.tag:115`).
- **Login code:** don't read credentials in a controller. Spring Security handles `POST /welcome` and calls `LoginService.loadUserByUsername`; after the login the profile is in the session as `user` (`SecurityConfig.LoginSuccessHandler`).
- **Logging:** use SLF4J, with `private static final Logger log = LoggerFactory.getLogger(X.class);` as the first class field and `{}` placeholders (`PositionDaoImpl.java:23,82`). No new `System.out` (none is left in `src`). Never log passwords or other personal data. `logback.xml` replaces CR/LF in messages, so logged user input can't forge log lines.
- **Front-end libraries:** load them from a pinned CDN version with an SRI `integrity` hash (sha384) and `crossorigin="anonymous"`, as in `layout.tag:24-43` and `login.jsp:15-16`. Prefer jsDelivr npm paths and take the hash from the npm package file. Compute the hash from the exact file, e.g. `curl -s URL | openssl dgst -sha384 -binary | openssl base64 -A`.
- **MVC config:** implement `WebMvcConfigurer`, not the deprecated `WebMvcConfigurerAdapter` (`WebConfig.java:21`).
- **Style:** tab indentation. Eclipse "Auto-generated method stub" TODO comments are left in place (`rms/service/*Impl.java`, `rms/dao/*Impl.java`).
- **Git:**
  - `master` is the default branch; work happens on `dev` (`git branch -a`).
  - Commit authors are recorded as `rafitsabbir` (`git log`).

## Tests
- **Kind:** characterization tests. They pin what the code does today, including known defects, so an upgrade that changes behaviour fails a test. Mark a test that pins a defect with a comment naming the gap, e.g. `// characterizes G10` (`src/test/java/rms/dao/MarksDaoImplTest.java:31`). Once a defect is fixed, the test pins the new behaviour and the comment goes (the G14 tests in `PositionDaoImplTest` now pin the refusal).
- **Layout and naming:** `src/test/java/rms/<layer>/<Class>Test.java`, package-private JUnit 5 classes, tab indentation, AssertJ assertions.
- **Mockito** runs as a `-javaagent` set in the surefire configuration (`pom.xml`), because JDK 21+ warns about, and later JDKs block, Mockito attaching itself at runtime. Keep the agent path quoted in `argLine`. IDE runs need the agent in the run configuration ([build-run.md](build-run.md)).
- **Controllers:** standalone MockMvc with Mockito `@Mock` services and `@InjectMocks` into the `@Autowired` fields. Pass `new WebConfig().viewResolver()` so view names resolve as in production (`src/test/java/rms/controller/PositionControllerTest.java`).
- **Security:** `SecurityConfigTest` runs the real `SecurityConfig` with MockMvc and Spring Security's test support (`springSecurity()`, `csrf()`, `user(…)`), over the real controllers with mocked DAO and services. POSTs in such tests need `.with(csrf())`. Build the Spring context by hand (`AnnotationConfigWebApplicationContext` with a `MockServletContext` on `src/main/webapp`): spring-test 7's `SpringExtension` (`@SpringJUnitConfig`, `@SpringJUnitWebConfig`) needs JUnit 6, and the project is on JUnit 5.13.4. Moving to JUnit 6 would allow it.
- **DAOs:** extend `MySqlContainerSupport`. It recreates the schema from `db/schema.sql` and `db/test-seed.sql` before every test, against a throwaway MySQL container, and is skipped without Docker. Set `RMS_REQUIRE_DOCKER=true` where Docker must be present, so a missing Docker fails the build instead of skipping. Wire the DAO through its setter (`src/test/java/rms/dao/MySqlContainerSupport.java`).
- **Why `MySqlContainerSupport` manages the container itself** (both confirmed by `mvnw` runs on 2026-09-26; don't "simplify" back):
  - `@Testcontainers(disabledWithoutDocker = true)` disables the class before any `@BeforeAll` runs, so a `RMS_REQUIRE_DOCKER` gate there never fires.
  - An assumption failing in `@BeforeAll` is reported by surefire as "Tests run: 0", not as skipped, which hides the gap. So `@BeforeAll` only starts the container, and the skip is an `assumeTrue` in `@BeforeEach`.
- **Mocks:** `@InjectMocks` fills the controllers' package-private `@Autowired` fields, so the controllers need no changes to be testable (`src/test/java/rms/controller/*Test.java`).
- **Data:** only synthetic values from `db/test-seed.sql` or the test itself. No real credentials, hostnames or production data.

## Known deviations (fix in passing, don't copy)
- `WebConfig.java:23-24` injects the `DataSource` with `@Autowired` into the same config class that creates it. Prefer a method parameter: `getNamedParameterJdbcTemplate(DataSource ds)`.
- Parameter-name typos: `fositioninfo` (`PositionDao.java:9,11`) and `irstname` (`UserInfo.java:67`).
- Security and robustness issues are tracked as gaps in [gaps.md](gaps.md), not here.
