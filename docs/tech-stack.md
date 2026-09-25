# Tech Stack

Purpose: Every technology and version in use, with evidence.
Last updated: 2026-09-25
Read this when: you're upgrading dependencies, checking compatibility, or answering "what does RMS use for X?".

| Layer | Technology | Version | Evidence |
|---|---|---|---|
| Language/JDK | Java. Not set in `pom.xml`; the committed WAR was built with JDK 1.8.0_221; Eclipse is set to 1.5 | 1.8 (build) | `target/rmsv2-1.0.1-SNAPSHOT.war` → `META-INF/MANIFEST.MF`, `.settings/org.eclipse.jdt.core.prefs` |
| Build | Maven with `maven-war-plugin` (committed WAR built with Maven 3.6.1) | plugin 2.3 | `pom.xml`, WAR `MANIFEST.MF` |
| Runtime | External Servlet 3.1 container; which one isn't named | Servlet API 3.1.0 (provided) | `pom.xml`, `rms/config/WebInitializer.java` |
| Web/UI | JSP with scriptlets, JSTL, Spring form/url tags | JSTL 1.2 | `pom.xml`, `WEB-INF/jsp/*.jsp` |
| Front-end libraries (CDN) | Bootstrap 3.3.7 and 4.1.1, jQuery 3.2.1/3.3.1, DataTables 1.10.19, Font Awesome 4.7.0 | as listed | `WEB-INF/jsp/*.jsp` |
| Framework | Spring MVC with Java config | 4.3.0.RELEASE | `pom.xml`, `rms/config/WebConfig.java` |
| Persistence | Spring JDBC `NamedParameterJdbcTemplate`, no ORM | 4.3.0.RELEASE | `WebConfig.java`, `rms/dao/*DaoImpl.java` |
| Database driver | MySQL Connector/J | 5.1.36 | `pom.xml` |
| Messaging/Integration | Not found | — | `pom.xml` |
| Scheduling/Batch | Not found | — | `src/main/java` (no `@Scheduled`) |
| Reporting | Not found | — | `pom.xml` |
| Logging | No framework; `System.out.println`. `commons-logging` 1.2 is pulled in by Spring | — | `rms/dao/*DaoImpl.java`, WAR `WEB-INF/lib/` |
| Security/Auth | Home-made (SQL check plus session attribute); no framework | — | `LoginController`, `LoginDaoImpl` |
| Testing | Not found | — | No `src/test/`, no test dependencies in `pom.xml` |
| Packaging | WAR file `rmsv2-1.0.1-SNAPSHOT` | — | `pom.xml` |

**Libraries bundled in the committed WAR** (`WEB-INF/lib/`): spring-aop, spring-beans, spring-context, spring-core, spring-expression, spring-jdbc, spring-tx, spring-web and spring-webmvc (all 4.3.0.RELEASE), jstl-1.2, mysql-connector-java-5.1.36, commons-logging-1.2.

**Pending upgrades:** three unmerged Dependabot branches on GitHub bump `mysql-connector-java` to 8.0.28, `spring-web` to 6.0.0 and `spring-webmvc` to 5.2.20.RELEASE (`git branch -r`).
