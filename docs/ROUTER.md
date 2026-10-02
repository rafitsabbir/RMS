# Docs Router

Purpose: Points each kind of task to the minimum set of docs and code to read.
Last updated: 2026-10-01 (Spring Security 7 replaces AuthInterceptor). 2026-10-01 (UI redesign: `layout.tag` replaces the `main.jsp` menu shell; new UI row). 2026-10-01 (CI workflows and the Tomcat context template). 2026-10-01 (Phase 1 release runbook; Phase 3: `jakarta.servlet`)
Read this when: you're starting any task. Start here, not with every doc.

**Rule:** read only the files listed for your task. Open other docs only if those files point you there, or the task clearly spans categories.

| Task / Question type | Read these files | Key entry points in code |
|---|---|---|
| Fixing a bug in a business flow | [business-flows/README.md](business-flows/README.md), then the one module file ([login](business-flows/login.md), [masters](business-flows/masters.md), [evaluation](business-flows/evaluation.md)); [open-questions.md](open-questions.md) if the area is marked partial | `rms/controller/<X>Controller.java` → `rms/service/<X>ServiceImpl.java` → `rms/dao/<X>DaoImpl.java`, `WEB-INF/jsp/<view>.jsp`, `src/test/java/rms/**/<X>*Test.java` |
| Adding a new feature or screen | [conventions.md](conventions.md), [business-flows/masters.md](business-flows/masters.md) (template pattern), [architecture.md](architecture.md) | `PositionController.java` (CRUD template), `WEB-INF/tags/layout.tag` (sidebar menu; `active` key per page), `main.jsp` (home tiles) |
| DB / SQL change | [data-model.md](data-model.md), then the affected module in business-flows/ | `rms/dao/*DaoImpl.java` (SQL string fields and `RowMapper`s), `rms/model/*Info.java`, `db/schema.sql` (inferred), `db/local/` (local DB setup), `src/test/java/rms/dao/` |
| Build, deploy or environment issue | [build-run.md](build-run.md), [tech-stack.md](tech-stack.md) | `pom.xml`, `mvnw`, `.mvn/wrapper/`, `.github/workflows/`, `deploy/tomcat/rms.xml`, `rms/config/WebInitializer.java`, `WebConfig.getDataSource` |
| Config or integration change | [config.md](config.md) | `rms/config/WebConfig.java`, `src/main/webapp/WEB-INF/web.xml` |
| Resuming upgrade work / "what's next?" | [upgrade-status.md](upgrade-status.md) first, then the phase in [modernization-plan.md](modernization-plan.md); [acceptance/README.md](acceptance/README.md) to sign off a phase | `pom.xml`, `src/test/java/rms/`, `db/`, `db/local/README.md` |
| Upgrading libraries/JDK/framework | [upgrade-status.md](upgrade-status.md), [modernization-plan.md](modernization-plan.md), [tech-stack.md](tech-stack.md), [conventions.md](conventions.md) | `pom.xml`, `WebConfig.java`, `LoginController.java` and `SecurityConfig.java` (the `jakarta.servlet` users), `WEB-INF/web.xml`, JSP `<head>` CDN links |
| Releasing a phase / deploying to production / rollback | [release-phase1.md](release-phase1.md), then [build-run.md](build-run.md) and [acceptance/README.md](acceptance/README.md) | `pom.xml`, the WAR from `./mvnw -B verify` (or the CI artifact), `deploy/tomcat/rms.xml`, `src/test/java/rms/SmokeTest.java` |
| Dependency upgrade / security fix | [upgrade-status.md](upgrade-status.md), [modernization-plan.md](modernization-plan.md) (phase and gates first), then [tech-stack.md](tech-stack.md), [build-run.md](build-run.md) | `pom.xml`, `src/test/java/rms/` (the characterization tests must stay green) |
| Understanding the overall system | [architecture.md](architecture.md), [business-flows/README.md](business-flows/README.md) | `WebConfig.java`, `rms/controller/` |
| Changing a page's look, the menu or front-end libraries | [conventions.md](conventions.md) ("JSP views and UI"), [tech-stack.md](tech-stack.md) (front-end row), [config.md](config.md) (static resources, CDNs) | `WEB-INF/tags/layout.tag`, `resources/css/rms.css`, `resources/js/rms.js`, `resources/img/`, `WEB-INF/jsp/*.jsp` |
| Code review / following conventions | [conventions.md](conventions.md), plus the "Cross-cutting concerns" section of [architecture.md](architecture.md) | the changed files, and their tests in `src/test/java/rms/` |
| Auth / security change | [business-flows/login.md](business-flows/login.md), plus the "Cross-cutting concerns" section of [architecture.md](architecture.md) | `SecurityConfig.java` (rules, login, CSRF, logout, password check), `SecurityInitializer.java`, `LoginServiceImpl.java` + `RmsUserDetails.java`, `LoginDaoImpl.java`, `LoginController.java`, `login.jsp`, `layout.tag` (role-based menu, Log out form), `SecurityConfigTest` |
| Working on incomplete/missing features | [gaps.md](gaps.md), then the relevant [business-flows/](business-flows/README.md) file | `layout.tag` ("Coming soon" entries for unbuilt modules), `MarksDaoImpl.java` (stubs) |
| Writing or running tests | [build-run.md](build-run.md) (Test section), [conventions.md](conventions.md) (Tests section) | `src/test/java/rms/`, `rms/config/SecurityConfigTest.java` (security, MockMvc), `rms/dao/MySqlContainerSupport.java`, `db/schema.sql`, `db/test-seed.sql` |
| Unclear behaviour or missing module | [open-questions.md](open-questions.md) | `layout.tag` ("Coming soon" entries for unbuilt modules) |

## Subagents
Project subagents are defined in `.claude/agents/`. Each one starts by reading this router.

| Task type | Agent | Edits files? |
|---|---|---|
| "Where is X / how does Y work", exploring modules, SQL, config or git history | `repo-explorer` | No |
| Tracing or documenting one business capability end to end, with a Mermaid diagram | `business-flow-tracer` | No |
| Finding partial, stubbed, dead or missing features; refreshing [gaps.md](gaps.md) | `gap-analyzer` | No |
| Merging confirmed findings into docs, deduplicating, keeping `CLAUDE.md` lean and this router complete | `docs-keeper` | `.md` only |
| Reviewing a diff or branch before commit or merge | `java-code-reviewer` | No |

Typical chain: `repo-explorer` / `business-flow-tracer` / `gap-analyzer` → `docs-keeper`; code change → `java-code-reviewer`.

## Keeping docs current
- **Where to write:** when a task confirms or changes a fact, update the one category file that owns it and bump its "Last updated" date.
- **Open questions:** move an item out of [open-questions.md](open-questions.md) once it's resolved.
- **Evidence:** every claim needs an evidence file path.
