# Docs Router

Purpose: Points each kind of task to the minimum set of docs and code to read.
Last updated: 2026-09-25
Read this when: you're starting any task. Start here, not with every doc.

**Rule:** read only the files listed for your task. Open other docs only if those files point you there, or the task clearly spans categories.

| Task / Question type | Read these files | Key entry points in code |
|---|---|---|
| Fixing a bug in a business flow | [business-flows/README.md](business-flows/README.md), then the one module file ([login](business-flows/login.md), [masters](business-flows/masters.md), [evaluation](business-flows/evaluation.md)); [open-questions.md](open-questions.md) if the area is marked partial | `rms/controller/<X>Controller.java` → `rms/service/<X>ServiceImpl.java` → `rms/dao/<X>DaoImpl.java`, `WEB-INF/jsp/<view>.jsp` |
| Adding a new feature or screen | [conventions.md](conventions.md), [business-flows/masters.md](business-flows/masters.md) (template pattern), [architecture.md](architecture.md) | `PositionController.java` (CRUD template), `main.jsp` (menu `load_*()` functions and `spring:url` values) |
| DB / SQL change | [data-model.md](data-model.md), then the affected module in business-flows/ | `rms/dao/*DaoImpl.java` (SQL string fields and `RowMapper`s), `rms/model/*Info.java` |
| Build, deploy or environment issue | [build-run.md](build-run.md), [tech-stack.md](tech-stack.md) | `pom.xml`, `rms/config/WebInitializer.java`, `WebConfig.getDataSource` |
| Config or integration change | [config.md](config.md) | `rms/config/WebConfig.java`, `src/main/webapp/WEB-INF/web.xml` |
| Dependency upgrade / security fix | [tech-stack.md](tech-stack.md), [build-run.md](build-run.md) | `pom.xml` |
| Understanding the overall system | [architecture.md](architecture.md), [business-flows/README.md](business-flows/README.md) | `WebConfig.java`, `rms/controller/` |
| Code review / following conventions | [conventions.md](conventions.md), plus the "Cross-cutting concerns" section of [architecture.md](architecture.md) | the changed files |
| Auth / security change | [business-flows/login.md](business-flows/login.md), plus the "Cross-cutting concerns" section of [architecture.md](architecture.md) | `LoginController.java`, `LoginDaoImpl.java`, `main.jsp` |
| Unclear behaviour or missing module | [open-questions.md](open-questions.md) | `main.jsp` (menu links to unbuilt modules) |

## Keeping docs current
- **Where to write:** when a task confirms or changes a fact, update the one category file that owns it and bump its "Last updated" date.
- **Open questions:** move an item out of [open-questions.md](open-questions.md) once it's resolved.
- **Evidence:** every claim needs an evidence file path.
