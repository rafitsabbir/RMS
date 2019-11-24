package rms.service;

import rms.model.UserInfo;

public interface LoginService {

	public String checkLogin(String username, String password);

	public UserInfo getUserInfo(String userid);
}
