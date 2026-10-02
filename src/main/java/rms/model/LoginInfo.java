package rms.model;

/** One row of the users table: the login name and the stored password (plain text today, G13). */
public class LoginInfo {

	private String userid = null;
	private String username = null;
	private String password = null;

	public LoginInfo() {

	}

	public LoginInfo(String userid, String username, String password) {
		this.userid = userid;
		this.username = username;
		this.password = password;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/** Leaves the password out, so a log line can't leak it. */
	@Override
	public String toString() {
		return "LoginInfo [userid=" + userid + ", username=" + username + "]";
	}
}
