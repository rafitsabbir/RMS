package rms.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import rms.model.LoginInfo;
import rms.model.Role;
import rms.model.UserInfo;

/**
 * The logged-in user Spring Security keeps in the session: the users row plus the admin row (the profile).
 * The authority is the profile's role (Role: admin.role, or the isinterviewer fallback), e.g. ROLE_HR; no role
 * means no authority (G16). Enabled means admin.isactive = 1; SecurityConfig checks it after the password.
 * Spring Security erases the password after the login.
 */
public class RmsUserDetails implements UserDetails, CredentialsContainer {

	private static final long serialVersionUID = 1L;

	private final String userid;
	private final String username;
	private String password;
	/** null when the users row has no admin row; SecurityConfig refuses such a login (G26) */
	private final UserInfo userinfo;
	private final List<GrantedAuthority> authorities;

	public RmsUserDetails(LoginInfo login, UserInfo userinfo) {
		this.userid = login.getUserid();
		this.username = login.getUsername();
		this.password = login.getPassword();
		this.userinfo = userinfo;
		Role role = roleOf(userinfo);
		this.authorities = role == null ? List.of() : List.of(new SimpleGrantedAuthority(role.getAuthority()));
	}

	private static Role roleOf(UserInfo userinfo) {
		return userinfo == null ? null : Role.parse(userinfo.getRole());
	}

	public String getUserid() {
		return userid;
	}

	public UserInfo getUserinfo() {
		return userinfo;
	}

	/** The role this login was granted, or null. */
	public Role getRole() {
		return roleOf(userinfo);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	/** admin.isactive = 1. A user without an admin row isn't enabled either. */
	@Override
	public boolean isEnabled() {
		return userinfo != null && userinfo.getIsactive() == 1;
	}

	@Override
	public void eraseCredentials() {
		password = null;
	}

	/** Leaves the password out, so a log line can't leak it. */
	@Override
	public String toString() {
		return "RmsUserDetails [userid=" + userid + ", username=" + username + ", authorities=" + authorities + "]";
	}
}
