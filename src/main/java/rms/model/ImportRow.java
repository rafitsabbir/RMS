package rms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One data line of a candidate import (F4): the candidate it would add, why it can't be added (problem), and possible
 * duplicates (candidates already in RMS, or an earlier line of the same file).
 */
public class ImportRow {

	private final int line;
	private final CandidateInfo candidate;
	private String problem = null;
	private final List<String> duplicates = new ArrayList<String>();

	public ImportRow(int line, CandidateInfo candidate) {
		this.line = line;
		this.candidate = candidate;
	}

	/** The line number in the file, the header being line 1. */
	public int getLine() {
		return line;
	}

	public CandidateInfo getCandidate() {
		return candidate;
	}

	public String getProblem() {
		return problem;
	}

	public void setProblem(String problem) {
		this.problem = problem;
	}

	/** e.g. "C3 Nora Newcomer (same e-mail)" or "line 4 (same phone)". */
	public List<String> getDuplicates() {
		return duplicates;
	}

	public boolean isValid() {
		return problem == null;
	}

	public boolean isDuplicate() {
		return problem == null && !duplicates.isEmpty();
	}

	/** Valid and not a possible duplicate. */
	public boolean isReady() {
		return problem == null && duplicates.isEmpty();
	}
}
