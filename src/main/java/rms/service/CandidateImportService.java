package rms.service;

import java.util.List;

import rms.model.ImportPreview;

/**
 * Adding candidates from a CSV file (F4, 2026-11-05; Super Admin and HR). Each line passes the same checks as the Add
 * Candidate form (CandidateRules) and the possible-duplicate check; nothing is added until the user confirms.
 */
public interface CandidateImportService {

	/** At most this many candidates per file. */
	int MAX_ROWS = 500;

	/** The columns of the template, in order; the header may name them in any order and case. */
	List<String> COLUMNS = List.of("First name", "Last name", "Position", "Language", "E-mail", "Phone", "Source",
			"Applied date", "Job Id");

	/** What the file (its cells, as CsvReader reads them) would add, line by line. Changes nothing. */
	ImportPreview preview(List<List<String>> cells);

	/**
	 * Adds the file's valid lines, checked again now (the data may have changed since the preview); possible duplicates
	 * only when includeduplicates. Each candidate added is logged as CANDIDATE_ADDED with "import" in the detail.
	 */
	Outcome importCandidates(List<List<String>> cells, boolean includeduplicates, String userid);
}
