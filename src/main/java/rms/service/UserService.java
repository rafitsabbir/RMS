package rms.service;

import java.util.List;

import rms.model.UserInfo;

/** Users and Roles, and changing one's own password. Passwords are given in plain text and stored as {bcrypt}. */
public interface UserService {

	public List<UserInfo> getAllUsers();

	public UserInfo findUserById(String userid);

	/** The generated userid, or null when the username is taken. The user must change the password at first login. */
	public String addUser(UserInfo userinfo, String password);

	public void updateUser(UserInfo userinfo);

	public void setActive(String userid, boolean active);

	/** A temporary password the user must change at the next page they open. */
	public void resetPassword(String userid, String password);

	/** Sets the user's own new password; false (and nothing changes) when the current one is wrong. */
	public boolean changePassword(String userid, String currentPassword, String newPassword);

	public int countActiveSuperAdmins();
}
