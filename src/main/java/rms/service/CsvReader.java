package rms.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a CSV file as Excel writes it (F4, the candidate import): UTF-8 with or without a byte order mark, or
 * Windows-1252 when the bytes aren't UTF-8 ("CSV" rather than "CSV UTF-8" in Excel); a comma or, where Excel uses it,
 * a semicolon between cells (whichever the first line has more of); quoted cells with doubled quotes and line breaks;
 * CRLF or LF line ends. Blank lines are skipped. The counterpart of CsvWriter.
 */
public final class CsvReader {

	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

	private CsvReader() {
	}

	/** The rows, each a list of cells as written (not trimmed). */
	public static List<List<String>> read(byte[] content) {
		String text = decode(content);
		char separator = separator(text);
		List<List<String>> rows = new ArrayList<List<String>>();
		List<String> row = new ArrayList<String>();
		StringBuilder cell = new StringBuilder();
		boolean quoted = false;
		boolean any = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (quoted) {
				if (c == '"') {
					if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
						cell.append('"');
						i++;
					} else {
						quoted = false;
					}
				} else {
					cell.append(c);
				}
			} else if (c == '"') {
				quoted = true;
				any = true;
			} else if (c == separator) {
				row.add(cell.toString());
				cell.setLength(0);
				any = true;
			} else if (c == '\r' || c == '\n') {
				if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
				end(rows, row, cell, any);
				row = new ArrayList<String>();
				any = false;
			} else {
				cell.append(c);
				any = true;
			}
		}
		end(rows, row, cell, any);
		return rows;
	}

	private static void end(List<List<String>> rows, List<String> row, StringBuilder cell, boolean any) {
		row.add(cell.toString());
		cell.setLength(0);
		// A line with nothing on it, or only empty cells, is skipped
		if (any && row.stream().anyMatch(value -> !value.isBlank())) {
			rows.add(row);
		}
	}

	/** UTF-8 (a byte order mark is dropped) when the bytes are valid UTF-8, else Windows-1252. */
	static String decode(byte[] content) {
		int start = content.length >= 3 && (content[0] & 0xFF) == 0xEF && (content[1] & 0xFF) == 0xBB
				&& (content[2] & 0xFF) == 0xBF ? 3 : 0;
		ByteBuffer bytes = ByteBuffer.wrap(content, start, content.length - start);
		try {
			return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT).decode(bytes).toString();
		} catch (CharacterCodingException e) {
			return new String(content, start, content.length - start, WINDOWS_1252);
		}
	}

	/** A semicolon when the first line has more semicolons than commas, else a comma. */
	static char separator(String text) {
		int end = text.indexOf('\n');
		String first = end < 0 ? text : text.substring(0, end);
		long commas = first.chars().filter(c -> c == ',').count();
		long semicolons = first.chars().filter(c -> c == ';').count();
		return semicolons > commas ? ';' : ',';
	}
}
