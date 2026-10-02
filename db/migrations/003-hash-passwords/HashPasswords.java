// RMS migration 003: replace the legacy plain-text passwords in users.password with {bcrypt} hashes (G13).
//
// NOT RUN BY ANYONE YET. It needs the owner's approval (docs/open-questions.md #25), and it is not part of the WAR.
// See README.md in this folder for when and how to run it, the backup it needs, and the rollback.
//
// Dry run by default: reads users and prints only counts. With --apply it updates the rows, in one transaction.
// Connection settings come from the environment only, so nothing lands in a file or the shell history:
//   RMS_DB_URL       e.g. jdbc:mysql://<host>:3306/<database>
//   RMS_DB_USER
//   RMS_DB_PASSWORD
// It never prints userids, usernames, passwords or the connection settings.

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashPasswords {

	/** {bcrypt} plus a 60-character hash. */
	static final int HASHED_LENGTH = 68;
	/** bcrypt reads at most 72 bytes; Spring Security refuses to encode a longer password. */
	static final int MAX_BYTES = 72;

	public static void main(String[] args) throws Exception {
		boolean apply = args.length == 1 && "--apply".equals(args[0]);
		if (args.length > 1 || (args.length == 1 && !apply)) {
			System.err.println("Usage: HashPasswords [--apply]   (without --apply: dry run, nothing is changed)");
			System.exit(2);
		}
		String url = required("RMS_DB_URL");
		String user = required("RMS_DB_USER");
		String password = required("RMS_DB_PASSWORD");

		try (Connection connection = DriverManager.getConnection(url, user, password)) {
			int length = passwordColumnLength(connection);
			System.out.println("users.password holds " + length + " characters (needs " + HASHED_LENGTH + ").");
			if (length < HASHED_LENGTH) {
				System.err.println("Stopped: widen users.password first (see db/migrations/002-user-roles.sql).");
				System.exit(1);
			}

			List<String[]> plain = new ArrayList<String[]>();
			int encoded = 0;
			int empty = 0;
			int tooLong = 0;
			try (PreparedStatement select = connection.prepareStatement("select userid, password from users");
					ResultSet rows = select.executeQuery()) {
				while (rows.next()) {
					String userid = rows.getString(1);
					String stored = rows.getString(2);
					if (stored == null || stored.isEmpty()) {
						// Can't log in today either (an empty password never matches)
						empty++;
					} else if (stored.startsWith("{")) {
						// Already {bcrypt}, or a plain-text password starting with a brace (#25): left alone
						encoded++;
					} else if (stored.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
						tooLong++;
					} else {
						plain.add(new String[] { userid, stored });
					}
				}
			}

			System.out.println("Plain-text passwords to hash:            " + plain.size());
			System.out.println("Left alone, starting with { (encoded):   " + encoded);
			System.out.println("Left alone, empty or NULL:               " + empty);
			System.out.println("Left alone, longer than 72 bytes:        " + tooLong);
			if (!apply) {
				System.out.println("Dry run: nothing was changed. Run with --apply to hash them.");
				return;
			}

			BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
			connection.setAutoCommit(false);
			int updated = 0;
			// Only if the row still holds the value read above, compared byte for byte (not by the collation)
			try (PreparedStatement update = connection.prepareStatement(
					"update users set password=? where userid=? and password = cast(? as binary)")) {
				for (String[] row : plain) {
					update.setString(1, "{bcrypt}" + bcrypt.encode(row[1]));
					update.setString(2, row[0]);
					update.setString(3, row[1]);
					updated += update.executeUpdate();
				}
				connection.commit();
			} catch (SQLException | RuntimeException e) {
				connection.rollback();
				System.err.println("Stopped, nothing was changed: " + e.getMessage());
				System.exit(1);
			}
			System.out.println("Hashed and committed: " + updated + " rows.");
			if (updated != plain.size()) {
				System.out.println("Some rows changed while this ran and were left as they were; run it again.");
			}
		}
	}

	private static int passwordColumnLength(Connection connection) throws SQLException {
		try (PreparedStatement query = connection.prepareStatement("select character_maximum_length "
				+ "from information_schema.columns where table_schema = database() and table_name = 'users' "
				+ "and column_name = 'password'");
				ResultSet result = query.executeQuery()) {
			if (!result.next()) {
				throw new SQLException("No users.password column in the current database");
			}
			return result.getInt(1);
		}
	}

	private static String required(String name) {
		String value = System.getenv(name);
		if (value == null || value.isEmpty()) {
			System.err.println("Set the environment variable " + name + ".");
			System.exit(2);
		}
		return value;
	}
}
