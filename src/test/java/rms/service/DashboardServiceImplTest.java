package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.dao.DashboardDao;
import rms.dao.ScheduleDao;
import rms.model.DashboardInfo;
import rms.model.Role;
import rms.model.ScheduleInfo;

/** The dashboard figures by role: staff see everything, an interviewer only their own two. */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

	@Mock
	DashboardDao dashboarddao;

	@Mock
	ScheduleDao scheduledao;

	DashboardServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new DashboardServiceImpl();
		service.setDashboardDao(dashboarddao);
		service.setScheduleDao(scheduledao);
	}

	@Test
	void staffGetEveryFigure() {
		List<ScheduleInfo> next = List.of(new ScheduleInfo());
		when(dashboarddao.countCandidatesByStatus()).thenReturn(Map.of("S", 3, "R", 2, "H", 1, "", 4));
		when(dashboarddao.countPendingEvaluations(null)).thenReturn(7);
		when(scheduledao.countUpcoming(null)).thenReturn(5);
		when(scheduledao.getUpcoming(null, DashboardServiceImpl.NEXT_INTERVIEWS)).thenReturn(next);
		when(dashboarddao.countOpenJobs()).thenReturn(2);
		when(dashboarddao.countCandidatesWithoutCv()).thenReturn(6);
		when(dashboarddao.countCandidatesWithInactiveInterviewer()).thenReturn(1);

		for (Role role : List.of(Role.SUPER_ADMIN, Role.HR, Role.HIRING_MANAGER)) {
			DashboardInfo dashboard = service.getDashboard(role, "U1");

			assertThat(dashboard.isPersonal()).as(role.name()).isFalse();
			assertThat(dashboard.getSelected()).isEqualTo(3);
			assertThat(dashboard.getRejected()).isEqualTo(2);
			assertThat(dashboard.getOnhold()).isEqualTo(1);
			assertThat(dashboard.getPending()).isEqualTo(4);
			assertThat(dashboard.getTotalcandidates()).isEqualTo(10);
			assertThat(dashboard.getPendingevaluations()).isEqualTo(7);
			assertThat(dashboard.getUpcominginterviews()).isEqualTo(5);
			assertThat(dashboard.getNextinterviews()).isSameAs(next);
			assertThat(dashboard.getOpenjobs()).isEqualTo(2);
			assertThat(dashboard.getCandidateswithoutcv()).isEqualTo(6);
			assertThat(dashboard.getCandidateswithinactiveinterviewer()).isEqualTo(1);
		}
	}

	@Test
	void anUnknownOrLowerCaseStatusCountsLikeTheProfileShowsIt() {
		// Pending is NULL or any text RMS doesn't know (DecisionStatus.labelOf); S, R and H are read in any case
		when(dashboarddao.countCandidatesByStatus()).thenReturn(Map.of("S", 1, "X", 2, "", 3, "h", 4));

		DashboardInfo dashboard = service.getDashboard(Role.HR, "U3");

		assertThat(dashboard.getSelected()).isEqualTo(1);
		assertThat(dashboard.getOnhold()).isEqualTo(4);
		assertThat(dashboard.getPending()).isEqualTo(5);
	}

	@Test
	void anInterviewerGetsOnlyTheirOwnFigures() {
		List<ScheduleInfo> next = List.of(new ScheduleInfo());
		when(dashboarddao.countPendingEvaluations("U2")).thenReturn(2);
		when(scheduledao.countUpcoming("U2")).thenReturn(1);
		when(scheduledao.getUpcoming("U2", DashboardServiceImpl.NEXT_INTERVIEWS)).thenReturn(next);

		DashboardInfo dashboard = service.getDashboard(Role.INTERVIEWER, "U2");

		assertThat(dashboard.isPersonal()).isTrue();
		assertThat(dashboard.getPendingevaluations()).isEqualTo(2);
		assertThat(dashboard.getUpcominginterviews()).isEqualTo(1);
		assertThat(dashboard.getNextinterviews()).isSameAs(next);
		// The staff figures aren't read at all
		verify(dashboarddao, never()).countCandidatesByStatus();
		verify(dashboarddao, never()).countOpenJobs();
		verify(dashboarddao, never()).countCandidatesWithoutCv();
		verify(dashboarddao, never()).countCandidatesWithInactiveInterviewer();
	}

	@Test
	void noRoleGetsNothingAndReadsNothing() {
		assertThat(service.getDashboard(null, "U6")).isNull();
		assertThat(service.getDashboard(Role.INTERVIEWER, null)).isNull();

		verifyNoInteractions(dashboarddao, scheduledao);
	}
}
