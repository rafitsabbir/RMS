package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.dao.CandidateDao;
import rms.dao.DashboardDao;
import rms.dao.JobDao;
import rms.model.CandidateInfo;
import rms.model.DecisionTime;
import rms.model.HiringGroup;
import rms.model.HiringReport;
import rms.model.JobInfo;

/** The hiring report: rows per job and per source, decision times, and the per-interviewer list as the DAO gives it. */
@ExtendWith(MockitoExtension.class)
class HiringReportServiceImplTest {

	@Mock
	CandidateDao candidatedao;

	@Mock
	JobDao jobdao;

	@Mock
	DashboardDao dashboarddao;

	@InjectMocks
	HiringReportServiceImpl service;

	@Test
	void candidatesAreCountedPerJobAndPerSource() {
		when(jobdao.getAllJob()).thenReturn(List.of(job(1, "OPEN"), job(2, "CLOSED"), job(4, "OPEN")));
		when(candidatedao.getAllCandidate()).thenReturn(List.of(
				candidate(1, "REFERRAL", "S", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 11), 1, 2),
				candidate(1, "REFERRAL", "s", LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 25), 1, 1),
				candidate(1, null, "H", null, null, 0, 0),
				candidate(2, "JOB_BOARD", "R", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 4), 1, 1),
				// Job 3 was deleted: it still gets its row, labelled as deleted
				candidate(3, "Newspaper", null, null, null, 0, 0),
				candidate(0, null, "X", null, null, 1, 0)));
		HiringGroup pending = new HiringGroup("Ivan Interviewer (U2)");
		when(dashboarddao.countPendingEvaluationsByInterviewer()).thenReturn(List.of(pending));

		HiringReport report = service.getReport();

		// Job 4 has no candidates and isn't listed; "No job" comes last
		assertThat(report.getByjob()).extracting(HiringGroup::getLabel, HiringGroup::getTotal, HiringGroup::getPending,
				HiringGroup::getOnhold, HiringGroup::getSelected, HiringGroup::getRejected, HiringGroup::getNocv,
				HiringGroup::getNotevaluated)
				.containsExactly(
						tuple("Job 1: SOFTWARE ENGINEER", 3, 0, 1, 2, 0, 1, 1),
						tuple("Job 2: SOFTWARE ENGINEER (closed)", 1, 0, 0, 0, 1, 0, 0),
						tuple("Job 3 (deleted)", 1, 1, 0, 0, 0, 1, 1),
						tuple("No job", 1, 1, 0, 0, 0, 0, 1));
		// (10 + 20) / 2 days from applying to Selected
		assertThat(report.getByjob().get(0).getAveragedaystoselected()).isEqualTo(15.0);
		assertThat(report.getByjob().get(1).getAveragedaystoselected()).isNull();

		// Known sources in their order, then a text this version doesn't know, then "Not recorded"
		assertThat(report.getBysource()).extracting(HiringGroup::getLabel, HiringGroup::getTotal)
				.containsExactly(tuple("Referral", 2), tuple("Job board", 1), tuple("Newspaper", 1),
						tuple("Not recorded", 2));

		// Every stage in pipeline order, empty ones too; the candidates have no stage, so they are all Applied
		assertThat(report.getBystage()).extracting(HiringGroup::getLabel, HiringGroup::getTotal)
				.containsExactly(tuple("Applied", 6), tuple("Screening", 0), tuple("Interview", 0), tuple("Offer", 0),
						tuple("Hired", 0), tuple("Withdrawn", 0));
		assertThat(report.getOverall().getTotal()).isEqualTo(6);
		assertThat(report.getToselected().getCount()).isEqualTo(2);
		assertThat(report.getToselected().getMedian()).isEqualTo(15.0);
		assertThat(report.getTorejected().getAverage()).isEqualTo(3.0);
		assertThat(report.getPendingbyinterviewer()).containsExactly(pending);
	}

	@Test
	void noCandidatesGiveEmptyRowsAndNoTimes() {
		HiringReport report = service.getReport();

		assertThat(report.getByjob()).isEmpty();
		assertThat(report.getBysource()).isEmpty();
		assertThat(report.getToselected().getCount()).isZero();
		assertThat(report.getToselected().getMedian()).isNull();
	}

	@Test
	void decisionTimesLeaveOutMissingAndImpossibleDates() {
		DecisionTime time = new DecisionTime();
		time.add(candidate(0, null, "S", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), 1, 1));
		time.add(candidate(0, null, "S", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 4), 1, 1));
		time.add(candidate(0, null, "S", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 9), 1, 1));
		time.add(candidate(0, null, "S", LocalDate.of(2026, 9, 1), null, 1, 1));
		// Decided before applying: a data error
		time.add(candidate(0, null, "S", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1), 1, 1));

		assertThat(time.getCount()).isEqualTo(3);
		assertThat(time.getMedian()).isEqualTo(3.0);
		assertThat(time.getAverage()).isEqualTo(4.0);
		assertThat(time.getShortest()).isEqualTo(1);
		assertThat(time.getLongest()).isEqualTo(8);

		time.add(candidate(0, null, "S", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), 1, 1));
		// Even count: the mean of the two middle values (1 and 3)
		assertThat(time.getMedian()).isEqualTo(2.0);
	}

	private static JobInfo job(int key, String status) {
		JobInfo job = new JobInfo();
		job.setJobkey(key);
		job.setPositionkey(1);
		job.setPositionname("SOFTWARE ENGINEER");
		job.setStatus(status);
		return job;
	}

	private static CandidateInfo candidate(int jobkey, String source, String status, LocalDate applied,
			LocalDate decided, int cvs, int evaluations) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setJobkey(jobkey);
		candidate.setSource(source);
		candidate.setCandidatestatus(status);
		candidate.setApplieddate(applied);
		candidate.setDecisiondate(decided);
		candidate.setCvcount(cvs);
		candidate.setEvaluationcount(evaluations);
		return candidate;
	}
}
