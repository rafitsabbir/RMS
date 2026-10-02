package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import rms.dao.DocumentDao;
import rms.dao.DocumentFileStore;
import rms.model.DocumentInfo;
import rms.model.DocumentUpload;

/** Uploads, the limits per kind, replacing, and the permanent delete, with a real file store on a temporary folder. */
@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

	static final byte[] PDF = DocumentRulesTest.PDF;
	static final byte[] PNG = DocumentRulesTest.PNG;

	@TempDir
	Path folder;

	@Mock
	DocumentDao documentdao;

	DocumentServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new DocumentServiceImpl();
		service.setDocumentDao(documentdao);
		service.setDocumentFileStore(new DocumentFileStore(folder.toString()));
	}

	@Test
	void uploadStoresTheFileAndSavesTheRow() throws IOException {
		when(documentdao.addDocument(any(DocumentInfo.class), eq(1))).thenReturn(7);

		Outcome outcome = service.upload(upload("CV", "cv.pdf", "application/pdf", PDF), "U3");

		assertThat(outcome).isEqualTo(new Outcome(true, "CV uploaded."));
		DocumentInfo saved = savedDocument(1);
		assertThat(saved.getCandidateid()).isEqualTo("C1");
		assertThat(saved.getDoctype()).isEqualTo("CV");
		assertThat(saved.getOriginalname()).isEqualTo("cv.pdf");
		assertThat(saved.getContenttype()).isEqualTo("application/pdf");
		assertThat(saved.getFilesize()).isEqualTo(PDF.length);
		assertThat(saved.getUploadedby()).isEqualTo("U3");
		assertThat(saved.getTitle()).isNull();
		assertThat(storedFile(saved.getStoredname())).hasBinaryContent(PDF);
	}

	@Test
	void uploadingASingleSlotKindAgainReplacesIt() {
		when(documentdao.countDocuments("C1", "SSC")).thenReturn(1);
		when(documentdao.addDocument(any(DocumentInfo.class), eq(1))).thenReturn(8);

		Outcome outcome = service.upload(upload("SSC", "ssc.png", "image/png", PNG), "U3");

		assertThat(outcome.done()).isTrue();
		assertThat(outcome.message()).startsWith("SSC certificate replaced. The previous file is kept");
		// maxActive 1: the DAO soft-deletes the active one in the same transaction
		verify(documentdao).addDocument(any(DocumentInfo.class), eq(1));
	}

	@Test
	void professionalCertificateNeedsATitleAndKeepsIssuerAndYear() {
		when(documentdao.addDocument(any(DocumentInfo.class), eq(5))).thenReturn(9);

		Outcome outcome = service.upload(new DocumentUpload("C1", "PROFESSIONAL", " PMP ", " Example Institute ",
				"2024", "pmp.pdf", "application/pdf", PDF), "U1");

		assertThat(outcome).isEqualTo(new Outcome(true, "Professional certificate uploaded."));
		DocumentInfo saved = savedDocument(5);
		assertThat(saved.getTitle()).isEqualTo("PMP");
		assertThat(saved.getIssuer()).isEqualTo("Example Institute");
		assertThat(saved.getIssueyear()).isEqualTo(2024);
	}

	@Test
	void professionalCertificateRefusals() {
		String thisYear = String.valueOf(Year.now().getValue());
		String nextYear = String.valueOf(Year.now().getValue() + 1);
		String yearMessage = "The year must be between 1950 and " + thisYear + ", or empty.";

		expectRefused(professional(" ", "", ""), "Please enter the certificate's title, for example PMP.");
		expectRefused(professional(null, "", ""), "Please enter the certificate's title, for example PMP.");
		expectRefused(professional("x".repeat(101), "", ""), "The title and the issuer can have at most 100 characters.");
		expectRefused(professional("PMP", "x".repeat(101), ""), "The title and the issuer can have at most 100 characters.");
		expectRefused(professional("PMP", "", "1949"), yearMessage);
		expectRefused(professional("PMP", "", nextYear), yearMessage);
		expectRefused(professional("PMP", "", "24"), yearMessage);
		expectRefused(professional("PMP", "", "20x4"), yearMessage);
	}

	@Test
	void sixthProfessionalCertificateIsRefusedBeforeTheFileIsWritten() throws IOException {
		when(documentdao.countDocuments("C1", "PROFESSIONAL")).thenReturn(5);

		Outcome outcome = service.upload(professional("PMP", "", ""), "U3");

		assertThat(outcome).isEqualTo(new Outcome(false,
				"This candidate already has 5 professional certificates. Delete one before adding another."));
		verify(documentdao, never()).addDocument(any(DocumentInfo.class), anyInt());
		assertThat(storedFiles()).isEmpty();
	}

	@Test
	void sixthProfessionalCertificateRefusedByTheInsertLeavesNoFile() throws IOException {
		// Another upload took the fifth place between the count and the insert
		when(documentdao.countDocuments("C1", "PROFESSIONAL")).thenReturn(4);
		when(documentdao.addDocument(any(DocumentInfo.class), eq(5))).thenReturn(0);

		Outcome outcome = service.upload(professional("PMP", "", ""), "U3");

		assertThat(outcome.done()).isFalse();
		assertThat(outcome.message()).startsWith("This candidate already has 5 professional certificates.");
		assertThat(storedFiles()).isEmpty();
	}

	@Test
	void aFailedInsertLeavesNoFile() throws IOException {
		when(documentdao.addDocument(any(DocumentInfo.class), eq(1))).thenThrow(new IllegalStateException("db down"));

		// Rethrown, for the usual error page
		assertThatThrownBy(() -> service.upload(upload("CV", "cv.pdf", "application/pdf", PDF), "U3"))
				.isInstanceOf(IllegalStateException.class);

		assertThat(storedFiles()).isEmpty();
	}

	@Test
	void wrongFilesAreRefusedAndNothingIsStored() throws IOException {
		expectRefused(upload("CV", "cv.docx", "application/pdf", PDF), "Only PDF, JPG and PNG files can be uploaded.");
		expectRefused(upload("CV", "cv.pdf", "text/html", PDF),
				"The file's type doesn't match its name. Only PDF, JPG and PNG files can be uploaded.");
		expectRefused(upload("CV", "cv.pdf", "application/pdf", "<html>".getBytes()),
				"The file's content isn't a PDF, JPG or PNG file.");
		expectRefused(upload("CV", "cv.pdf", "application/pdf", Arrays.copyOf(PDF, (int) DocumentRules.MAX_BYTES + 1)),
				"The file is larger than 5 MB.");
		expectRefused(upload("CV", "cv.pdf", "application/pdf", new byte[0]), "Please choose a file.");
		expectRefused(upload("NOTATYPE", "cv.pdf", "application/pdf", PDF), "Please choose the kind of document.");
		expectRefused(upload("cv", "cv.pdf", "application/pdf", PDF), "Please choose the kind of document.");
		expectRefused(upload(null, "cv.pdf", "application/pdf", PDF), "Please choose the kind of document.");

		assertThat(storedFiles()).isEmpty();
	}

	@Test
	void pathTraversalNameIsOnlyShownNeverUsedAsAPath() throws IOException {
		when(documentdao.addDocument(any(DocumentInfo.class), eq(1))).thenReturn(7);

		service.upload(upload("CV", "../../../outside/evil.pdf", "application/pdf", PDF), "U3");

		DocumentInfo saved = savedDocument(1);
		assertThat(saved.getOriginalname()).isEqualTo("evil.pdf");
		assertThat(saved.getStoredname()).matches("[0-9a-f]{32}");
		assertThat(storedFile(saved.getStoredname())).exists();
		assertThat(folder.getParent().resolve("outside")).doesNotExist();
		assertThat(storedFiles()).hasSize(1);
	}

	@Test
	void withoutStorageNothingIsAccepted() {
		service.setDocumentFileStore(new DocumentFileStore((String) null));

		assertThat(service.isStorageConfigured()).isFalse();
		expectRefused(upload("CV", "cv.pdf", "application/pdf", PDF), DocumentServiceImpl.NOT_CONFIGURED);
		assertThat(service.purge("C1", "Retention period ended", "U1"))
				.isEqualTo(new Outcome(false, DocumentServiceImpl.NOT_CONFIGURED));
		verify(documentdao, never()).getUnpurgedDocuments(anyString());
	}

	@Test
	void findFileReturnsTheStoredFileOrNull() throws IOException {
		String storedname = new DocumentFileStore(folder.toString()).store(PDF);

		assertThat(service.findFile(document(1, storedname))).hasBinaryContent(PDF);
		assertThat(service.findFile(document(2, "a0000000000000000000000000000001"))).isNull();
		assertThat(service.findFile(document(3, "../x"))).isNull();
	}

	@Test
	void deleteIsSoftAndKeepsTheFile() throws IOException {
		String storedname = new DocumentFileStore(folder.toString()).store(PDF);
		when(documentdao.deleteDocument(1, "U3")).thenReturn(true);

		assertThat(service.deleteDocument(1, "U3")).isTrue();

		assertThat(storedFile(storedname)).exists();
	}

	@Test
	void purgeRemovesEveryFileAndMarksTheRows() throws IOException {
		DocumentFileStore store = new DocumentFileStore(folder.toString());
		String active = store.store(PDF);
		String replaced = store.store(PNG);
		when(documentdao.getUnpurgedDocuments("C1")).thenReturn(List.of(document(1, active), document(4, replaced),
				// A row whose file is already gone (a seed row) is marked too
				document(5, "a0000000000000000000000000000005")));

		Outcome outcome = service.purge("C1", "  Retention period ended  ", "U1");

		assertThat(outcome).isEqualTo(new Outcome(true, "3 files deleted permanently."));
		assertThat(storedFiles()).isEmpty();
		verify(documentdao).markPurged(1, "U1", "Retention period ended");
		verify(documentdao).markPurged(4, "U1", "Retention period ended");
		verify(documentdao).markPurged(5, "U1", "Retention period ended");
	}

	@Test
	void purgeNeedsAReason() {
		assertThat(service.purge("C1", " ", "U1")).isEqualTo(
				new Outcome(false, "Please give the reason for deleting the documents permanently."));
		assertThat(service.purge("C1", null, "U1").done()).isFalse();
		assertThat(service.purge("C1", "x".repeat(256), "U1"))
				.isEqualTo(new Outcome(false, "The reason can have at most 255 characters."));

		verify(documentdao, never()).getUnpurgedDocuments(anyString());
	}

	@Test
	void purgeWithNothingStored() {
		when(documentdao.getUnpurgedDocuments("C2")).thenReturn(List.of());

		assertThat(service.purge("C2", "Retention period ended", "U1"))
				.isEqualTo(new Outcome(true, "There were no stored files to delete."));
	}

	@Test
	void purgeKeepsTheRowOfAFileThatCantBeRemoved() throws IOException {
		DocumentFileStore store = new DocumentFileStore(folder.toString());
		String removable = store.store(PDF);
		// A non-empty folder where the file should be: deleting it fails
		String stuck = "b0000000000000000000000000000001";
		Files.createDirectories(folder.resolve("b0").resolve(stuck).resolve("inside"));
		when(documentdao.getUnpurgedDocuments("C1")).thenReturn(List.of(document(1, removable), document(2, stuck)));

		Outcome outcome = service.purge("C1", "Retention period ended", "U1");

		assertThat(outcome).isEqualTo(new Outcome(false,
				"1 file deleted permanently, but 1 file couldn't be removed and is kept. Please tell IT."));
		verify(documentdao).markPurged(1, "U1", "Retention period ended");
		verify(documentdao, never()).markPurged(eq(2), anyString(), anyString());
	}

	@Test
	void purgeCountsARowThatCantBeMarked() throws IOException {
		DocumentFileStore store = new DocumentFileStore(folder.toString());
		String first = store.store(PDF);
		String second = store.store(PNG);
		when(documentdao.getUnpurgedDocuments("C1")).thenReturn(List.of(document(1, first), document(2, second)));
		lenient().doThrow(new QueryTimeoutException("timeout")).when(documentdao)
				.markPurged(2, "U1", "Retention period ended");

		Outcome outcome = service.purge("C1", "Retention period ended", "U1");

		assertThat(outcome.done()).isFalse();
		assertThat(outcome.message()).startsWith("1 file deleted permanently, but 1 file");
		// Both files are gone; a later purge marks row 2 (deleting a missing file does nothing)
		assertThat(storedFiles()).isEmpty();
	}

	private void expectRefused(DocumentUpload upload, String message) {
		assertThat(service.upload(upload, "U3")).isEqualTo(new Outcome(false, message));
		verify(documentdao, never()).addDocument(any(DocumentInfo.class), anyInt());
	}

	private DocumentInfo savedDocument(int maxActive) {
		ArgumentCaptor<DocumentInfo> captor = ArgumentCaptor.forClass(DocumentInfo.class);
		verify(documentdao).addDocument(captor.capture(), eq(maxActive));
		return captor.getValue();
	}

	private Path storedFile(String storedname) {
		return folder.resolve(storedname.substring(0, 2)).resolve(storedname);
	}

	private List<Path> storedFiles() throws IOException {
		try (Stream<Path> files = Files.walk(folder)) {
			return files.filter(Files::isRegularFile).toList();
		}
	}

	private static DocumentUpload upload(String doctype, String name, String type, byte[] content) {
		return new DocumentUpload("C1", doctype, null, null, null, name, type, content);
	}

	private static DocumentUpload professional(String title, String issuer, String year) {
		return new DocumentUpload("C1", "PROFESSIONAL", title, issuer, year, "pmp.pdf", "application/pdf", PDF);
	}

	private static DocumentInfo document(int key, String storedname) {
		DocumentInfo document = new DocumentInfo();
		document.setDocumentkey(key);
		document.setCandidateid("C1");
		document.setStoredname(storedname);
		return document;
	}
}
