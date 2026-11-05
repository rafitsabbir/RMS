package rms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.DashboardDao;
import rms.dao.ScheduleDao;
import rms.model.DashboardInfo;
import rms.model.DecisionStatus;
import rms.model.HiringStage;
import rms.model.Role;

/** The home page dashboard (Phase 4): counts only, read from the DAOs on every visit. */
@Service
public class DashboardServiceImpl implements DashboardService {

	/** Reminders (F10): an evaluation is overdue this many days after its interview (owner may change, #35). */
	static final int OVERDUE_DAYS = 3;
	/** Reminders (F10): a candidate is "long at a stage" after this many days there. */
	static final int STUCK_DAYS = 14;

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
			own.setOverdueevaluations(dashboarddao.getOverdueEvaluations(userid, overdueBefore()));
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
		// Every stage, in pipeline order; NULL and unknown texts count as Applied (HiringStage.of)
		for (HiringStage stage : HiringStage.values()) {
			all.getStagecounts().put(stage, 0);
		}
		for (Map.Entry<String, Integer> entry : dashboarddao.countCandidatesByStage().entrySet()) {
			all.getStagecounts().merge(HiringStage.of(entry.getKey()), entry.getValue(), Integer::sum);
		}
		all.setPendingevaluations(dashboarddao.countPendingEvaluations(null));
		all.setUpcominginterviews(scheduledao.countUpcoming(null));
		all.setNextinterviews(scheduledao.getUpcoming(null, NEXT_INTERVIEWS));
		all.setOpenjobs(dashboarddao.countOpenJobs());
		all.setCandidateswithoutcv(dashboarddao.countCandidatesWithoutCv());
		all.setCandidateswithinactiveinterviewer(dashboarddao.countCandidatesWithInactiveInterviewer());
		all.setOverdueevaluations(dashboarddao.getOverdueEvaluations(null, overdueBefore()));
		all.setStuckcandidates(dashboarddao.getStuckCandidates(LocalDate.now().minusDays(STUCK_DAYS)));
		return all;
	}

	/** An evaluation is overdue OVERDUE_DAYS full days after its interview started. */
	private static LocalDateTime overdueBefore() {
		return LocalDateTime.now().minusDays(OVERDUE_DAYS);
	}
}
