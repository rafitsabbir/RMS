package rms.dao;

import java.util.List;

import rms.model.LoginInfo;
import rms.model.UserInfo;

public interface LoginDao {

	/** Every users row with this username: none, one, or several (no unique index is known, G22). */
	public List<LoginInfo> findLogins(String username);

	public UserInfo getUserInfo(String userid);
}
