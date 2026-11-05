package rms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** The Candidates list's filters: each on its own, combined, and the empty filter. */
class CandidateFilterTest {

	@Test
	void anEmptyFilterIsInactiveAndPassesEveryone() {
		CandidateFilter filter = new CandidateFilter();
		filter.setStatus(" ");
		filter.setSource("");

		assertThat(filter.isActive()).isFalse();
		assertThat(filter.matches(new CandidateInfo())).isTrue();
	}

	@Test
	void jobAnyNoneOrOne() {
		CandidateFilter filter = new CandidateFilter();
		filter.setJobkey(CandidateFilter.NO_JOB);
		assertThat(filter.matches(candidate(0, "S", null, null, 0, 0))).isTrue();
		assertThat(filter.matches(candidate(2, "S", null, null, 0, 0))).isFalse();

		filter.setJobkey(2);
		assertThat(filter.matches(candidate(2, "S", null, null, 0, 0))).isTrue();
		assertThat(filter.matches(candidate(1, "S", null, null, 0, 0))).isFalse();
	}

	@Test
	void statusPendingMeansNoKnownDecision() {
		CandidateFilter filter = new CandidateFilter();
		filter.setStatus(CandidateFilter.PENDING);
		assertThat(filter.matches(candidate(0, null, null, null, 0, 0))).isTrue();
		// A value this version doesn't know shows as Pending, so it filters as Pending
		assertThat(filter.matches(candidate(0, "X", null, null, 0, 0))).isTrue();
		assertThat(filter.matches(candidate(0, "H", null, null, 0, 0))).isFalse();

		filter.setStatus("S");
		// Older rows may be lower case (DecisionStatus.parse)
		assertThat(filter.matches(candidate(0, "s", null, null, 0, 0))).isTrue();
		assertThat(filter.matches(candidate(0, null, null, null, 0, 0))).isFalse();
	}

	@Test
	void sourceOrNotRecorded() {
		CandidateFilter filter = new CandidateFilter();
		filter.setSource(CandidateFilter.NO_SOURCE);
		assertThat(filter.matches(candidate(0, null, null, null, 0, 0))).isTrue();
		assertThat(filter.matches(candidate(0, null, "REFERRAL", null, 0, 0))).isFalse();

		filter.setSource("REFERRAL");
		assertThat(filter.matches(candidate(0, null, "REFERRAL", null, 0, 0))).isTrue();
		assertThat(filter.matches(candidate(0, null, null, null, 0, 0))).isFalse();
	}

	@Test
	void appliedRangeIncludesBothEndsAndLeavesOutNoDate() {
		CandidateFilter filter = new CandidateFilter();
		filter.setAppliedfrom(LocalDate.of(2026, 9, 1));
		filter.setAppliedto(LocalDate.of(2026, 9, 30));

		assertThat(filter.matches(candidate(0, null, null, LocalDate.of(2026, 9, 1), 0, 0))).isTrue();
		assertThat(filter.matches(candidate(0, null, null, LocalDate.of(2026, 9, 30), 0, 0))).isTrue();
		assertThat(filter.matches(candidate(0, null, null, LocalDate.of(2026, 8, 31), 0, 0))).isFalse();
		assertThat(filter.matches(candidate(0, null, null, LocalDate.of(2026, 10, 1), 0, 0))).isFalse();
		assertThat(filter.matches(candidate(0, null, null, null, 0, 0))).isFalse();

		filter.setAppliedfrom(null);
		assertThat(filter.matches(candidate(0, null, null, LocalDate.of(2020, 1, 1), 0, 0))).isTrue();
	}

	@Test
	void noCvAndNotEvaluatedCombineWithTheOthers() {
		CandidateFilter filter = new CandidateFilter();
		filter.setNocv(true);
		filter.setNotevaluated(true);
		filter.setStatus(CandidateFilter.PENDING);

		assertThat(filter.isActive()).isTrue();
		assertThat(filter.matches(candidate(0, null, null, null, 0, 0))).isTrue();
		assertThat(filter.matches(candidate(0, null, null, null, 1, 0))).isFalse();
		assertThat(filter.matches(candidate(0, null, null, null, 0, 2))).isFalse();
		assertThat(filter.matches(candidate(0, "R", null, null, 0, 0))).isFalse();
	}

	@Test
	void stageMatchesTheStageReadAsAStage() {
		CandidateFilter filter = new CandidateFilter();
		filter.setStage("APPLIED");
		assertThat(filter.isActive()).isTrue();

		CandidateInfo candidate = candidate(0, null, null, null, 0, 0);
		// NULL reads as Applied
		assertThat(filter.matches(candidate)).isTrue();
		candidate.setStage("interview");
		assertThat(filter.matches(candidate)).isFalse();
		filter.setStage("INTERVIEW");
		assertThat(filter.matches(candidate)).isTrue();
	}

	private static CandidateInfo candidate(int jobkey, String status, String source, LocalDate applied, int cvs,
			int evaluations) {
		CandidateInfo candidate = new CandidateInfo();
		candidate.setJobkey(jobkey);
		candidate.setCandidatestatus(status);
		candidate.setSource(source);
		candidate.setApplieddate(applied);
		candidate.setCvcount(cvs);
		candidate.setEvaluationcount(evaluations);
		return candidate;
	}

	@Test
	void searchFindsEveryWordInTheIdNameEmailOrPhone() {
		CandidateInfo nora = new CandidateInfo();
		nora.setCandidateid("C12");
		nora.setFirstname("Nora");
		nora.setLastname("Newcomer");
		nora.setEmail("nora.n@example.test");
		nora.setPhone("+1 (555) 010-0123");
		CandidateFilter filter = new CandidateFilter();

		filter.setQ("  ");
		assertThat(filter.isActive()).isFalse();
		for (String hit : new String[] { "nora", "NEWCOMER", "nora newcomer", "c12", "example.test", "555 010",
				"5550100123", "(555)", "n@ex" }) {
			filter.setQ(hit);
			assertThat(filter.isActive()).isTrue();
			assertThat(filter.matches(nora)).as(hit).isTrue();
		}
		for (String miss : new String[] { "nora smith", "c13", "999", "ivan" }) {
			filter.setQ(miss);
			assertThat(filter.matches(nora)).as(miss).isFalse();
		}
		// Missing fields don't break the search
		filter.setQ("nora");
		assertThat(filter.matches(new CandidateInfo())).isFalse();
	}
}
