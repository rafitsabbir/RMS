# RMS on Tomcat 11 / JDK 21 (docs/build-run.md, "Container").
# Build the WAR first (./mvnw -B clean package), then: docker build -t rms .
# The image holds no credentials: the JNDI DataSource reads RMS_DB_URL, RMS_DB_USER and RMS_DB_PASSWORD from the
# container's environment at start-up (deploy/tomcat/rms.xml). Documents go to the /var/rms/docs volume.
FROM tomcat:11.0-jre21-temurin@sha256:07a9af94a3cc93d229887acd70b9645e8fb668b9cb79406f7bce3a581d50e279

ENV RMS_DOC_DIR=/var/rms/docs

WORKDIR /usr/local/tomcat

# Everything Tomcat runs (conf, lib, the unpacked application) stays root-owned and read-only to the app user;
# it writes only logs, temp, work and the documents. Auto-deploy is off and the shutdown port is closed, so a
# file written at run time can't become code or change the DataSource.
RUN groupadd --system --gid 10001 rms \
	&& useradd --system --uid 10001 --gid rms --home-dir /nonexistent --shell /usr/sbin/nologin rms \
	&& rm -rf webapps/* \
	&& mkdir -p conf/Catalina/localhost /var/rms/docs \
	&& chown -R rms:rms logs temp work /var/rms/docs \
	&& sed -i -e 's/autoDeploy="true"/autoDeploy="false"/' -e 's/<Server port="8005"/<Server port="-1"/' conf/server.xml \
	&& grep -q 'autoDeploy="false"' conf/server.xml && grep -q '<Server port="-1"' conf/server.xml \
	&& printf '%s\n' '# ${...} in the context file come from the environment (deploy/tomcat/rms.xml)' \
		'CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource -XX:MaxRAMPercentage=75"' \
		> bin/setenv.sh

# The DataSource is Tomcat's, so the driver goes in Tomcat's lib/ (deploy/tomcat/rms.xml)
COPY target/rmsv2-1.0.1-SNAPSHOT/WEB-INF/lib/mysql-connector-j-*.jar lib/
COPY deploy/tomcat/rms.xml conf/Catalina/localhost/rms.xml
# The unpacked WAR (Maven's exploded target folder), served at /rms
COPY target/rmsv2-1.0.1-SNAPSHOT/ webapps/rms/

USER 10001:10001
VOLUME /var/rms/docs
EXPOSE 8080
# /login answers without the database, so this shows Tomcat and the application are up, not the DataSource
HEALTHCHECK --interval=30s --start-interval=5s --timeout=5s --start-period=60s --retries=3 \
	CMD curl -fsS -o /dev/null http://localhost:8080/rms/login || exit 1
