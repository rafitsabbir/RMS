package rms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** One row of the activity log: who did what to which record, and when. The detail never holds personal data. */
public class ActivityInfo {

	private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private long activitykey = 0;
	private String userid = null;
	private String username = null;
	private String action = null;
	private String entitytype = null;
	private String entityid = null;
	private String detail = null;
	private LocalDateTime createdat = null;

	/** The action as a readable label, for example SELECTED becomes "Selected". */
	public String getActionlabel() {
		if (action == null || action.isEmpty()) {
			return "";
		}
		String text = action.replace('_', ' ').toLowerCase(Locale.ROOT);
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	public String getCreatedlabel() {
		return createdat == null ? "" : LABEL.format(createdat);
	}

	@Override
	public String toString() {
		return "ActivityInfo [activitykey=" + activitykey + ", userid=" + userid + ", action=" + action
				+ ", entitytype=" + entitytype + ", entityid=" + entityid + "]";
	}

	public long getActivitykey() {
		return activitykey;
	}

	public void setActivitykey(long activitykey) {
		this.activitykey = activitykey;
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

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getEntitytype() {
		return entitytype;
	}

	public void setEntitytype(String entitytype) {
		this.entitytype = entitytype;
	}

	public String getEntityid() {
		return entityid;
	}

	public void setEntityid(String entityid) {
		this.entityid = entityid;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public LocalDateTime getCreatedat() {
		return createdat;
	}

	public void setCreatedat(LocalDateTime createdat) {
		this.createdat = createdat;
	}
}
