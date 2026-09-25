# Conventions

Purpose: The coding patterns RMS actually uses, so new code matches existing code.
Last updated: 2026-09-25
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
  - List pages use DataTables with Update/Delete links (`viewposition.jsp`, `viewlanguage.jsp`).
- **Style:** tab indentation. Eclipse "Auto-generated method stub" TODO comments are left in place (`rms/service/*Impl.java`, `rms/dao/*Impl.java`).
- **Git:**
  - `master` is the default branch; work happens on `dev` (`git branch -a`).
  - Commit authors are recorded as `rafitsabbir` (`git log`).
