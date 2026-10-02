package rms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Decision statuses, criteria, an evaluation's total and the averages (Phase 3). */
class EvaluationModelTest {

	@Test
	void decisionStatusesAndPending() {
		assertThat(DecisionStatus.parse(" s ")).isEqualTo(DecisionStatus.S);
		assertThat(DecisionStatus.parse("h")).isEqualTo(DecisionStatus.H);
		assertThat(DecisionStatus.parse("X")).isNull();
		assertThat(DecisionStatus.parse(null)).isNull();
		assertThat(DecisionStatus.labelOf("r")).isEqualTo("Rejected");
		assertThat(DecisionStatus.labelOf(null)).isEqualTo("Pending");
		assertThat(DecisionStatus.labelOf("")).isEqualTo("Pending");
		// Selected and Rejected lock the evaluations; On hold and Pending don't (decision I)
		assertThat(DecisionStatus.locks("S")).isTrue();
		assertThat(DecisionStatus.locks("r")).isTrue();
		assertThat(DecisionStatus.locks("H")).isFalse();
		assertThat(DecisionStatus.locks(null)).isFalse();
	}

	@Test
	void tenCriteriaInColumnOrder() {
		assertThat(Criterion.values()).extracting(Criterion::getField).containsExactly("workexp", "techknowledge",
				"leadership", "decision", "probsolving", "stress", "education", "comskill", "attitude", "personality");
		assertThat(Criterion.STRESS.getCommentfield()).isEqualTo("stresscomment");
	}

	@Test
	void evaluationTotalAndComments() {
		MarksInfo marks = scored(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		marks.setComment(Criterion.ATTITUDE, "Positive.");
		marks.setComments("Private remark");

		assertThat(marks.getTotal()).isEqualTo(55);
		assertThat(marks.getScores()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		assertThat(marks.getCriterioncomments().get(8)).isEqualTo("Positive.");
		assertThat(marks.getAttitudecomment()).isEqualTo("Positive.");
		assertThat(marks.toString()).doesNotContain("Private").doesNotContain("Positive");
	}

	@Test
	void summaryAveragesEachCriterionAndTotalsTheAverages() {
		ResultInfo summary = ResultInfo.summarize(List.of(scored(8, 9, 7, 8, 9, 6, 8, 7, 9, 8),
				scored(6, 7, 8, 6, 7, 8, 7, 8, 7, 6)));

		assertThat(summary.getEvaluations()).isEqualTo(2);
		assertThat(summary.getRoundedaverages()).extracting(BigDecimal::toPlainString)
				.containsExactly("7.0", "8.0", "7.5", "7.0", "8.0", "7.0", "7.5", "7.5", "8.0", "7.0");
		assertThat(summary.getTotal()).isEqualByComparingTo("74.5");
	}

	@Test
	void totalIsOfTheUnroundedAverages() {
		// Three evaluations: averages of 7.333... ten times; rounded each is 7.3 (73.0), the true total is 73.3
		ResultInfo summary = ResultInfo.summarize(List.of(scored(7), scored(7), scored(8)));

		assertThat(summary.getRoundedaverages().get(0)).isEqualByComparingTo("7.3");
		assertThat(summary.getTotal()).isEqualByComparingTo("73.3");
	}

	@Test
	void noEvaluationsMeansNoAverages() {
		ResultInfo summary = ResultInfo.summarize(List.of());

		assertThat(summary.getEvaluations()).isZero();
		assertThat(summary.getAverages()).hasSize(10).containsOnlyNulls();
		assertThat(summary.getRoundedaverages()).containsOnlyNulls();
		assertThat(summary.getTotal()).isNull();
	}

	@Test
	void labels() {
		CandidateInfo candidate = new CandidateInfo();
		assertThat(candidate.getStatuslabel()).isEqualTo("Pending");
		assertThat(candidate.isLocked()).isFalse();
		candidate.setCandidatestatus("s");
		assertThat(candidate.getStatuslabel()).isEqualTo("Selected");
		assertThat(candidate.isLocked()).isTrue();
		DecisionInfo decision = new DecisionInfo();
		decision.setStatus("H");
		decision.setReason("Private reason");
		assertThat(decision.getStatuslabel()).isEqualTo("On hold");
		assertThat(decision.toString()).doesNotContain("Private");
	}

	private static MarksInfo scored(int... scores) {
		int[] all = scores.length == 1 ? fill(scores[0]) : scores;
		MarksInfo marks = new MarksInfo();
		for (Criterion criterion : Criterion.values()) {
			marks.setScore(criterion, all[criterion.ordinal()]);
		}
		return marks;
	}

	private static int[] fill(int score) {
		int[] all = new int[10];
		Arrays.fill(all, score);
		return all;
	}
}
