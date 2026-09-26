# Conventions

Purpose: The coding patterns RMS actually uses, so new code matches existing code.
Last updated: 2026-09-26
Read this when: you're adding or reviewing code, or creating a new screen or module.

## Package & naming
| Convention | Example | Evidence |
|---|---|---|
| Base package `rms`, one package per layer | `rms.{config, controller, service, dao, model}` | `src/main/java/rms/` |
| Service and DAO are an interface plus an `*Impl` class | `PositionService` / `PositionServiceImpl`, `PositionDao` / `PositionDaoImpl` | `rms/service`, `rms/dao` |
| Models named `*Info` | `UserInfo`, `PositionInfo`, `LanguageInfo`, `MarksInfo` | `rms/model` |
| Primary keys named `<entity>key` (int) | `positionkey`, `languagekey` | `PositionInfo`, `LanguageInfo` |
| Lowercase JSP names in the form `create<x>.jsp` / `view<x>.jsp` | `createposition.jsp`, `viewposition.jsp` | `WEB-INF/jsp/` |
| Lowercase URLs without separators | `/createposition`, `/viewpositionlist`, `/saveposition`, `/updateposition/{positionkey}`, `/deleteposition/{positionkey}` | `PositionController`, `LanguageController` |

## Coding patterns
- **Controllers:** class-level `@RequestMapping("/")`, and `@RequestMapping(value, method = RequestMethod.X)` on methods (no `@GetMapping` or `@PostMapping`). They return `ModelAndView` and inject services with `@Autowired` on a field (`rms/controller/*`).
- **Save handling:** one `save` endpoint does both create and update. A key greater than 0 means update; otherwise insert. It then redirects to the list page (`PositionController.save`, `LanguageController.save`).
- **Services:** DAOs are injected with an `@Autowired` setter. The methods only pass calls through (`rms/service/*Impl.java`).
- **DAOs:**
  - SQL lives in `String` fields at the top of the class, using named `:params`.
  - Parameters go in a `HashMap` paramMap.
  - Each DAO has a `private static final class XMapper implements RowMapper<XInfo>`.
  - `NamedParameterJdbcTemplate` is injected with an `@Autowired` setter (`rms/dao/*Impl.java`).
- **Views:**
  - JSPs use scriptlets as well as JSTL.
  - URLs are built with `<spring:url>`.
  - **EL opt-in:** 6 of the 7 JSPs declare `isELIgnored="false"` (lines 1-2), because `web.xml` uses the Servlet 2.3 DTD, where EL is off by default. `viewmarks.jsp` doesn't opt in and uses no EL, so keep the attribute on new JSPs (`WEB-INF/jsp/*.jsp`, `web.xml:1-3`, G37).
  - List pages use DataTables with Update/Delete links (`viewposition.jsp`, `viewlanguage.jsp`).
- **Logging:** use SLF4J, with `private static final Logger log = LoggerFactory.getLogger(X.class);` as the first class field and `{}` placeholders (`PositionDaoImpl.java:22,73`). No new `System.out`. Never log passwords or other personal data. `logback.xml` replaces CR/LF in messages, so logged user input can't forge log lines.
- **Front-end libraries:** load them from a pinned CDN version with an SRI `integrity` hash (sha384) and `crossorigin="anonymous"`, as in every JSP head since Phase 1 (`viewposition.jsp:11-21`). Compute the hash from the exact file, e.g. `curl -s URL | openssl dgst -sha384 -binary | openssl base64 -A`.
- **MVC config:** implement `WebMvcConfigurer`, not the deprecated `WebMvcConfigurerAdapter` (`WebConfig.java:21`).
- **Style:** tab indentation. Eclipse "Auto-generated method stub" TODO comments are left in place (`rms/service/*Impl.java`, `rms/dao/*Impl.java`).
- **Git:**
  - `master` is the default branch; work happens on `dev` (`git branch -a`).
  - Commit authors are recorded as `rafitsabbir` (`git log`).

## Tests
- **Kind:** characterization tests. They pin what the code does today, including known defects, so an upgrade that changes behaviour fails a test. Mark a test that pins a defect with a comment naming the gap, e.g. `// characterizes G14` (`src/test/java/rms/dao/PositionDaoImplTest.java`).
- **Layout and naming:** `src/test/java/rms/<layer>/<Class>Test.java`, package-private JUnit 5 classes, tab indentation, AssertJ assertions.
- **Controllers:** standalone MockMvc with Mockito `@Mock` services and `@InjectMocks` into the `@Autowired` fields. Pass `new WebConfig().viewResolver()` so view names resolve as in production (`src/test/java/rms/controller/PositionControllerTest.java`).
- **DAOs:** extend `MySqlContainerSupport`. It recreates the schema from `db/schema.sql` and `db/test-seed.sql` before every test, against a throwaway MySQL container, and is skipped without Docker. Set `RMS_REQUIRE_DOCKER=true` where Docker must be present, so a missing Docker fails the build instead of skipping. Wire the DAO through its setter (`src/test/java/rms/dao/MySqlContainerSupport.java`).
- **Why `MySqlContainerSupport` manages the container itself** (both confirmed by `mvnw` runs on 2026-09-26; don't "simplify" back):
  - `@Testcontainers(disabledWithoutDocker = true)` disables the class before any `@BeforeAll` runs, so a `RMS_REQUIRE_DOCKER` gate there never fires.
  - An assumption failing in `@BeforeAll` is reported by surefire as "Tests run: 0", not as skipped, which hides the gap. So `@BeforeAll` only starts the container, and the skip is an `assumeTrue` in `@BeforeEach`.
- **Mocks:** `@InjectMocks` fills the controllers' package-private `@Autowired` fields, so the controllers need no changes to be testable (`src/test/java/rms/controller/*Test.java`).
- **Data:** only synthetic values from `db/test-seed.sql` or the test itself. No real credentials, hostnames or production data.

## Known deviations (fix in passing, don't copy)
- `WebConfig.java:23-24` injects the `DataSource` with `@Autowired` into the same config class that creates it. Prefer a method parameter: `getNamedParameterJdbcTemplate(DataSource ds)`.
- `main.jsp:34` sets the `user` session attribute again, although `LoginController.java:45` already set it.
- Parameter-name typos: `fositioninfo` (`PositionDao.java:9,11`) and `irstname` (`UserInfo.java:71`).
- Security and robustness issues are tracked as gaps in [gaps.md](gaps.md), not here.
