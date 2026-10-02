package rms.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface LoginService {

	/** The user Spring Security checks the password against (SecurityConfig). */
	public RmsUserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
