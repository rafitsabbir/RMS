# Conventions

Purpose: The coding patterns RMS actually uses, so new code matches existing code.
Last updated: 2026-10-01 (small fixes batch: save handling with `errorMessage`, state changes are POST, interceptor registration rule, line references, the `characterizes` example, the `main.jsp:35` deviation removed). 2026-10-01 (Phase 3: EL, Jakarta Tags URI, request encoding, Mockito agent; after the review: keep `web.xml`, parameter names). 2026-09-30 (output escaping rule after G27/G38)
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
  - **Refusals show the form again:** a blank name, or a business duplicate, returns the create view with the entered object and an `errorMessage` model attribute, which the create JSP prints through `<c:out>` (`PositionController.form`, `:95-101`; `createposition.jsp:28-34`). Only a successful save redirects to the list.
  - **DAO `add*`/`update*` return a boolean** for business duplicates (`false` = refused), not an exception. A missing or deleted key is `null` from the finder, which the controller turns into `ResponseStatusException(NOT_FOUND)`.
  - **State changes are POST.** Saves and deletes use `RequestMethod.POST`; a GET is refused with 405. List pages render Delete as a POST form with a link-style button (`viewposition.jsp:55-60`), not as a link. There's no CSRF token yet (G32).
- **Services:** DAOs are injected with an `@Autowired` setter. The methods only pass calls through (`rms/service/*Impl.java`).
- **DAOs:**
  - SQL lives in `String` fields at the top of the class, using named `:params`.
  - Parameters go in a `HashMap` paramMap.
  - Each DAO has a `private static final class XMapper implements RowMapper<XInfo>`.
  - `NamedParameterJdbcTemplate` is injected with an `@Autowired` setter (`rms/dao/*Impl.java`).
- **Views:**
  - JSPs use scriptlets as well as JSTL.
  - URLs are built with `<spring:url>`.
  - **Output escaping:** print user or database text through `<c:out>`, never as raw `${…}` or `<%= %>` (G27):
    - EL: `<c:out value="${x.name}"/>` (`viewposition.jsp:50`).
    - Scriptlet: `<c:out value="<%=x.getName()%>"/>` (`viewmarks.jsp:81-83`, `main.jsp:81,84,87`).
    - Spring `form:` tags already escape by default. Never write user data into a JavaScript string or URL; take the user from the session instead (G38).
    - A `<spring:url>` value that goes into a JavaScript string gets `htmlEscape="true" javaScriptEscape="true"` (`main.jsp:12-21`).
  - **EL:** since Phase 3, `web.xml` uses the Servlet 6.1 schema, so EL is on by default (G37 fixed). All 7 JSPs still declare `isELIgnored="false"` from the Servlet 2.3 days; it's harmless.
  - **JSTL:** the taglib URI is `jakarta.tags.core` (Jakarta Tags 3.0), not `http://java.sun.com/jsp/jstl/core`.
  - **Request encoding:** `web.xml` pins `request-character-encoding` to ISO-8859-1 to match the JSPs' `pageEncoding`. Change both together (G34). Don't delete `web.xml`: without the pin Tomcat 11 answers Latin-1 form posts with HTTP 400 (`SmokeTest.latin1FormPostIsAccepted`).
  - List pages use DataTables 1.13.11 with its Bootstrap 3 integration, an Update link and a Delete POST form (`viewposition.jsp`, `viewlanguage.jsp`). Load one jQuery, then Bootstrap JS, then DataTables; `resources/css/datatables.css` goes after the CDN CSS (G31, G34).
  - **Role checks are null-safe:** write `"N".equalsIgnoreCase(x.getIsinterviewer())`, never `x.getIsinterviewer().equals…` (`main.jsp:42,67`, `viewmarks.jsp:96,98`, G16).
