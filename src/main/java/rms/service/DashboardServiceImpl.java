package rms.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.DashboardDao;
import rms.dao.ScheduleDao;
import rms.model.DashboardInfo;
import rms.model.DecisionStatus;
import rms.model.Role;

/** The home page dashboard (Phase 4): counts only, read from the DAOs on every visit. */
@Service
public class DashboardServiceImpl implements DashboardService {

	/** How many upcoming interviews the home page lists. */
	static final int NEXT_INTERVIEWS = 5;

	DashboardDao dashboarddao;
	ScheduleDao scheduledao;

	@Autowired
	public void setDashboardDao(DashboardDao dashboarddao) {
		this.dashboarddao = dashboarddao;
	}

	@Autowired
	public void setScheduleDao(ScheduleDao scheduledao) {
		this.scheduledao = scheduledao;
	}

	@Override
	public DashboardInfo getDashboard(Role role, String userid) {
		if (role == Role.INTERVIEWER && userid != null) {
			DashboardInfo own = new DashboardInfo();
			own.setPersonal(true);
			own.setPendingevaluations(dashboarddao.countPendingEvaluations(userid));
			own.setUpcominginterviews(scheduledao.countUpcoming(userid));
			own.setNextinterviews(scheduledao.getUpcoming(userid, NEXT_INTERVIEWS));
			return own;
		}
		if (role != Role.SUPER_ADMIN && role != Role.HR && role != Role.HIRING_MANAGER) {
			return null;
		}
		DashboardInfo all = new DashboardInfo();
		Map<String, Integer> bystatus = dashboarddao.countCandidatesByStatus();
		for (Map.Entry<String, Integer> entry : bystatus.entrySet()) {
			DecisionStatus status = DecisionStatus.parse(entry.getKey());
			int count = entry.getValue();
			if (status == DecisionStatus.S) {
				all.setSelected(all.getSelected() + count);
			} else if (status == DecisionStatus.R) {
				all.setRejected(all.getRejected() + count);
			} else if (status == DecisionStatus.H) {
				all.setOnhold(all.getOnhold() + count);
			} else {
				// No status, or one RMS doesn't know, shows as Pending everywhere (DecisionStatus.labelOf)
				all.setPending(all.getPending() + count);
			}
		}
		all.setPendingevaluations(dashboarddao.countPendingEvaluations(null));
		all.setUpcominginterviews(scheduledao.countUpcoming(null));
		all.setNextinterviews(scheduledao.getUpcoming(null, NEXT_INTERVIEWS));
		all.setOpenjobs(dashboarddao.countOpenJobs());
		all.setCandidateswithoutcv(dashboarddao.countCandidatesWithoutCv());
		all.setCandidateswithinactiveinterviewer(dashboarddao.countCandidatesWithInactiveInterviewer());
		return all;
	}
}
