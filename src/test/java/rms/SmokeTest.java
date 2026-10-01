package rms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * HTTP smoke test against an already deployed WAR (local dev only).
 * Enabled only when RMS_BASE_URL is set, e.g. the local Tomcat context URL.
 * The login test also needs RMS_SMOKE_USER and RMS_SMOKE_PASSWORD (seed test values); the Latin-1 post
 * needs only a working database (it expects "Invalid login!").
 */
@EnabledIfEnvironmentVariable(named = "RMS_BASE_URL", matches = ".+")
class SmokeTest {

	private static String baseUrl() {
		String url = System.getenv("RMS_BASE_URL");
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	@Test
	void loginPageRenders() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + "/login").toURL().openConnection();

		assertThat(connection.getResponseCode()).isEqualTo(200);
		assertThat(read(connection)).contains("id=\"Login\"");
	}

	@Test
	void loginPageCarriesNoSessionIdInUrls() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + "/login").toURL().openConnection();

		// G41: sessions are tracked by cookie only
		assertThat(read(connection)).doesNotContainIgnoringCase("jsessionid");
	}

	@Test
	void rootRedirectsToLoginWhenLoggedOut() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + "/").toURL().openConnection();
		connection.setInstanceFollowRedirects(false);

		assertThat(connection.getResponseCode()).isEqualTo(302);
		assertThat(connection.getHeaderField("Location")).endsWith("/login");
	}

	@Test
	void loggedOutRequestRedirectsToLogin() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + "/viewpositionlist").toURL().openConnection();
		connection.setInstanceFollowRedirects(false);

		assertThat(connection.getResponseCode()).isEqualTo(302);
		assertThat(connection.getHeaderField("Location")).endsWith("/login");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_USER", matches = ".+")
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_PASSWORD", matches = ".+")
	void loginShowsMenu() throws IOException {
		HttpURLConnection login = postLogin(System.getenv("RMS_SMOKE_USER"), System.getenv("RMS_SMOKE_PASSWORD"));

		// G30: the login redirects to the menu page, with the session in a cookie
		assertThat(login.getResponseCode()).isEqualTo(302);
		String location = login.getHeaderField("Location");
		assertThat(location).endsWith("/home");
		HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + "/").resolve(location).toURL()
				.openConnection();
		connection.setInstanceFollowRedirects(false);
		connection.setRequestProperty("Cookie", sessionCookie(login));

		assertThat(connection.getResponseCode()).isEqualTo(200);
		String body = read(connection);
		assertThat(body).contains("sidenav");
		// G38: the score-entry URLs carry no userid
		assertThat(body).doesNotContain("?user=");
	}

	/** The pages post ISO-8859-1; web.xml pins that decoding (Tomcat 11 would answer 400 for UTF-8-invalid bytes). */
	@Test
	void latin1FormPostIsAccepted() throws IOException {
		HttpURLConnection connection = postLogin("caf\u00e9", "x");

		assertThat(connection.getResponseCode()).isEqualTo(200);
		assertThat(read(connection)).contains("Invalid login!");
	}

	/** Posts the login form the way the browser does for an ISO-8859-1 page. */
	private static HttpURLConnection postLogin(String username, String password) throws IOException {
		String form = "username=" + URLEncoder.encode(username, StandardCharsets.ISO_8859_1)
				+ "&password=" + URLEncoder.encode(password, StandardCharsets.ISO_8859_1);
		HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + "/welcome").toURL().openConnection();
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		OutputStream out = connection.getOutputStream();
		try {
			out.write(form.getBytes(StandardCharsets.ISO_8859_1));
		} finally {
			out.close();
		}
		return connection;
	}

	/** The JSESSIONID cookie the response set, as a Cookie request header value. */
	private static String sessionCookie(HttpURLConnection connection) {
		for (String header : connection.getHeaderFields().getOrDefault("Set-Cookie", List.of())) {
			if (header.startsWith("JSESSIONID=")) {
				return header.split(";", 2)[0];
			}
		}
		throw new AssertionError("no JSESSIONID cookie in " + connection.getHeaderFields());
	}

	private static String read(HttpURLConnection connection) throws IOException {
		InputStream in = connection.getInputStream();
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[4096];
			int n;
			while ((n = in.read(chunk)) != -1) {
				buffer.write(chunk, 0, n);
			}
			return new String(buffer.toByteArray(), "ISO-8859-1");
		} finally {
			in.close();
		}
	}

}
