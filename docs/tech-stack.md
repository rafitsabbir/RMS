# Tech Stack

Purpose: Every technology and version in use, with evidence.
Last updated: 2026-09-26
Read this when: you're upgrading dependencies, checking compatibility, or answering "what does RMS use for X?".

| Layer | Technology | Version | Evidence |
|---|---|---|---|
| Language/JDK | Java 1.8 source/target, set in `pom.xml`; built with JDK 8 (Temurin 1.8.0_504 on 2026-09-26). The WAR formerly committed on `master` was built with JDK 1.8.0_221 and has Java 5 bytecode (class version 49). Eclipse `.settings` still say 1.5 | 1.8 | `pom.xml:16-17`, `master:target/rmsv2-1.0.1-SNAPSHOT.war` → `META-INF/MANIFEST.MF`, `.settings/org.eclipse.jdt.core.prefs` |
| Build | Maven 3.9.16 via the Maven Wrapper; compiler 3.13.0, surefire 3.5.3 and war 3.4.0 plugins; Java 1.8 level, UTF-8 | 3.9.16 | `.mvn/wrapper/maven-wrapper.properties`, `pom.xml:16-18,131-155` |
| Runtime | External Servlet 3.1 container; which one isn't named | Servlet API 3.1.0 (provided) | `pom.xml`, `rms/config/WebInitializer.java` |
| Web/UI | JSP with scriptlets, JSTL, Spring form/url tags | JSTL 1.2 | `pom.xml`, `WEB-INF/jsp/*.jsp` |
| Front-end libraries (CDN) | Bootstrap 3.3.7 and 4.1.1, jQuery 3.2.1/3.3.1, DataTables 1.10.19, Font Awesome 4.7.0 | as listed | `login.jsp:13-18`, `create*.jsp:10-14`, `view*.jsp:12-21`, `main.jsp:26` |
| Framework | Spring MVC with Java config | 4.3.0.RELEASE | `pom.xml`, `rms/config/WebConfig.java` |
| Persistence | Spring JDBC `NamedParameterJdbcTemplate`, no ORM | 4.3.0.RELEASE | `WebConfig.java`, `rms/dao/*DaoImpl.java` |
| Database driver | MySQL Connector/J | 5.1.36 | `pom.xml` |
| Messaging/Integration | Not found | — | `pom.xml` |
| Scheduling/Batch | Not found | — | `src/main/java` (no `@Scheduled`) |
| Reporting | Not found | — | `pom.xml` |
| Logging | No framework; `System.out.println`. `commons-logging` 1.2 is pulled in by Spring | — | `rms/dao/*DaoImpl.java`, WAR `WEB-INF/lib/` |
| Security/Auth | Home-made (SQL check plus session attribute); no framework | — | `LoginController`, `LoginDaoImpl` |
| Testing | JUnit 5 (versions aligned by `junit-bom`), Mockito, AssertJ, spring-test standalone MockMvc, Testcontainers MySQL (test scope only; characterization tests) | JUnit 5.13.4, Mockito 4.11.0, AssertJ 3.27.3, Testcontainers 1.21.4 | `pom.xml:19-38,93-130`, `src/test/java/rms/` |
| Packaging | WAR file `rmsv2-1.0.1-SNAPSHOT` | — | `pom.xml` |

**Libraries bundled in the WAR** (`WEB-INF/lib/`, the same in the old `master` WAR and in a fresh `mvnw verify` build; `jar tf`): spring-aop, spring-beans, spring-context, spring-core, spring-expression, spring-jdbc, spring-tx, spring-web and spring-webmvc (all 4.3.0.RELEASE), jstl-1.2, mysql-connector-java-5.1.36, commons-logging-1.2.

**Pending upgrades:** there are three unmerged Dependabot branches on GitHub (`git branch -r`):
- One bumps `mysql-connector-java` to 8.0.28.
- The two named after `spring-web` 6.0.0 and `spring-webmvc` 5.2.20.RELEASE each change the shared `spring.version` property (`pom.xml:13`), so each would move **every** Spring module. The 6.0.0 branch can't compile on this codebase (see B8 in [modernization-plan.md](modernization-plan.md)).

**Support status and upgrade path:** see [modernization-plan.md](modernization-plan.md). Spring 4.3 support ended in 2020, and the target is Spring 7.0 on JDK 21 and Tomcat 11, going through 5.3.
