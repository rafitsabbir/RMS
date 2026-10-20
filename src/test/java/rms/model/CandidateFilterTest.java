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
}
