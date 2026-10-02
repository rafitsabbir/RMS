package rms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import rms.dao.UserDao;
import rms.model.UserInfo;

@Service
public class UserServiceImpl implements UserService {

	UserDao userdao;

	/** SecurityConfig.passwordEncoder: encodes as {bcrypt}, and matches {bcrypt} and legacy plain-text rows. */
	PasswordEncoder passwordEncoder;

	@Autowired
	public void setUserdao(UserDao userdao) {
		this.userdao = userdao;
	}

	@Autowired
	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public List<UserInfo> getAllUsers() {
		return userdao.getAllUsers();
	}

	@Override
	public UserInfo findUserById(String userid) {
		return userdao.findUserById(userid);
	}

	@Override
	public String addUser(UserInfo userinfo, String password) {
		return userdao.addUser(userinfo, passwordEncoder.encode(password));
	}

	@Override
	public void updateUser(UserInfo userinfo) {
		userdao.updateUser(userinfo);
	}

	@Override
	public void setActive(String userid, boolean active) {
		userdao.setActive(userid, active);
	}

	@Override
	public void resetPassword(String userid, String password) {
		userdao.setPassword(userid, passwordEncoder.encode(password), true);
	}

	@Override
	public boolean changePassword(String userid, String currentPassword, String newPassword) {
		String stored = userdao.findPassword(userid);
		if (stored == null || !passwordEncoder.matches(currentPassword, stored)) {
			return false;
		}
		userdao.setPassword(userid, passwordEncoder.encode(newPassword), false);
		return true;
	}

	@Override
	public int countActiveSuperAdmins() {
		return userdao.countActiveSuperAdmins();
	}

}
