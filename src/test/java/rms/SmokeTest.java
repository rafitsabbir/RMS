package rms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * HTTP smoke test against an already deployed WAR (local dev only).
 * Enabled only when RMS_BASE_URL is set, e.g. the local Tomcat context URL.
 * The login test also needs RMS_SMOKE_USER and RMS_SMOKE_PASSWORD (seed test values).
 */
@EnabledIfEnvironmentVariable(named = "RMS_BASE_URL", matches = ".+")
class SmokeTest {

	private static String baseUrl() {
		String url = System.getenv("RMS_BASE_URL");
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	@Test
	void loginPageRenders() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl() + "/login").openConnection();

		assertThat(connection.getResponseCode()).isEqualTo(200);
		assertThat(read(connection)).contains("id=\"Login\"");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_USER", matches = ".+")
	@EnabledIfEnvironmentVariable(named = "RMS_SMOKE_PASSWORD", matches = ".+")
	void loginShowsMenu() throws IOException {
		String form = "username=" + URLEncoder.encode(System.getenv("RMS_SMOKE_USER"), "UTF-8")
				+ "&password=" + URLEncoder.encode(System.getenv("RMS_SMOKE_PASSWORD"), "UTF-8");
		HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl() + "/welcome").openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		OutputStream out = connection.getOutputStream();
		try {
			out.write(form.getBytes("UTF-8"));
		} finally {
			out.close();
		}

		assertThat(connection.getResponseCode()).isEqualTo(200);
		assertThat(read(connection)).contains("sidenav");
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
