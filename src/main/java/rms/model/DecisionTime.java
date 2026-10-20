package rms.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * How long decisions took: the days from the applied date to the decision date of the candidates with that decision
 * and both dates. The decision date is the current decision's (candidate.decisiondate); an earlier On hold doesn't
 * count separately. A decision dated before the application is a data error and is left out.
 */
public class DecisionTime {

	private final List<Long> days = new ArrayList<Long>();

	/** The days from applying to the decision, or null without both dates or when the decision is earlier. */
	public static Long daysToDecision(CandidateInfo candidate) {
		LocalDate applied = candidate.getApplieddate();
		LocalDate decided = candidate.getDecisiondate();
		if (applied == null || decided == null || decided.isBefore(applied)) {
			return null;
		}
		return ChronoUnit.DAYS.between(applied, decided);
	}

	public void add(CandidateInfo candidate) {
		Long value = daysToDecision(candidate);
		if (value != null) {
			days.add(value);
		}
	}

	/** Candidates counted (with both dates). */
	public int getCount() {
		return days.size();
	}

	/** The average, one decimal, or null with none. */
	public Double getAverage() {
		return days.isEmpty() ? null
				: Math.round(10.0 * days.stream().mapToLong(Long::longValue).sum() / days.size()) / 10.0;
	}

	/** The median, or null with none (the mean of the two middle values for an even count, one decimal). */
	public Double getMedian() {
		if (days.isEmpty()) {
			return null;
		}
		List<Long> sorted = days.stream().sorted().toList();
		int middle = sorted.size() / 2;
		return sorted.size() % 2 == 1 ? sorted.get(middle).doubleValue()
				: Math.round(10.0 * (sorted.get(middle - 1) + sorted.get(middle)) / 2) / 10.0;
	}

	/** The fastest and slowest, or null with none. */
	public Long getShortest() {
		return days.stream().min(Long::compare).orElse(null);
	}

	public Long getLongest() {
		return days.stream().max(Long::compare).orElse(null);
	}
}