- **Access control:** `AuthInterceptor` is registered twice in `WebConfig.addInterceptors` (`WebConfig.java:47-54`). A new page is admin-only by default, because the second registration covers every path except `/`, `/home`, `/login`, `/welcome` and `/resources/**`. A page that any logged-in user may open (the interviewer's, G5) must be added to the login-only registration (`WebConfig.java:50`), and `AuthInterceptorTest.eachPathHasOneCheck` extended. New public paths go into the exclusion list on the admin-only registration (`:53`).
- **Logging:** use SLF4J, with `private static final Logger log = LoggerFactory.getLogger(X.class);` as the first class field and `{}` placeholders (`PositionDaoImpl.java:23,82`). No new `System.out` (none is left in `src`). Never log passwords or other personal data. `logback.xml` replaces CR/LF in messages, so logged user input can't forge log lines.
- **Front-end libraries:** load them from a pinned CDN version with an SRI `integrity` hash (sha384) and `crossorigin="anonymous"`, as in every JSP head since Phase 1 (`viewposition.jsp:11-24`). Compute the hash from the exact file, e.g. `curl -s URL | openssl dgst -sha384 -binary | openssl base64 -A`.
- **MVC config:** implement `WebMvcConfigurer`, not the deprecated `WebMvcConfigurerAdapter` (`WebConfig.java:22`).
- **Style:** tab indentation. Eclipse "Auto-generated method stub" TODO comments are left in place (`rms/service/*Impl.java`, `rms/dao/*Impl.java`).
- **Git:**
  - `master` is the default branch; work happens on `dev` (`git branch -a`).
  - Commit authors are recorded as `rafitsabbir` (`git log`).

## Tests
- **Kind:** characterization tests. They pin what the code does today, including known defects, so an upgrade that changes behaviour fails a test. Mark a test that pins a defect with a comment naming the gap, e.g. `// characterizes G10` (`src/test/java/rms/dao/MarksDaoImplTest.java:31`). Once a defect is fixed, the test pins the new behaviour and the comment goes (the G14 tests in `PositionDaoImplTest` now pin the refusal).
- **Layout and naming:** `src/test/java/rms/<layer>/<Class>Test.java`, package-private JUnit 5 classes, tab indentation, AssertJ assertions.
- **Mockito** runs as a `-javaagent` set in the surefire configuration (`pom.xml`), because JDK 21+ warns about, and later JDKs block, Mockito attaching itself at runtime. Keep the agent path quoted in `argLine`. IDE runs need the agent in the run configuration ([build-run.md](build-run.md)).
- **Controllers:** standalone MockMvc with Mockito `@Mock` services and `@InjectMocks` into the `@Autowired` fields. Pass `new WebConfig().viewResolver()` so view names resolve as in production (`src/test/java/rms/controller/PositionControllerTest.java`).
- **DAOs:** extend `MySqlContainerSupport`. It recreates the schema from `db/schema.sql` and `db/test-seed.sql` before every test, against a throwaway MySQL container, and is skipped without Docker. Set `RMS_REQUIRE_DOCKER=true` where Docker must be present, so a missing Docker fails the build instead of skipping. Wire the DAO through its setter (`src/test/java/rms/dao/MySqlContainerSupport.java`).
- **Why `MySqlContainerSupport` manages the container itself** (both confirmed by `mvnw` runs on 2026-09-26; don't "simplify" back):
  - `@Testcontainers(disabledWithoutDocker = true)` disables the class before any `@BeforeAll` runs, so a `RMS_REQUIRE_DOCKER` gate there never fires.
  - An assumption failing in `@BeforeAll` is reported by surefire as "Tests run: 0", not as skipped, which hides the gap. So `@BeforeAll` only starts the container, and the skip is an `assumeTrue` in `@BeforeEach`.
- **Mocks:** `@InjectMocks` fills the controllers' package-private `@Autowired` fields, so the controllers need no changes to be testable (`src/test/java/rms/controller/*Test.java`).
- **Data:** only synthetic values from `db/test-seed.sql` or the test itself. No real credentials, hostnames or production data.

## Known deviations (fix in passing, don't copy)
- `WebConfig.java:24-25` injects the `DataSource` with `@Autowired` into the same config class that creates it. Prefer a method parameter: `getNamedParameterJdbcTemplate(DataSource ds)`.
- Parameter-name typos: `fositioninfo` (`PositionDao.java:9,11`) and `irstname` (`UserInfo.java:67`).
- Security and robustness issues are tracked as gaps in [gaps.md](gaps.md), not here.
