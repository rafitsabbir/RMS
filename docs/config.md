# Configuration

Purpose: Where RMS's configuration lives and what it controls. No secrets are recorded here.
Last updated: 2026-10-01 (Tomcat context template and CI workflows). 2026-10-01 (Phase 3 `web.xml`, then `metadata-complete`). 2026-09-30 (access-control interceptor)
Read this when: you're changing configuration, the DB connection, static resources, view resolution, or anything that talks to an outside system.

## Configuration sources
| Item | Location | Details | Evidence |
|---|---|---|---|
| Spring config | `rms.config.WebConfig` (Java) | `@EnableWebMvc`, `@ComponentScan("rms")`, `implements WebMvcConfigurer` (was `extends WebMvcConfigurerAdapter` before Phase 1) | `WebConfig.java` |
| Servlet bootstrap | `rms.config.WebInitializer` | DispatcherServlet on `/`; root config `WebConfig`; no servlet-specific config | `WebInitializer.java` |
| DataSource | JNDI lookup `java:comp/env/jdbc/springrms` (defined in the container) | `NamedParameterJdbcTemplate` bean built on it. Since Phase 1 the container resource should use `com.mysql.cj.jdbc.Driver` (the old class name is a deprecated shim) ([build-run.md](build-run.md)) | `WebConfig.getDataSource`, `getNamedParameterJdbcTemplate` |
| View resolver | `InternalResourceViewResolver` + `JstlView` | prefix `/WEB-INF/jsp/`, suffix `.jsp` | `WebConfig.viewResolver` |
| Static resources | `/resources/**` → `/resources/` | CSS, JS, images | `WebConfig.addResourceHandlers`, `src/main/webapp/resources/` |
| Access control | `rms.config.AuthInterceptor` on all paths except `/login`, `/welcome` and `/resources/**` | Logged out → redirect to `/login`; not an admin (`isinterviewer` ≠ `N`) → 403 (G11). The public paths are the only settings | `WebConfig.addInterceptors`, `AuthInterceptor.java` |
| `web.xml` | `src/main/webapp/WEB-INF/web.xml` | Servlet 6.1 descriptor (Phase 3; was an empty 2.3 DTD stub, G37): `display-name`, and `request-character-encoding` ISO-8859-1 to match the JSPs. Tomcat 11's own default is UTF-8, which rejects the JSPs' Latin-1 form posts with HTTP 400 (G34), so keep the file (`SmokeTest.latin1FormPostIsAccepted`). `metadata-complete="true"`: no annotation scan, as under the 2.3 DTD. EL is on by default. No `<session-config>`, so sessions are also tracked by URL rewriting (G41) | `web.xml` |
| Tomcat context template | `deploy/tomcat/rms.xml` (not in the WAR) | The `jdbc/springrms` resource for `conf/Catalina/localhost/rms.xml`: `com.mysql.cj.jdbc.Driver`, `validationQuery`, and only `${RMS_DB_URL}`, `${RMS_DB_USER}`, `${RMS_DB_PASSWORD}` placeholders, which Tomcat fills from environment variables with `EnvironmentPropertySource`. No values are in the repo ([build-run.md](build-run.md)) | `deploy/tomcat/rms.xml` |
| CI | `.github/workflows/ci.yml`, `.github/workflows/osv-scanner.yml` | Build and tests on PRs and pushes to `dev`/`master`; OSV-Scanner dependency scan. No secrets are used | `.github/workflows/` |
| Logging | `src/main/resources/logback.xml` | Console appender; `rms` at INFO, `org.springframework` at WARN, root INFO | `logback.xml` |

- **Not found:** `.properties` or `.yml` application config files, Spring profiles, and environment-variable lookups (`src/main`).

## Integration points
- **Database:** MySQL through JNDI (see above). This is the only back-end integration.
- **Browser-side CDNs:** the JSPs load Bootstrap, jQuery, DataTables and Font Awesome from public CDNs at runtime (`WEB-INF/jsp/*.jsp`). Hosts: cdn.jsdelivr.net (Bootstrap), code.jquery.com, cdn.datatables.net and cdnjs.cloudflare.com (Font Awesome). Every link carries an SRI `integrity` hash and `crossorigin="anonymous"`, so a changed file is blocked by the browser. The pages need internet access to look right.
- **Not found:** messaging, REST clients, email, file transfer (`pom.xml`).
