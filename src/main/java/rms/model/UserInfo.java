package rms.model;

public class UserInfo {

	private int isactive = 0;
	private String userid = null;
	private String username = null;
	private String password = null;
	private String firstname = null;
	private String lastname = null;
	private String email = null;
	private String phone = null;
	private String designation = null;
	private String isinterviewer = null;
	
	public UserInfo(){
		
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getIsinterviewer() {
		return isinterviewer;
	}

	public void setIsinterviewer(String isinterviewer) {
		this.isinterviewer = isinterviewer;
	}

	@Override
	public String toString() {
		return "UserInfo [isactive=" + isactive + ", userid=" + userid + ", username=" + username + ", password="
				+ password + ", firstname=" + firstname + ", lastname=" + lastname + ", email=" + email + ", phone="
				+ phone + ", designation=" + designation + ", isinterviewer=" + isinterviewer + "]";
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
