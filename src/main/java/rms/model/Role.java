package rms.model;

/**
 * The four user roles (admin.role, migration 002). A row without a role falls back to the legacy
 * admin.isinterviewer column: N is SUPER_ADMIN, Y is INTERVIEWER. Anything else, including an unknown role
 * text, is no role, so the user sees only Home (fail closed, G16).
 */
public enum Role {

	SUPER_ADMIN("Super Admin", "N"),
	HR("HR", "Y"),
	HIRING_MANAGER("Hiring Manager", "Y"),
	INTERVIEWER("Interviewer", "Y");

	private final String label;
	private final String isinterviewer;

	Role(String label, String isinterviewer) {
		this.label = label;
		this.isinterviewer = isinterviewer;
	}

	/** The name shown on the pages. */
	public String getLabel() {
		return label;
	}

	/**
	 * The admin.isinterviewer value written with this role, so a WAR from before the roles still works after a
	 * rollback. Only SUPER_ADMIN is N (an admin there). HR and HIRING_MANAGER are Y, not NULL: the released WAR's
	 * main.jsp calls getIsinterviewer().equalsIgnoreCase(...) and fails with HTTP 500 on NULL, while Y gives the
	 * interviewer menu with no admin pages.
	 */
	public String getIsinterviewer() {
		return isinterviewer;
	}

	/** The Spring Security authority, e.g. ROLE_HR. */
	public String getAuthority() {
		return "ROLE_" + name();
	}

	/** The role stored in admin.role, or, when that is NULL, the one isinterviewer implies; null for none. */
	public static Role of(String role, String isinterviewer) {
		if (role != null) {
			return parse(role);
		}
		if ("N".equalsIgnoreCase(isinterviewer)) {
			return SUPER_ADMIN;
		}
		if ("Y".equalsIgnoreCase(isinterviewer)) {
			return INTERVIEWER;
		}
		return null;
	}

	/** The role with this name (any case, trimmed), or null. */
	public static Role parse(String name) {
		if (name == null) {
			return null;
		}
		for (Role role : values()) {
			if (role.name().equalsIgnoreCase(name.trim())) {
				return role;
			}
		}
		return null;
	}
}
