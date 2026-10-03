package rms.service;

import java.nio.charset.StandardCharsets;

/**
 * Builds a CSV file for Excel: UTF-8 with a byte order mark, CRLF line ends, every cell quoted when needed. A cell that
 * starts with = + - @ (or a tab or line break) gets a leading apostrophe so Excel shows it as text and never runs it as
 * a formula (CSV injection). The apostrophe also appears before phone numbers that start with +.
 */
public final class CsvWriter {

	private final StringBuilder out = new StringBuilder("﻿");

	/** Adds one row; null cells are empty. */
	public CsvWriter row(Object... cells) {
		for (int i = 0; i < cells.length; i++) {
			if (i > 0) {
				out.append(',');
			}
			out.append(cell(cells[i]));
		}
		out.append("\r\n");
		return this;
	}

	public byte[] toBytes() {
		return out.toString().getBytes(StandardCharsets.UTF_8);
	}

	static String cell(Object value) {
		if (value == null) {
			return "";
		}
		String text = value.toString();
		if (!text.isEmpty() && "=+-@\t\r\n".indexOf(text.charAt(0)) >= 0) {
			text = "'" + text;
		}
		boolean quote = text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0
				|| text.indexOf('\r') >= 0;
		return quote ? '"' + text.replace("\"", "\"\"") + '"' : text;
	}
}
