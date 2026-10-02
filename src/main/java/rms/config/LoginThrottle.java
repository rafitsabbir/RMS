package rms.config;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Login rate limit (G42): after 5 failed logins for one username from one address within 15 minutes, that
 * username is locked for that address for 15 minutes. A successful login clears the count.
 * <p>
 * In memory: each Tomcat node counts on its own, and a restart forgets every count. The address is the
 * request's remote address; behind a reverse proxy that is the proxy's, unless Tomcat's RemoteIpValve is set up,
 * and then the lock applies to the username from every address.
 */
public class LoginThrottle {

	private static final Logger log = LoggerFactory.getLogger(LoginThrottle.class);

	static final int MAX_FAILURES = 5;
	static final Duration WINDOW = Duration.ofMinutes(15);
	static final Duration LOCK = Duration.ofMinutes(15);
	/** Above this many tracked keys, finished entries are dropped on the next failure. */
	private static final int PURGE_ABOVE = 10000;

	private final Clock clock;
	private final Map<String, Attempts> attempts = new ConcurrentHashMap<String, Attempts>();

	public LoginThrottle() {
		this(Clock.systemUTC());
	}

	LoginThrottle(Clock clock) {
		this.clock = clock;
	}

	/** Failure times within the window, and the end of the current lock (null when not locked). */
	private static final class Attempts {
		final Deque<Instant> failures = new ArrayDeque<Instant>();
		Instant lockedUntil;
	}

	public boolean isLocked(String username, String address) {
		Attempts entry = attempts.get(key(username, address));
		if (entry == null) {
			return false;
		}
		synchronized (entry) {
			return entry.lockedUntil != null && clock.instant().isBefore(entry.lockedUntil);
		}
	}

	public void loginFailed(String username, String address) {
		Instant now = clock.instant();
		if (attempts.size() > PURGE_ABOVE) {
			purge(now);
		}
		Attempts entry = attempts.computeIfAbsent(key(username, address), k -> new Attempts());
		synchronized (entry) {
			if (entry.lockedUntil != null && !now.isBefore(entry.lockedUntil)) {
				entry.lockedUntil = null;
				entry.failures.clear();
			}
			drop(entry, now);
			entry.failures.addLast(now);
			if (entry.lockedUntil == null && entry.failures.size() >= MAX_FAILURES) {
				entry.lockedUntil = now.plus(LOCK);
				// Neither the username nor the address: both can be personal data
				log.warn("Login locked for {} minutes after {} failures", LOCK.toMinutes(), MAX_FAILURES);
			}
		}
	}

	public void loginSucceeded(String username, String address) {
		attempts.remove(key(username, address));
	}

	/** Forgets every count (tests). */
	void clear() {
		attempts.clear();
	}

	private static void drop(Attempts entry, Instant now) {
		Instant oldest = now.minus(WINDOW);
		while (!entry.failures.isEmpty() && !entry.failures.peekFirst().isAfter(oldest)) {
			entry.failures.removeFirst();
		}
	}

	private void purge(Instant now) {
		attempts.entrySet().removeIf(e -> {
			Attempts entry = e.getValue();
			synchronized (entry) {
				drop(entry, now);
				return entry.failures.isEmpty() && (entry.lockedUntil == null || !now.isBefore(entry.lockedUntil));
			}
		});
	}

	/**
	 * Usernames are folded the way MySQL's default collations compare them at login: case and accents ignored, and
	 * the German sharp s as ss. So "Admin", "admin" and accented spellings of it share one count instead of each
	 * getting 5 tries.
	 */
	static String key(String username, String address) {
		String name = username == null ? "" : username.trim();
		name = MARKS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
		name = name.toLowerCase(Locale.ROOT).replace(SHARP_S, "ss");
		return name + "\n" + (address == null ? "" : address);
	}

	private static final Pattern MARKS = Pattern.compile("\\p{M}+");
	/** The German sharp s (U+00DF), as a code so this file stays ASCII. */
	private static final String SHARP_S = String.valueOf((char) 0xDF);
}
