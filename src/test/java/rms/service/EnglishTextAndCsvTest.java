package rms.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** The English-only rule and the CSV writer used by the reports. */
class EnglishTextAndCsvTest {

	@Test
	void englishMeansPrintableAscii() {
		assertThat(EnglishText.isEnglish(null)).isTrue();
		assertThat(EnglishText.isEnglish("")).isTrue();
		assertThat(EnglishText.isEnglish("Anna-Marie O'Neil, Jr. (QA) #1 @ 50%")).isTrue();
		assertThat(EnglishText.isEnglish("José")).isFalse();
		assertThat(EnglishText.isEnglish("আমি")).isFalse();
		assertThat(EnglishText.isEnglish("Иван")).isFalse();
		// A line break isn't allowed in a single-line field, but is in a multi-line one
		assertThat(EnglishText.isEnglish("a\nb")).isFalse();
		assertThat(EnglishText.isEnglishMultiline("a\r\nb\tc")).isTrue();
		assertThat(EnglishText.isEnglishMultiline("a\u0000b")).isFalse();
		assertThat(EnglishText.isEnglishMultiline("a b")).isFalse();
	}

	@Test
	void aCsvHasAByteOrderMarkCrlfAndQuotedCells() {
		String csv = new String(new CsvWriter().row("Name", "Note").row("Ann, Lee", "said \"hi\"").row(1, null)
				.toBytes(), StandardCharsets.UTF_8);

		assertThat(csv).isEqualTo("﻿Name,Note\r\n\"Ann, Lee\",\"said \"\"hi\"\"\"\r\n1,\r\n");
	}

	@Test
	void formulaCellsAreMadeText() {
		assertThat(CsvWriter.cell("=SUM(A1:A9)")).isEqualTo("'=SUM(A1:A9)");
		assertThat(CsvWriter.cell("+8801700000000")).isEqualTo("'+8801700000000");
		assertThat(CsvWriter.cell("-1")).isEqualTo("'-1");
		assertThat(CsvWriter.cell("@cmd")).isEqualTo("'@cmd");
		assertThat(CsvWriter.cell("\tx")).isEqualTo("'\tx");
		assertThat(CsvWriter.cell("a=b")).isEqualTo("a=b");
		assertThat(CsvWriter.cell(java.time.LocalDate.of(2026, 10, 3))).isEqualTo("2026-10-03");
	}
}
