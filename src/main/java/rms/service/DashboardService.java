package rms.service;

import rms.model.DashboardInfo;
import rms.model.Role;

public interface DashboardService {

	/**
	 * The home page figures for a role: everything for Super Admin, HR and Hiring Manager; an Interviewer's own
	 * pending evaluations and upcoming interviews. Null for no role.
	 */
	public DashboardInfo getDashboard(Role role, String userid);
}
