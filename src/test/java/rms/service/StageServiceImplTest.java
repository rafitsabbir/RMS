package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.dao.CandidateDao;
import rms.dao.StageDao;
import rms.model.CandidateInfo;
import rms.model.HiringStage;

/** The hiring stage rules (owner decision 2026-10-20): moves by hand, the link to scheduling, and the log entry. */
@ExtendWith(MockitoExtension.class)
class StageServiceImplTest {

	@Mock
	StageDao stagedao;

	@Mock
	CandidateDao candidatedao;

	@Mock
	ActivityService activityservice;

	@InjectMocks
	StageServiceImpl service;

	@Test
	void aMoveIsSavedAndLoggedWithTheCodesOnly() {
		given("C1", null, "SCREENING");
		when(stagedao.moveStage("C1", "SCREENING", "INTERVIEW", "Phone screen passed", "U3")).thenReturn(true);

		assertThat(service.moveStage("C1", "SCREENING", "interview", "  Phone screen passed ", "U3"))
				.isEqualTo(Outcome.ok("Moved to Interview."));
		verify(activityservice).record("U3", "STAGE_CHANGED", "CANDIDATE", "C1", "SCREENING>INTERVIEW");
	}

	@Test
	void offerAndHiredNeedSelected() {
		given("C1", "H", "INTERVIEW");
		assertThat(service.moveStage("C1", "INTERVIEW", "OFFER", null, "U3").message()).contains("Record Selected first");
		assertThat(service.moveStage("C1", "INTERVIEW", "HIRED", null, "U3").done()).isFalse();

		given("C1", "s", "OFFER");
		when(stagedao.moveStage("C1", "OFFER", "HIRED", "", "U3")).thenReturn(true);
		assertThat(service.moveStage("C1", "OFFER", "HIRED", null, "U3").done()).isTrue();
	}

	@Test
	void nothingMovesWhileRejected() {
		given("C2", "R", "INTERVIEW");

		assertThat(service.moveStage("C2", "INTERVIEW", "WITHDRAWN", null, "U3").message())
				.contains("Rejected", "Change the decision first");
		verify(stagedao, never()).moveStage(anyString(), any(), anyString(), any(), anyString());
	}

	@Test
	void hiredAndWithdrawnMayBeLeftAgainForMistakes() {
		given("C1", null, "WITHDRAWN");
		when(stagedao.moveStage("C1", "WITHDRAWN", "SCREENING", "", "U3")).thenReturn(true);

		assertThat(service.moveStage("C1", "WITHDRAWN", "SCREENING", "", "U3").done()).isTrue();
	}

	@Test
	void badInputIsRefused() {
		assertThat(service.moveStage("C1", "APPLIED", "REJECTED", null, "U3").message()).isEqualTo("Please choose a stage.");
		assertThat(service.moveStage("C9", "APPLIED", "SCREENING", null, "U3").message()).contains("no longer exists");

		given("C1", null, "APPLIED");
		assertThat(service.moveStage("C1", "APPLIED", "APPLIED", null, "U3").message()).contains("already at Applied");
		assertThat(service.moveStage("C1", "APPLIED", "SCREENING", "Café chat", "U3").message()).contains("English");
		assertThat(service.moveStage("C1", "APPLIED", "SCREENING", "x".repeat(501), "U3").message()).contains("500");
		// A post without the form's stage can't be checked for staleness
		assertThat(service.moveStage("C1", " ", "SCREENING", null, "U3").message()).contains("reload");
		verify(stagedao, never()).moveStage(anyString(), any(), anyString(), any(), anyString());
	}

	@Test
	void aStaleFormIsRefused() {
		// The form showed Applied; meanwhile someone moved the candidate to Interview
		given("C1", null, "INTERVIEW");
		assertThat(service.moveStage("C1", "APPLIED", "SCREENING", null, "U3").message())
				.contains("changed meanwhile", "now Interview");

		// Or between the read and the update (the DAO's guard)
		given("C1", null, "APPLIED");
		when(stagedao.moveStage("C1", "APPLIED", "SCREENING", "", "U3")).thenReturn(false);
		assertThat(service.moveStage("C1", "APPLIED", "SCREENING", null, "U3").message())
				.contains("stage or the decision was changed meanwhile");
		verify(activityservice, never()).record(anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void aNullStageIsApplied() {
		given("C1", null, null);
		when(stagedao.moveStage("C1", null, "SCREENING", "", "U3")).thenReturn(true);

		assertThat(service.moveStage("C1", "APPLIED", "SCREENING", null, "U3").done()).isTrue();
		verify(activityservice).record("U3", "STAGE_CHANGED", "CANDIDATE", "C1", "APPLIED>SCREENING");
	}

	@Test
	void schedulingMovesAppliedAndScreeningToInterview() {
		given("C1", "H", "SCREENING");
		when(stagedao.moveStage("C1", "SCREENING", "INTERVIEW", "Interview scheduled", "U3")).thenReturn(true);

		service.interviewScheduled("C1", "U3");

		verify(activityservice).record("U3", "STAGE_CHANGED", "CANDIDATE", "C1", "SCREENING>INTERVIEW");
	}

	@Test
	void schedulingLeavesLaterStagesAndRejectedCandidates() {
		given("C1", "S", "OFFER");
		service.interviewScheduled("C1", "U3");
		given("C2", "R", "APPLIED");
		service.interviewScheduled("C2", "U3");
		service.interviewScheduled("C9", "U3");

		verify(stagedao, never()).moveStage(anyString(), any(), anyString(), any(), anyString());
		verifyNoInteractions(activityservice);
	}

	@Test
	void aFailedMoveAfterSchedulingIsOnlyLogged() {
		given("C1", null, "APPLIED");
		when(stagedao.moveStage("C1", "APPLIED", "INTERVIEW", "Interview scheduled", "U3"))
				.thenThrow(new org.springframework.dao.DataAccessResourceFailureException("down"));

		service.interviewScheduled("C1", "U3");

		verifyNoInteractions(activityservice);
	}

	@Test
	void stagesReadAsApplied() {
		assertThat(HiringStage.of(null)).isEqualTo(HiringStage.APPLIED);
		assertThat(HiringStage.of(" offer ")).isEqualTo(HiringStage.OFFER);
		assertThat(HiringStage.of("REJECTED")).isEqualTo(HiringStage.APPLIED);
		assertThat(HiringStage.parse("REJECTED")).isNull();
		assertThat(HiringStage.PIPELINE).doesNotContain(HiringStage.WITHDRAWN).hasSize(5);
	}

	private void given(String candidateid, String status, String stage) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid(candidateid);
		candidate.setCandidatestatus(status);
		candidate.setStage(stage);
		when(candidatedao.findCandidateById(candidateid)).thenReturn(candidate);
	}
}
