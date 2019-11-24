package rms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.LoginDao;
import rms.model.UserInfo;

@Service
public class LoginServiceImpl implements LoginService {

	LoginDao logindao;

	@Autowired
	public void setUserdao(LoginDao logindao) {
		this.logindao = logindao;
	}

	public String checkLogin(String username, String password) {
		// TODO Auto-generated method stub
		return logindao.checkUser(username, password);
	}

	public UserInfo getUserInfo(String userid) {
		// TODO Auto-generated method stub
		return logindao.getUserInfo(userid);
	}

}
