package rms.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The four roles, and the fallback for rows from before migration 002. */
class RoleTest {

	@Test
	void storedRoleWinsOverIsinterviewer() {
		assertThat(Role.of("HR", "N")).isEqualTo(Role.HR);
		assertThat(Role.of("hiring_manager", null)).isEqualTo(Role.HIRING_MANAGER);
		assertThat(Role.of(" SUPER_ADMIN ", "Y")).isEqualTo(Role.SUPER_ADMIN);
	}

	@Test
	void nullRoleFallsBackToIsinterviewer() {
		assertThat(Role.of(null, "N")).isEqualTo(Role.SUPER_ADMIN);
		assertThat(Role.of(null, "n")).isEqualTo(Role.SUPER_ADMIN);
		assertThat(Role.of(null, "Y")).isEqualTo(Role.INTERVIEWER);
		assertThat(Role.of(null, "y")).isEqualTo(Role.INTERVIEWER);
		assertThat(Role.of(null, null)).isNull();
		assertThat(Role.of(null, "X")).isNull();
	}

	@Test
	void unknownStoredRoleIsNoRole() {
		// Fail closed: a typo in admin.role doesn't fall back to isinterviewer
		assertThat(Role.of("ADMIN", "N")).isNull();
		assertThat(Role.of("", "N")).isNull();
	}

	@Test
	void isinterviewerKeepsAnOlderWarSafe() {
		// Only SUPER_ADMIN is N (an admin in WARs from before the roles). The others are Y, not NULL: the released
		// WAR's main.jsp calls getIsinterviewer().equalsIgnoreCase(...) and fails with HTTP 500 on NULL
		assertThat(Role.SUPER_ADMIN.getIsinterviewer()).isEqualTo("N");
		assertThat(Role.HR.getIsinterviewer()).isEqualTo("Y");
		assertThat(Role.HIRING_MANAGER.getIsinterviewer()).isEqualTo("Y");
		assertThat(Role.INTERVIEWER.getIsinterviewer()).isEqualTo("Y");
	}

	@Test
	void authorityAndLabel() {
		assertThat(Role.HIRING_MANAGER.getAuthority()).isEqualTo("ROLE_HIRING_MANAGER");
		assertThat(Role.HIRING_MANAGER.getLabel()).isEqualTo("Hiring Manager");
	}

	@Test
	void userInfoShowsTheRoleLabel() {
		UserInfo user = new UserInfo();
		assertThat(user.getRolelabel()).isEmpty();
		user.setRole("SUPER_ADMIN");
		assertThat(user.getRolelabel()).isEqualTo("Super Admin");
	}
}
