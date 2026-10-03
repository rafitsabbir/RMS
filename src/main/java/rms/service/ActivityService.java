package rms.service;

import java.util.List;

import rms.model.ActivityInfo;

public interface ActivityService {

	/**
	 * Writes one entry to the activity log. Never throws: if the log can't be written (for example migration 007 is not
	 * run yet) a WARN is logged and the user's action carries on.
	 */
	public void record(String userid, String action, String entitytype, String entityid, String detail);

	/** The newest entries first (at most {@link ActivityServiceImpl#MAX_ROWS}); one action when action is not blank. */
	public List<ActivityInfo> getRecent(String action);

	public List<String> getActions();
}
