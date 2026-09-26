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
| Front-end libraries (CDN, every link pinned with an SRI `integrity` hash) | Bootstrap 3.4.1 and 4.6.2 (jsDelivr), jQuery 3.7.1 (code.jquery.com), DataTables 1.13.11, Font Awesome 4.7.0 (cdnjs). Upgraded in Phase 1 from Bootstrap 3.3.7/4.1.1, jQuery 3.2.1/3.3.1 and DataTables 1.10.19 | as listed | `login.jsp:13-18`, `create*.jsp:10-14`, `view*.jsp:12-21`, `main.jsp:26` |
| Framework | Spring MVC with Java config; all modules versioned by the imported `spring-framework-bom` | 5.3.39 (was 4.3.0.RELEASE until Phase 1) | `pom.xml:13,32-39`, `rms/config/WebConfig.java` |
| Persistence | Spring JDBC `NamedParameterJdbcTemplate`, no ORM | 5.3.39 | `WebConfig.java`, `rms/dao/*DaoImpl.java` |
| Database driver | MySQL Connector/J `com.mysql:mysql-connector-j`, with `protobuf-java` excluded (only needed for the X DevAPI). Driver class `com.mysql.cj.jdbc.Driver`. 8.2.0 is the newest release that supports MySQL 5.7 servers; 8.3.0+ need 8.0+ (release notes). Chosen while the server version is unknown (open question #16) | 8.2.0 (was `mysql:mysql-connector-java` 5.1.36 until Phase 1) | `pom.xml:93-104` |
| Messaging/Integration | Not found | — | `pom.xml` |
| Scheduling/Batch | Not found | — | `src/main/java` (no `@Scheduled`) |
| Reporting | Not found | — | `pom.xml` |
| Logging | SLF4J API with Logback (console appender). Spring 5 logs through `spring-jcl`, which picks up SLF4J. One `System.out` remains in `main.jsp:172` (G30) | SLF4J 2.0.20, Logback 1.3.16 | `pom.xml:106-116`, `src/main/resources/logback.xml`, `PositionDaoImpl.java:22,73`, `LanguageDaoImpl.java:23,73` |
| Security/Auth | Home-made (SQL check plus session attribute); no framework | — | `LoginController`, `LoginDaoImpl` |
| Testing | JUnit 5 (versions aligned by `junit-bom`), Mockito, AssertJ, spring-test standalone MockMvc, Testcontainers MySQL (test scope only; characterization tests) | JUnit 5.13.4, Mockito 4.11.0, AssertJ 3.27.3, Testcontainers 1.21.4 | `pom.xml:19-38,93-130`, `src/test/java/rms/` |
| Packaging | WAR file `rmsv2-1.0.1-SNAPSHOT` | — | `pom.xml` |

**Libraries bundled in the WAR** (`WEB-INF/lib/` of a `mvnw verify` build, `jar tf`, 2026-09-26): spring-aop, spring-beans, spring-context, spring-core, spring-expression, spring-jcl, spring-jdbc, spring-tx, spring-web and spring-webmvc (all 5.3.39), jstl-1.2, mysql-connector-j-8.2.0, slf4j-api-2.0.20, logback-classic and logback-core 1.3.16. The pre-Phase-1 WAR had the Spring 4.3.0 jars, mysql-connector-java-5.1.36 and commons-logging-1.2.

**Dependabot branches:** the three stale branches (mysql-connector-java 8.0.28, spring-web 6.0.0, spring-webmvc 5.2.20) were deleted on 2026-09-26 with the owner's approval; Phase 1 replaced them (last commits `df180f5`, `07248d3`, `852d704`).

**Support status and upgrade path:** see [modernization-plan.md](modernization-plan.md). Spring 5.3 (the current waypoint) lost open-source support on 2024-08-31; the target is Spring 7.0 on JDK 21 and Tomcat 11.
