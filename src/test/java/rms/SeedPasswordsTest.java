package rms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import rms.config.SecurityConfig;

/** The seed's {bcrypt} rows (U3-U5) match their documented test passwords, test-only-3 to test-only-5. No Docker. */
class SeedPasswordsTest {

	private static final Pattern HASHED_ROW = Pattern.compile("[(]'U([0-9]+)', '[^']+', '([{]bcrypt[}][^']+)'");

	@Test
	void bcryptSeedRowsMatchTheirTestPasswords() throws IOException {
		String seed = Files.readString(Path.of(System.getProperty("basedir", "."), "db", "test-seed.sql"),
				StandardCharsets.UTF_8);
		PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

		Matcher row = HASHED_ROW.matcher(seed);
		int found = 0;
		while (row.find()) {
			assertThat(row.group(2)).hasSize(68);
			assertThat(encoder.matches("test-only-" + row.group(1), row.group(2))).as("U" + row.group(1)).isTrue();
			found++;
		}
		assertThat(found).isEqualTo(3);
	}
}
