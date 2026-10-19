# RMS on Tomcat 11 / JDK 21 (docs/build-run.md, "Container").
# Build the WAR first (./mvnw -B package, or the CI build), then: docker build -t rms .
# The image holds no credentials: the JNDI DataSource reads RMS_DB_URL, RMS_DB_USER and RMS_DB_PASSWORD from the
# container's environment at start-up (deploy/tomcat/rms.xml). Documents go to the /var/rms/docs volume.
FROM tomcat:11.0-jre21-temurin@sha256:07a9af94a3cc93d229887acd70b9645e8fb668b9cb79406f7bce3a581d50e279

ENV CATALINA_OPTS="-Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource" \
	RMS_DOC_DIR=/var/rms/docs

WORKDIR /usr/local/tomcat

# Tomcat runs as an unprivileged user that can write only its work folders and the documents
RUN groupadd --system rms \
	&& useradd --system --gid rms --home-dir /nonexistent --shell /usr/sbin/nologin rms \
	&& rm -rf webapps/* \
	&& mkdir -p conf/Catalina/localhost /var/rms/docs \
	&& chown -R rms:rms logs temp work webapps conf/Catalina /var/rms/docs

# The DataSource is Tomcat's, so the driver goes in Tomcat's lib/ (deploy/tomcat/rms.xml)
COPY target/rmsv2-1.0.1-SNAPSHOT/WEB-INF/lib/mysql-connector-j-*.jar lib/
COPY --chown=rms:rms deploy/tomcat/rms.xml conf/Catalina/localhost/rms.xml
COPY --chown=rms:rms target/rmsv2-1.0.1-SNAPSHOT.war webapps/rms.war

USER rms
VOLUME /var/rms/docs
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
	CMD curl -fsS -o /dev/null http://localhost:8080/rms/login || exit 1
