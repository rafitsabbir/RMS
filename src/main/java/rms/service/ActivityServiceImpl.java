package rms.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import rms.dao.ActivityDao;
import rms.model.ActivityInfo;

/**
 * The activity log (owner request, 2026-10-03): who marked, selected, rejected, scheduled and so on. Entries are
 * written after the action worked, so a failed action leaves no entry. Details hold codes and keys, never scores,
 * reasons, comments or personal data.
 */
@Service
public class ActivityServiceImpl implements ActivityService {
	private static final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);

	public static final int MAX_ROWS = 500;
	static final int MAX_DETAIL = 300;

	ActivityDao activitydao;

	@Autowired
	public void setActivityDao(ActivityDao activitydao) {
		this.activitydao = activitydao;
	}

	@Override
	public void record(String userid, String action, String entitytype, String entityid, String detail) {
		ActivityInfo activity = new ActivityInfo();
		activity.setUserid(userid == null ? "" : userid);
		activity.setAction(action);
		activity.setEntitytype(entitytype);
		activity.setEntityid(entityid);
		activity.setDetail(detail != null && detail.length() > MAX_DETAIL ? detail.substring(0, MAX_DETAIL) : detail);
		try {
			activitydao.add(activity);
		} catch (DataAccessException e) {
			log.warn("The activity {} by {} couldn't be logged: {}", action, userid,
					e.getMostSpecificCause().getClass().getSimpleName());
		}
	}

	@Override
	public List<ActivityInfo> getRecent(String action) {
		return activitydao.getRecent(action == null || action.isBlank() ? null : action.trim(), MAX_ROWS);
	}

	@Override
	public List<String> getActions() {
		return activitydao.getActions();
	}
}
