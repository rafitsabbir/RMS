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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * HTTP smoke test against an already deployed WAR (local dev only).
 * Enabled only when RMS_BASE_URL is set, e.g. the local Tomcat context URL.
 * The login tests also need RMS_SMOKE_USER and RMS_SMOKE_PASSWORD (seed test values); the Latin-1 post
 * needs only a working database (it expects a failed login).
 * Every POST first opens a page for the session cookie and the CSRF token, as a browser does (G32).
 */
@EnabledIfEnvironmentVariable(named = "RMS_BASE_URL", matches = ".+")
class SmokeTest {

	/** The hidden field Spring's form:form writes for Spring Security's CSRF token. */
	private static final Pattern CSRF_FIELD = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

	private static String baseUrl() {
		String url = System.getenv("RMS_BASE_URL");
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	@Test
	void loginPageRenders() throws IOException {
		HttpURLConnection connection = open("/login", null);

		assertThat(connection.getResponseCode()).isEqualTo(200);
		String body = read(connection);
		assertThat(body).contains("id=\"Login\"");
		assertThat(body).contains("name=\"_csrf\"");
	}

	@Test
	void loginPageCarriesNoSessionIdInUrls() throws IOException {
		HttpURLConnection connection = open("/login", null);

		// G41: sessions are tracked by cookie only
		assertThat(read(connection)).doesNotContainIgnoringCase("jsessionid");
	}

	@Test
	void rootRedirectsToLoginWhenLoggedOut() throws IOException {
		HttpURLConnection connection = open("/", null);

		assertThat(connection.getResponseCode()).isEqualTo(302);
		assertThat(connection.getHeaderField("Location")).endsWith("/login");
	}

	@Test
	void loggedOutRequestRedirectsToLogin() throws IOException {
		HttpURLConnection connection = open("/viewpositionlist", null);

		assertThat(connection.getResponseCode()).isEqualTo(302);
		assertThat(connection.getHeaderField("Location")).endsWith("/login");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_USER", matches = ".+")
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_PASSWORD", matches = ".+")
	void loginShowsMenu() throws IOException {
		HttpURLConnection login = postLogin(System.getenv("RMS_SMOKE_USER"), System.getenv("RMS_SMOKE_PASSWORD"));

		// G30: the login redirects to the menu page, with the (new) session in a cookie
		assertThat(login.getResponseCode()).isEqualTo(302);
		String location = login.getHeaderField("Location");
		assertThat(location).endsWith("/home");
		HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + "/").resolve(location).toURL()
				.openConnection();
		connection.setInstanceFollowRedirects(false);
		connection.setRequestProperty("Cookie", sessionCookie(login));

		assertThat(connection.getResponseCode()).isEqualTo(200);
		String body = read(connection);
		assertThat(body).contains("id=\"rms-sidebar\"");
		// Menu items are real links now, not pages loaded into an <object>
		assertThat(body).doesNotContain("<object");
		// G38: the score-entry URLs carry no userid
		assertThat(body).doesNotContain("?user=");
		// Log out is a form with the CSRF token (G32)
		assertThat(body).contains("id=\"logout\"");
		assertThat(CSRF_FIELD.matcher(body).find()).isTrue();
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_USER", matches = ".+")
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_PASSWORD", matches = ".+")
	void listPageUsesTheLayout() throws IOException {
		HttpURLConnection login = postLogin(System.getenv("RMS_SMOKE_USER"), System.getenv("RMS_SMOKE_PASSWORD"));
		assertThat(login.getResponseCode()).isEqualTo(302);
		HttpURLConnection connection = open("/viewpositionlist", sessionCookie(login));

		assertThat(connection.getResponseCode()).isEqualTo(200);
		String body = read(connection);
		assertThat(body).contains("<title>Positions - RMS</title>");
		assertThat(body).contains("id=\"rms-sidebar\"");
		assertThat(body).contains("id=\"positiontable\"");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_USER", matches = ".+")
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_PASSWORD", matches = ".+")
	void candidatePagesRender() throws IOException {
		HttpURLConnection login = postLogin(System.getenv("RMS_SMOKE_USER"), System.getenv("RMS_SMOKE_PASSWORD"));
		String cookie = sessionCookie(login);

		HttpURLConnection list = open("/viewcandidatelist", cookie);
		assertThat(list.getResponseCode()).isEqualTo(200);
		String body = read(list);
		assertThat(body).contains("<title>Candidates - RMS</title>");
		assertThat(body).contains("id=\"candidatetable\"");

		// G1: the add form, with the position and language choices and the CSRF token
		HttpURLConnection form = open("/createcandidate", cookie);
		assertThat(form.getResponseCode()).isEqualTo(200);
		body = read(form);
		assertThat(body).contains("id=\"positionkey\"");
		assertThat(body).contains("id=\"languagekey\"");
		assertThat(CSRF_FIELD.matcher(body).find()).isTrue();
	}

	/** Needs a Super Admin smoke user (test.admin in the seed). */
	@Test
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_USER", matches = ".+")
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_PASSWORD", matches = ".+")
	void userAndPasswordPagesRender() throws IOException {
		HttpURLConnection login = postLogin(System.getenv("RMS_SMOKE_USER"), System.getenv("RMS_SMOKE_PASSWORD"));
		String cookie = sessionCookie(login);

		HttpURLConnection list = open("/viewuserlist", cookie);
		assertThat(list.getResponseCode()).isEqualTo(200);
		String body = read(list);
		assertThat(body).contains("<title>Users and Roles - RMS</title>");
		assertThat(body).contains("id=\"usertable\"");

		HttpURLConnection form = open("/createuser", cookie);
		assertThat(form.getResponseCode()).isEqualTo(200);
		body = read(form);
		assertThat(body).contains("id=\"role\"");
		assertThat(body).contains("value=\"HIRING_MANAGER\"");
		assertThat(CSRF_FIELD.matcher(body).find()).isTrue();

		HttpURLConnection password = open("/changepassword", cookie);
		assertThat(password.getResponseCode()).isEqualTo(200);
		body = read(password);
		assertThat(body).contains("id=\"currentpassword\"");
		assertThat(CSRF_FIELD.matcher(body).find()).isTrue();
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_USER", matches = ".+")
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_PASSWORD", matches = ".+")
	void logoutEndsTheSession() throws IOException {
		HttpURLConnection login = postLogin(System.getenv("RMS_SMOKE_USER"), System.getenv("RMS_SMOKE_PASSWORD"));
		String cookie = sessionCookie(login);
		String token = csrfToken(read(open("/home", cookie)));

		HttpURLConnection logout = post("/logout", "_csrf=" + URLEncoder.encode(token, StandardCharsets.ISO_8859_1),
				cookie);

		assertThat(logout.getResponseCode()).isEqualTo(302);
		assertThat(logout.getHeaderField("Location")).endsWith("/login");
		HttpURLConnection after = open("/home", cookie);
		assertThat(after.getResponseCode()).isEqualTo(302);
		assertThat(after.getHeaderField("Location")).endsWith("/login");
	}

	@Test
	void postWithoutCsrfTokenIsRefused() throws IOException {
		HttpURLConnection connection = post("/saveposition", "positionname=x", null);

		// G32: Spring Security refuses a POST without the token (a login post goes back to the login page instead)
		assertThat(connection.getResponseCode()).isEqualTo(403);
		HttpURLConnection login = post("/welcome", "username=x&password=x", null);
		assertThat(login.getResponseCode()).isEqualTo(302);
		assertThat(login.getHeaderField("Location")).endsWith("/login?expired");
	}

	@Test
	void brandAssetsAreServed() throws IOException {
		HttpURLConnection connection = open("/resources/img/logo.svg", null);

		assertThat(connection.getResponseCode()).isEqualTo(200);
		assertThat(connection.getContentType()).startsWith("image/svg+xml");
		// Static resources stay cacheable (SecurityConfig.resourceFilterChain)
		String cacheControl = connection.getHeaderField("Cache-Control");
		assertThat(cacheControl == null ? "" : cacheControl).doesNotContain("no-store");
	}

	/** The pages post ISO-8859-1; web.xml pins that decoding (Tomcat 11 would answer 400 for UTF-8-invalid bytes). */
	@Test
	void latin1FormPostIsAccepted() throws IOException {
		HttpURLConnection connection = postLogin("café", "x");

		// A failed login (302 to the error page), not HTTP 400
		assertThat(connection.getResponseCode()).isEqualTo(302);
		assertThat(connection.getHeaderField("Location")).endsWith("/login?error");
	}

	/** Opens the login page for a session and its CSRF token, then posts the form as the browser does. */
	private static HttpURLConnection postLogin(String username, String password) throws IOException {
		HttpURLConnection page = open("/login", null);
		String cookie = sessionCookie(page);
		String token = csrfToken(read(page));
		String form = "username=" + URLEncoder.encode(username, StandardCharsets.ISO_8859_1)
				+ "&password=" + URLEncoder.encode(password, StandardCharsets.ISO_8859_1)
				+ "&_csrf=" + URLEncoder.encode(token, StandardCharsets.ISO_8859_1);
		return post("/welcome", form, cookie);
	}

	private static HttpURLConnection open(String path, String cookie) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + path).toURL().openConnection();
		connection.setInstanceFollowRedirects(false);
		if (cookie != null) {
			connection.setRequestProperty("Cookie", cookie);
		}
		return connection;
	}

	/** Posts an ISO-8859-1 form, the way the browser does for these pages. */
	private static HttpURLConnection post(String path, String form, String cookie) throws IOException {
		HttpURLConnection connection = open(path, cookie);
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

	private static String csrfToken(String body) {
		Matcher field = CSRF_FIELD.matcher(body);
		assertThat(field.find()).as("a _csrf field in the page").isTrue();
		return field.group(1);
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
