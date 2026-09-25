# RMS — Recruitment Management System

"Simple Recruitment Management System" (`README.md`). A single-module Spring MVC + JSP web app packaged as a WAR.

## Structure
- A single Maven module. There are no subprojects (`pom.xml`).
- `src/main/java/rms/` has 4 subpackages: `config`, `controller`, `dao`, `model`, `service`. It holds 26 Java files.
- `src/main/webapp/`: 7 JSP views in `WEB-INF/jsp/`, plus `index.jsp`, `WEB-INF/web.xml`, and `resources/` (CSS, JS, images).
- Size: about 1.9k lines across the `.java` and `.jsp` files. The repo tracks 166 files, and 106 of them are under `target/`.
- `target/` (build output) is committed. There is no `.gitignore`.
- Eclipse project files are committed: `.project`, `.classpath`, `.settings/`.

## Build & runtime
- Built with Maven. The artifact is `com.sabbir:rmsv2:1.0.1-SNAPSHOT` with `<packaging>war</packaging>` (`pom.xml`).
- The only build plugin configured is `maven-war-plugin` 2.3, with `failOnMissingWebXml=false` (`pom.xml`).
- The Java version isn't set in `pom.xml`: there's no `maven.compiler` property and no compiler plugin. Eclipse settings target Java 1.5 (`.settings/org.eclipse.jdt.core.prefs`, `.classpath`).
- Build command: `mvn package` produces `target/rmsv2-1.0.1-SNAPSHOT.war`. Deploy it to a Servlet 3.1 container; `javax.servlet-api` 3.1.0 is marked `provided`.
- There is no embedded server or `main()` method. The app is started by a servlet container.

## Tech stack (`pom.xml`)
- Spring 4.3.0.RELEASE: `spring-webmvc`, `spring-web`, `spring-context`, `spring-jdbc`, `spring-tx`.
- JSTL 1.2 and Servlet API 3.1.0.
- MySQL Connector/J 5.1.36.
- No ORM (no Hibernate or JPA), no logging library, and no test library.

## Entry points
- `rms.config.WebInitializer` extends `AbstractAnnotationConfigDispatcherServletInitializer`. It maps the DispatcherServlet to `/` and loads `WebConfig` as the root config.
- `rms.config.WebConfig` sets up `@EnableWebMvc` and `@ComponentScan("rms")`. It serves static files from `/resources/**` and resolves views from `/WEB-INF/jsp/*.jsp`.
- `WEB-INF/web.xml` is an empty archetype stub (DTD 2.3) that only sets `display-name`.
- Controllers (`rms/controller/`):
  - `LoginController`: `GET /login`, `POST /welcome`
  - `LanguageController`: `/createlanguage`, `/viewlanguagelist`, `POST /savelanguage`, `/updatelanguage/{languagekey}`, `/deletelanguage/{languagekey}`
  - `PositionController`: `/createposition`, `POST /saveposition`, `/viewpositionlist`, `/updateposition/{positionkey}`, `/deleteposition/{positionkey}`
  - `MarksController`: `/adminviewmarks`
- There are no `@Scheduled` jobs or batch runners.

## Configuration
- There are no `.properties` or `.yml` files and no Spring profiles. All configuration is in Java code (`WebConfig.java`).
- The DataSource is looked up through JNDI at `java:comp/env/jdbc/springrms` (`WebConfig.getDataSource`). The actual connection settings are defined in the servlet container, not in this repo.
- There are no external integrations beyond the database.

## Data layer
- MySQL, based on the connector dependency.
- Plain JDBC through `NamedParameterJdbcTemplate`, using hand-written SQL strings in the `*DaoImpl` classes.
- Tables referenced: `users`, `admin`, `language`, `position`, plus the marks query in `MarksDaoImpl`.
- There are no migration scripts, no schema or DDL files, and no stored procedure calls.
- Login checks `username` and `password` directly in SQL: `select userid from users where username= :username and password= :password` (`LoginDaoImpl`).

## Tests
- There are none: no `src/test/` directory and no test dependencies. `.classpath` points to `src/test/java`, but that folder doesn't exist.

## Conventions
- Package names start with `rms.`, with one layer per package: `controller` → `service` → `dao` → `model`.
- Each service and DAO has an interface plus an `*Impl` class: `LanguageService`/`LanguageServiceImpl`, `PositionDao`/`PositionDaoImpl`, and so on.
- Model classes are named `*Info`: `UserInfo`, `LanguageInfo`, `PositionInfo`, `MarksInfo`.
- Each controller has a class-level `@RequestMapping("/")` and uses `@RequestMapping(value, method)` on its methods. It doesn't use `@GetMapping` or `@PostMapping`.
- Deletes are sent as GET requests, as in `/deletelanguage/{key}`.
- Indentation is tabs. Some Eclipse "Auto-generated method stub" TODO comments are left in place.

## Git context
- 5 commits, from 2019-11-24 to 2020-01-17. The latest is `e5705c0 add candidat position`.
- Contributors: `rafitsabbir` (4 commits) and `A. K Sabbir Ahamed Rafit` (1 commit), which appear to be the same person.
- Branches:
  - Local: `master` and `dev`. `dev` was created from `master` on 2026-09-25.
  - Remote, besides `master`: three unmerged Dependabot branches that bump `mysql-connector-java` to 8.0.28, `spring-web` to 6.0.0, and `spring-webmvc` to 5.2.20.RELEASE.

## Open Questions
- Which Java version is actually intended? Eclipse says 1.5, but Spring 4.3 needs Java 6 or later, and `pom.xml` doesn't set a compiler level.
- Which servlet container is the target (Tomcat?), and where is the JNDI resource `jdbc/springrms` defined? That definition isn't in the repo.
- Where does the DB schema come from? No DDL or migration scripts are tracked.
- Does `mvn package` succeed as-is with a modern JDK? It hasn't been run, and the default compiler level may be too old.
- Are passwords stored in plain text? The login SQL compares the password column directly.
- Should `target/` be tracked in git? No `.gitignore` exists.
- Is there any session or auth check on the admin routes besides `/welcome`? The controllers haven't been reviewed in depth.
