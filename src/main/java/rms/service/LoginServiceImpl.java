package rms.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import rms.dao.LoginDao;
import rms.model.LoginInfo;

@Service
public class LoginServiceImpl implements LoginService {
	private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);

	LoginDao logindao;

	@Autowired
	public void setUserdao(LoginDao logindao) {
		this.logindao = logindao;
	}

	public RmsUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		List<LoginInfo> logins = logindao.findLogins(username);
		if (logins.isEmpty()) {
			throw new UsernameNotFoundException("No such user");
		}
		if (logins.size() > 1) {
			// users has no known unique index on username (G22); two rows would make the login ambiguous
			List<String> userids = new ArrayList<String>();
			for (LoginInfo login : logins) {
				userids.add(login.getUserid());
			}
			log.warn("Users {} share one username; login refused", userids);
			throw new UsernameNotFoundException("Ambiguous username");
		}
		LoginInfo login = logins.get(0);
		// May be null (no admin row): refused after the password check, as before (SecurityConfig, G26)
		return new RmsUserDetails(login, logindao.getUserInfo(login.getUserid()));
	}

}
