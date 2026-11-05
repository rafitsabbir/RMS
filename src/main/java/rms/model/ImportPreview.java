package rms.model;

import java.util.ArrayList;
import java.util.List;

/** What a candidate import file would do (F4): its lines, or why the file can't be read at all. */
public class ImportPreview {

	private final List<ImportRow> rows = new ArrayList<ImportRow>();
	private String problem = null;

	public List<ImportRow> getRows() {
		return rows;
	}

	/** Why the whole file can't be imported (no header, a missing column, too many lines), or null. */
	public String getProblem() {
		return problem;
	}

	public void setProblem(String problem) {
		this.problem = problem;
	}

	public long getReadycount() {
		return rows.stream().filter(ImportRow::isReady).count();
	}

	public long getDuplicatecount() {
		return rows.stream().filter(ImportRow::isDuplicate).count();
	}

	public long getInvalidcount() {
		return rows.stream().filter(row -> !row.isValid()).count();
	}
}
