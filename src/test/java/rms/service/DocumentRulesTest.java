package rms.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import rms.service.DocumentRules.FileKind;

/** The upload file checks: extension, declared type and first bytes must agree; size; the display name. */
class DocumentRulesTest {

	static final byte[] PDF = "%PDF-1.7 synthetic".getBytes();
	static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10 };
	static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D };

	@Test
	void acceptsPdfJpgAndPngWhoseNameTypeAndContentAgree() {
		assertThat(DocumentRules.problem("cv.pdf", "application/pdf", PDF)).isNull();
		assertThat(DocumentRules.problem("photo.JPG", "image/jpeg", JPEG)).isNull();
		assertThat(DocumentRules.problem("photo.jpeg", "image/jpeg; charset=binary", JPEG)).isNull();
		assertThat(DocumentRules.problem("scan.png", "IMAGE/PNG", PNG)).isNull();
	}

	@Test
	void refusesOtherExtensions() {
		for (String name : new String[] { "cv.docx", "cv.exe", "cv.pdf.exe", "cv", "cv.", "cv.gif" }) {
			assertThat(DocumentRules.problem(name, "application/pdf", PDF)).as(name)
					.isEqualTo("Only PDF, JPG and PNG files can be uploaded.");
		}
	}

	@Test
	void refusesADeclaredTypeThatDoesntMatchTheName() {
		String message = "The file's type doesn't match its name. Only PDF, JPG and PNG files can be uploaded.";
		assertThat(DocumentRules.problem("cv.pdf", "text/html", PDF)).isEqualTo(message);
		assertThat(DocumentRules.problem("cv.pdf", "image/png", PDF)).isEqualTo(message);
		assertThat(DocumentRules.problem("cv.pdf", null, PDF)).isEqualTo(message);
		assertThat(DocumentRules.problem("cv.pdf", "application/octet-stream", PDF)).isEqualTo(message);
	}

	@Test
	void refusesWrongMagicBytes() {
		String message = "The file's content isn't a PDF, JPG or PNG file.";
		// An HTML page renamed to .pdf, with the right declared type
		assertThat(DocumentRules.problem("cv.pdf", "application/pdf", "<html><script>".getBytes())).isEqualTo(message);
		// A PNG named .jpg
		assertThat(DocumentRules.problem("photo.jpg", "image/jpeg", PNG)).isEqualTo(message);
		// Shorter than the signature
		assertThat(DocumentRules.problem("cv.pdf", "application/pdf", "%PD".getBytes())).isEqualTo(message);
	}

	@Test
	void refusesEmptyAndOversizeFiles() {
		assertThat(DocumentRules.problem("cv.pdf", "application/pdf", new byte[0])).isEqualTo("Please choose a file.");
		assertThat(DocumentRules.problem("cv.pdf", "application/pdf", null)).isEqualTo("Please choose a file.");

		byte[] limit = Arrays.copyOf(PDF, (int) DocumentRules.MAX_BYTES);
		assertThat(DocumentRules.problem("cv.pdf", "application/pdf", limit)).isNull();
		byte[] over = Arrays.copyOf(PDF, (int) DocumentRules.MAX_BYTES + 1);
		assertThat(DocumentRules.problem("cv.pdf", "application/pdf", over)).isEqualTo("The file is larger than 5 MB.");
	}

	@Test
	void kindGivesTheCanonicalContentType() {
		assertThat(DocumentRules.kindOf("a.Jpeg")).isEqualTo(FileKind.JPEG);
		assertThat(DocumentRules.kindOf("a.jpg").getContenttype()).isEqualTo("image/jpeg");
		assertThat(DocumentRules.kindOf("a.pdf").getContenttype()).isEqualTo("application/pdf");
		assertThat(DocumentRules.kindOf("a.png").getContenttype()).isEqualTo("image/png");
		assertThat(DocumentRules.kindOf("a.txt")).isNull();
		assertThat(DocumentRules.kindOf(null)).isNull();
	}

	@Test
	void displayNameDropsAnyPath() {
		assertThat(DocumentRules.displayName("../../etc/passwd.pdf")).isEqualTo("passwd.pdf");
		assertThat(DocumentRules.displayName("..\\..\\windows\\win.ini.png")).isEqualTo("win.ini.png");
		assertThat(DocumentRules.displayName("C:\\Users\\someone\\cv.pdf")).isEqualTo("cv.pdf");
		assertThat(DocumentRules.displayName("/cv.pdf")).isEqualTo("cv.pdf");
	}

	@Test
	void displayNameDropsControlAndFormattingCharacters() {
		// A right-to-left override character could disguise the real extension
		assertThat(DocumentRules.displayName("cv\u202Egnp.pdf")).isEqualTo("cvgnp.pdf");
		assertThat(DocumentRules.displayName("c\u0000v\r\n.pdf")).isEqualTo("cv.pdf");
		assertThat(DocumentRules.displayName(" cv.pdf ")).isEqualTo("cv.pdf");
	}

	@Test
	void displayNameIsNeverEmptyOrHidden() {
		assertThat(DocumentRules.displayName(null)).isEqualTo("document");
		assertThat(DocumentRules.displayName("")).isEqualTo("document");
		assertThat(DocumentRules.displayName("../")).isEqualTo("document");
		assertThat(DocumentRules.displayName(".pdf")).isEqualTo("document.pdf");
	}

	@Test
	void displayNameKeepsTheExtensionWhenShortened() {
		String name = DocumentRules.displayName("x".repeat(300) + ".pdf");

		assertThat(name).hasSize(DocumentRules.MAX_NAME).endsWith("x.pdf");
		// Accented letters stay (U+00E9)
		assertThat(DocumentRules.displayName("é".repeat(10) + ".png")).isEqualTo("é".repeat(10) + ".png");
	}
}
