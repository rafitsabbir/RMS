# Build & Run

Purpose: How RMS is built, packaged, deployed and tested.
Last updated: 2026-09-25
Read this when: you're building, deploying, setting up an environment, or fixing a build or startup failure.

## Build
- **Tool:** Maven. The artifact is `com.sabbir:rmsv2:1.0.1-SNAPSHOT`, packaged as a WAR (`pom.xml`).
- **Command:** `mvn package` builds `target/rmsv2-1.0.1-SNAPSHOT.war`.
  - This is standard Maven behaviour for WAR packaging (`pom.xml`). It hasn't been run in this environment: Java and Maven aren't installed on this machine as of 2026-09-25.
- **Java version:** `pom.xml` doesn't set a compiler level. The committed WAR was built with JDK 1.8.0_221 and Maven 3.6.1 (`target/rmsv2-1.0.1-SNAPSHOT.war` → `META-INF/MANIFEST.MF`).
- **WAR settings:** `maven-war-plugin` 2.3, with `warSourceDirectory=src/main/webapp` and `failOnMissingWebXml=false` (`pom.xml`).

## Run / deploy
- **How it runs:** deploy the WAR to a Servlet 3.1 container. `javax.servlet-api` 3.1.0 is `provided` (`pom.xml`). There's no embedded server and no `main()` (`rms/config/WebInitializer.java`).
- **Database connection:** the container must provide a JNDI DataSource named `jdbc/springrms`, which the app looks up as `java:comp/env/jdbc/springrms` (`WebConfig.getDataSource`). Its definition isn't in the repo.
- **Entry URLs:** `GET /login` shows the login page (`LoginController.loginPage`). `index.jsp` is a "Hello World!" placeholder (`src/main/webapp/index.jsp`).

## Test
- There are no tests: no `src/test/` directory and no test dependencies (`pom.xml`). `.classpath` refers to `src/test/java`, but the folder doesn't exist.

## Repo notes
- `target/` (build output, including the WAR) is committed, and there's no `.gitignore` (`git ls-files target`).
- **The committed WAR is out of date:** it contains no Position classes or JSPs (`unzip -l target/rmsv2-1.0.1-SNAPSHOT.war`), so it was built before commit `e5705c0`. Don't deploy it as-is. Rebuild it (see gap G29 in [gaps.md](gaps.md)).
- Eclipse project files are committed: `.project`, `.classpath`, `.settings/`.
