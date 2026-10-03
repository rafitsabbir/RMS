package rms.dao;

import java.util.List;

import rms.model.ActivityInfo;

public interface ActivityDao {

	public void add(ActivityInfo activity);

	/** The newest entries first, at most limit; only one action when action is not null. */
	public List<ActivityInfo> getRecent(String action, int limit);

	/** The action names that appear in the log, for the filter. */
	public List<String> getActions();
}
