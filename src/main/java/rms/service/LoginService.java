package rms.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import rms.model.UserInfo;

public interface LoginService {

	/** The user Spring Security checks the password against (SecurityConfig). */
	public RmsUserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

	/** The user's profile as stored now (active flag, role, forced password change), or null without an admin row. */
	public UserInfo currentProfile(String userid);
}
