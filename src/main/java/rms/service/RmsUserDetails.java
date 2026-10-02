package rms.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import rms.model.LoginInfo;
import rms.model.UserInfo;

/**
 * The logged-in user Spring Security keeps in the session: the users row plus the admin row (the profile).
 * The role comes from admin.isinterviewer: N is ROLE_ADMIN, Y is ROLE_INTERVIEWER, anything else (including
 * NULL) no role (G16). Spring Security erases the password after the login.
 */
public class RmsUserDetails implements UserDetails, CredentialsContainer {

	private static final long serialVersionUID = 1L;

	public static final String ADMIN = "ROLE_ADMIN";
	public static final String INTERVIEWER = "ROLE_INTERVIEWER";

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
		this.authorities = rolesOf(userinfo);
	}

	private static List<GrantedAuthority> rolesOf(UserInfo userinfo) {
		String isinterviewer = userinfo == null ? null : userinfo.getIsinterviewer();
		if ("N".equalsIgnoreCase(isinterviewer)) {
			return List.of(new SimpleGrantedAuthority(ADMIN));
		}
		if ("Y".equalsIgnoreCase(isinterviewer)) {
			return List.of(new SimpleGrantedAuthority(INTERVIEWER));
		}
		return List.of();
	}

	public String getUserid() {
		return userid;
	}

	public UserInfo getUserinfo() {
		return userinfo;
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
