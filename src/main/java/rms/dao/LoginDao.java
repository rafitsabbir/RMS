package rms.dao;

import rms.model.UserInfo;

public interface LoginDao {

	public String checkUser(String username, String password);

	public UserInfo getUserInfo(String userid);
}
