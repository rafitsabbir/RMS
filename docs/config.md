# Configuration

Purpose: Where RMS's configuration lives and what it controls. No secrets are recorded here.
Last updated: 2026-09-26
Read this when: you're changing configuration, the DB connection, static resources, view resolution, or anything that talks to an outside system.

## Configuration sources
| Item | Location | Details | Evidence |
|---|---|---|---|
| Spring config | `rms.config.WebConfig` (Java) | `@EnableWebMvc`, `@ComponentScan("rms")`, `implements WebMvcConfigurer` (was `extends WebMvcConfigurerAdapter` before Phase 1) | `WebConfig.java` |
| Servlet bootstrap | `rms.config.WebInitializer` | DispatcherServlet on `/`; root config `WebConfig`; no servlet-specific config | `WebInitializer.java` |
| DataSource | JNDI lookup `java:comp/env/jdbc/springrms` (defined in the container) | `NamedParameterJdbcTemplate` bean built on it. Since Phase 1 the container resource should use `com.mysql.cj.jdbc.Driver` (the old class name is a deprecated shim) ([build-run.md](build-run.md)) | `WebConfig.getDataSource`, `getNamedParameterJdbcTemplate` |
| View resolver | `InternalResourceViewResolver` + `JstlView` | prefix `/WEB-INF/jsp/`, suffix `.jsp` | `WebConfig.viewResolver` |
| Static resources | `/resources/**` → `/resources/` | CSS, JS, images | `WebConfig.addResourceHandlers`, `src/main/webapp/resources/` |
| `web.xml` | `src/main/webapp/WEB-INF/web.xml` | Empty archetype stub (DTD 2.3), only `display-name` | `web.xml` |
| Logging | `src/main/resources/logback.xml` | Console appender; `rms` at INFO, `org.springframework` at WARN, root INFO | `logback.xml` |

- **Not found:** `.properties` or `.yml` application config files, Spring profiles, and environment-variable lookups (`src/main`).

## Integration points
- **Database:** MySQL through JNDI (see above). This is the only back-end integration.
- **Browser-side CDNs:** the JSPs load Bootstrap, jQuery, DataTables and Font Awesome from public CDNs at runtime (`WEB-INF/jsp/*.jsp`). Hosts: cdn.jsdelivr.net (Bootstrap), code.jquery.com, cdn.datatables.net and cdnjs.cloudflare.com (Font Awesome). Every link carries an SRI `integrity` hash and `crossorigin="anonymous"`, so a changed file is blocked by the browser. The pages need internet access to look right.
- **Not found:** messaging, REST clients, email, file transfer (`pom.xml`).
