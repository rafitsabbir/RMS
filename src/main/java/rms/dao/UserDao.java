package rms.dao;

import java.util.List;

import rms.model.UserInfo;

/** Users and Roles: the users row (login) and the admin row (profile, role, active flag) of each user. */
public interface UserDao {

	/** Every admin row, active or not, with its username; username is null when there is no users row. */
	public List<UserInfo> getAllUsers();

	/** The user, active or not, or null. */
	public UserInfo findUserById(String userid);

	/**
	 * Adds the users and admin rows in one transaction and returns the generated userid (U1, U2, ...), or null when
	 * the username is taken. The password is stored as given (already encoded) and must be changed at first login.
	 */
	public String addUser(UserInfo userinfo, String encodedPassword);

	/** Profile and role; the username, userid and active flag don't change here. */
	public void updateUser(UserInfo userinfo);

	public void setActive(String userid, boolean active);

	/** Stores an encoded password; mustChange makes the user set their own at the next page they open. */
	public void setPassword(String userid, String encodedPassword, boolean mustChange);

	/** The stored (encoded or legacy plain-text) password, or null when there is no users row. */
	public String findPassword(String userid);

	/** Active users whose effective role is SUPER_ADMIN (admin.role, or isinterviewer N when role is NULL). */
	public int countActiveSuperAdmins();
}
