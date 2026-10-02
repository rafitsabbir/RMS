package rms.model;

import java.io.Serializable;

/** Kept in the HTTP session, so it must be serializable (G36). */
public class UserInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private int isactive = 0;
	private String userid = null;
	private String username = null;
	private String firstname = null;
	private String lastname = null;
	private String email = null;
	private String phone = null;
	private String designation = null;
	private String isinterviewer = null;
	/** The effective role name (Role.of: admin.role, or the isinterviewer fallback); null for none */
	private String role = null;
	/** users.mustchangepassword: 1 after a password reset, until the user sets their own */
	private int mustchangepassword = 0;

	public UserInfo(){

	}

	public String getIsinterviewer() {
		return isinterviewer;
	}

	public void setIsinterviewer(String isinterviewer) {
		this.isinterviewer = isinterviewer;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	/** The role's display name, or empty for none. */
	public String getRolelabel() {
		Role parsed = Role.parse(role);
		return parsed == null ? "" : parsed.getLabel();
	}

	public int getMustchangepassword() {
		return mustchangepassword;
	}

	public void setMustchangepassword(int mustchangepassword) {
		this.mustchangepassword = mustchangepassword;
	}

	@Override
	public String toString() {
		return "UserInfo [isactive=" + isactive + ", userid=" + userid + ", username=" + username
				+ ", firstname=" + firstname + ", lastname=" + lastname + ", email=" + email + ", phone="
				+ phone + ", designation=" + designation + ", isinterviewer=" + isinterviewer + ", role=" + role
				+ ", mustchangepassword=" + mustchangepassword + "]";
	}

	public int getIsactive() {
		return isactive;
	}

	public void setIsactive(int isactive) {
		this.isactive = isactive;
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

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String irstname) {
		this.firstname = irstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}
}
