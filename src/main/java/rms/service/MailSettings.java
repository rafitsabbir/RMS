package rms.service;

import java.util.Map;

/**
 * The mail server for staff notifications (F1, 2026-11-05), read once at startup from the environment. No host means
 * no e-mail: RMS then works as before. Never logged: the values may hold a password and host names.
 *
 * <ul>
 * <li>RMS_MAIL_HOST: the SMTP server; unset or blank turns notifications off</li>
 * <li>RMS_MAIL_PORT: default 587</li>
 * <li>RMS_MAIL_USER and RMS_MAIL_PASSWORD: optional, for servers that need a login</li>
 * <li>RMS_MAIL_FROM: the sender address; required with a host</li>
 * <li>RMS_MAIL_SECURITY: starttls (default, port 587), ssl (port 465) or none (a relay on a closed network)</li>
 * <li>RMS_APP_URL: the address users open RMS at, for example https://rms.example.org/rms; links are left out without it</li>
 * </ul>
 */
public record MailSettings(String host, int port, String user, String password, String from, String security,
		String appurl) {

	public static final String STARTTLS = "starttls";
	public static final String SSL = "ssl";
	public static final String NONE = "none";

	/** Notifications switched off. */
	public static final MailSettings OFF = new MailSettings(null, 587, null, null, null, STARTTLS, null);

	public static MailSettings fromEnvironment() {
		return from(System.getenv());
	}

	static MailSettings from(Map<String, String> env) {
		String host = clean(env.get("RMS_MAIL_HOST"));
		int port = 587;
		String portsetting = clean(env.get("RMS_MAIL_PORT"));
		if (portsetting != null) {
			try {
				port = Integer.parseInt(portsetting);
			} catch (NumberFormatException e) {
				port = -1;
			}
		}
		String security = clean(env.get("RMS_MAIL_SECURITY"));
		security = security == null ? STARTTLS : security.toLowerCase();
		String appurl = clean(env.get("RMS_APP_URL"));
		if (appurl != null && appurl.endsWith("/")) {
			appurl = appurl.substring(0, appurl.length() - 1);
		}
		return new MailSettings(host, port, clean(env.get("RMS_MAIL_USER")), env.get("RMS_MAIL_PASSWORD"),
				clean(env.get("RMS_MAIL_FROM")), security, appurl);
	}

	/** A host is set. */
	public boolean configured() {
		return host != null;
	}

	/** Why the settings can't be used, or null when they can (or when no host is set). */
	public String problem() {
		if (!configured()) {
			return null;
		}
		if (from == null || !from.contains("@")) {
			return "RMS_MAIL_FROM must be the sender's e-mail address";
		}
		if (port < 1 || port > 65535) {
			return "RMS_MAIL_PORT must be a port number";
		}
		if (!STARTTLS.equals(security) && !SSL.equals(security) && !NONE.equals(security)) {
			return "RMS_MAIL_SECURITY must be starttls, ssl or none";
		}
		if (appurl != null && !appurl.startsWith("https://") && !appurl.startsWith("http://")) {
			return "RMS_APP_URL must start with https:// or http://";
		}
		return null;
	}

	/** Usable: a host is set and nothing is wrong. */
	public boolean enabled() {
		return configured() && problem() == null;
	}

	@Override
	public String toString() {
		// Keeps the password and the host out of any log line
		return "MailSettings[" + (configured() ? "configured" : "off") + "]";
	}

	private static String clean(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
