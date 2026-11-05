package rms.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/** F4: CSV files as Excel saves them. */
class CsvReaderTest {

	@Test
	void commasQuotesLineBreaksAndBlankLines() {
		String csv = "First name,Last name,Note\r\n"
				+ "Nora,\"Newcomer, Jr.\",\"said \"\"hi\"\"\"\r\n"
				+ "\r\n"
				+ ",,\r\n"
				+ "Ivo,Ivanov,\"two\nlines\"\n"
				+ "Last,Line,";

		List<List<String>> rows = CsvReader.read(csv.getBytes(StandardCharsets.UTF_8));

		assertThat(rows).containsExactly(List.of("First name", "Last name", "Note"),
				List.of("Nora", "Newcomer, Jr.", "said \"hi\""), List.of("Ivo", "Ivanov", "two\nlines"),
				List.of("Last", "Line", ""));
	}

	@Test
	void excelsSemicolonsByteOrderMarkAndWindows1252() {
		byte[] bom = "﻿First name;Last name\r\nZoe;Zed\r\n".getBytes(StandardCharsets.UTF_8);
		assertThat(CsvReader.read(bom)).containsExactly(List.of("First name", "Last name"), List.of("Zoe", "Zed"));

		// Plain "CSV" from Excel: Windows-1252, not UTF-8
		byte[] ansi = "First name,Last name\r\nRené,Müller\r\n".getBytes(Charset.forName("windows-1252"));
		assertThat(CsvReader.read(ansi).get(1)).containsExactly("René", "Müller");
	}

	@Test
	void anEmptyFileHasNoRows() {
		assertThat(CsvReader.read(new byte[0])).isEmpty();
		assertThat(CsvReader.read("\r\n\r\n".getBytes(StandardCharsets.UTF_8))).isEmpty();
	}
}
