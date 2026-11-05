package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import rms.model.CandidateInfo;
import rms.model.ImportPreview;
import rms.model.ImportRow;
import rms.model.JobInfo;
import rms.model.LanguageInfo;
import rms.model.PositionInfo;

/** F4: each line passes the form's checks and the duplicate check; only confirmed, valid lines are added. */
@ExtendWith(MockitoExtension.class)
class CandidateImportServiceImplTest {

	static final List<String> HEADER = List.of("First name", "Last name", "Position", "Language", "E-mail", "Phone",
			"Source", "Applied date", "Job Id");

	@Mock
	CandidateService candidateservice;

	@Mock
	PositionService positionservice;

	@Mock
	LanguageService languageservice;

	@Mock
	JobService jobservice;

	@Mock
	ActivityService activityservice;

	CandidateImportServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CandidateImportServiceImpl();
		service.setCandidateService(candidateservice);
		service.setPositionService(positionservice);
		service.setLanguageService(languageservice);
		service.setJobService(jobservice);
		service.setActivityService(activityservice);
		PositionInfo engineer = new PositionInfo();
		engineer.setPositionkey(1);
		engineer.setPositionname("SOFTWARE ENGINEER");
		LanguageInfo java = new LanguageInfo();
		java.setLanguagekey(1);
		java.setLanguagename("JAVA");
		lenient().when(positionservice.getAllPosition()).thenReturn(List.of(engineer));
		lenient().when(positionservice.findPositionById(1)).thenReturn(engineer);
		lenient().when(languageservice.getAllLanguage()).thenReturn(List.of(java));
		lenient().when(languageservice.findLanguageById(1)).thenReturn(java);
		lenient().when(candidateservice.findPossibleDuplicates(any(CandidateInfo.class))).thenReturn(List.of());
	}

	@Test
	void validLinesAreReadyWithTheirKeys() {
		ImportPreview preview = service.preview(file(
				line("Nora", "Newcomer", "software engineer", "Java", "nora@example.test", "000 111", "Job board",
						"2026-10-01", "")));

		assertThat(preview.getProblem()).isNull();
		ImportRow row = preview.getRows().get(0);
		assertThat(row.isReady()).isTrue();
		assertThat(row.getLine()).isEqualTo(2);
		CandidateInfo candidate = row.getCandidate();
		assertThat(candidate.getPositionkey()).isEqualTo(1);
		assertThat(candidate.getLanguagekey()).isEqualTo(1);
		assertThat(candidate.getSource()).isEqualTo("JOB_BOARD");
		assertThat(candidate.getApplieddate()).isEqualTo(LocalDate.of(2026, 10, 1));
		verify(candidateservice, never()).addCandidate(any());
	}

	@Test
	void eachLineGetsTheFormsMessageOrANamedOne() {
		ImportPreview preview = service.preview(file(
				line("", "Newcomer", "SOFTWARE ENGINEER", "JAVA", "", "", "", "", ""),
				line("Nora", "Newcomer", "Astronaut", "JAVA", "", "", "", "", ""),
				line("Nora", "Newcomer", "SOFTWARE ENGINEER", "Klingon", "", "", "", "", ""),
				line("Nora", "Newcomer", "SOFTWARE ENGINEER", "JAVA", "not-an-address", "", "", "", ""),
				line("Nora", "Newcomer", "SOFTWARE ENGINEER", "JAVA", "", "", "Newspaper", "", ""),
				line("Nora", "Newcomer", "SOFTWARE ENGINEER", "JAVA", "", "", "", "01/10/2026", ""),
				line("Nora", "Newcomer", "SOFTWARE ENGINEER", "JAVA", "", "", "", "2099-01-01", ""),
				line("Nora", "Newcomer", "", "JAVA", "", "", "", "", "abc"),
				line("Zoë", "Newcomer", "SOFTWARE ENGINEER", "JAVA", "", "", "", "", "")));

		assertThat(preview.getRows()).extracting(ImportRow::getProblem).containsExactly(
				"Please enter the first name.",
				"Position \"Astronaut\" isn't in RMS. Add it under Positions first, or correct it.",
				"Language \"Klingon\" isn't in RMS. Add it under Languages first, or correct it.",
				"Please enter a valid e-mail address, or leave it empty.",
				"Source \"Newspaper\" isn't one of: Referral, Job board, Website, Agency, Walk-in, Other.",
				"Applied date \"01/10/2026\" isn't a date in the form YYYY-MM-DD (e.g. 2026-11-05).",
				"The applied date can't be in the future.",
				"Please choose a job, or none.",
				"Names can use English letters only. " + EnglishText.PROBLEM);
		assertThat(preview.getInvalidcount()).isEqualTo(9);
	}

	@Test
	void aJobIdGivesThePosition() {
		JobInfo job = new JobInfo();
		job.setJobkey(4);
		job.setPositionkey(1);
		when(jobservice.findJobById(4)).thenReturn(job);

		ImportRow row = service.preview(file(line("Nora", "Newcomer", "", "JAVA", "", "", "", "", "4"))).getRows().get(0);

		assertThat(row.isReady()).isTrue();
		assertThat(row.getCandidate().getPositionkey()).isEqualTo(1);
		assertThat(row.getCandidate().getPositionname()).isEqualTo("SOFTWARE ENGINEER");
	}

	@Test
	void duplicatesInRmsAndInTheFileAreFlagged() {
		CandidateInfo existing = new CandidateInfo();
		existing.setCandidateid("C3");
		existing.setFirstname("Nora");
		existing.setLastname("Newcomer");
		existing.setDuplicatereason("same e-mail");
		when(candidateservice.findPossibleDuplicates(argThat(c -> c != null && "Nora".equals(c.getFirstname()))))
				.thenReturn(List.of(existing));

		ImportPreview preview = service.preview(file(
				line("Nora", "Newcomer", "SOFTWARE ENGINEER", "JAVA", "nora@example.test", "", "", "", ""),
				line("Ivo", "Ivanov", "SOFTWARE ENGINEER", "JAVA", "ivo@example.test", "+1 (555) 0100", "", "", ""),
				line("Ivan", "Ivanov", "SOFTWARE ENGINEER", "JAVA", "IVO@example.test", "15550100", "", "", "")));

		assertThat(preview.getRows()).extracting(ImportRow::getDuplicates).containsExactly(
				List.of("C3 Nora Newcomer (same e-mail)"), List.of(),
				List.of("line 3 (same e-mail)", "line 3 (same phone)"));
		assertThat(preview.getReadycount()).isEqualTo(1);
		assertThat(preview.getDuplicatecount()).isEqualTo(2);
	}

	@Test
	void fileProblemsStopTheWholeFile() {
		assertThat(service.preview(List.of()).getProblem()).contains("empty");
		assertThat(service.preview(List.of(List.of("Name", "Mail"))).getProblem()).contains("\"First name\" is missing");
		assertThat(service.preview(List.of(List.of("First name", "Last name", "Language"))).getProblem())
				.contains("Position or a Job Id");
		assertThat(service.preview(List.of(HEADER)).getProblem()).isEqualTo("The file has a header but no candidates.");
		List<List<String>> tooMany = new ArrayList<>();
		tooMany.add(HEADER);
		for (int i = 0; i <= CandidateImportService.MAX_ROWS; i++) {
			tooMany.add(line("A", "B", "SOFTWARE ENGINEER", "JAVA", "", "", "", "", ""));
		}
		assertThat(service.preview(tooMany).getProblem()).contains("at most 500");
	}

	@Test
	void columnsAreFoundByNameInAnyOrderAndOthersAreIgnored() {
		// The candidates export's own columns, as they are written
		List<List<String>> exported = List.of(
				List.of("Candidate ID", "First name", "Last name", "Position", "Language", "E-mail", "Stage"),
				List.of("C1", "Carla", "Candidate", "SOFTWARE ENGINEER", "JAVA", "carla@example.test", "Offer"));

		ImportRow row = service.preview(exported).getRows().get(0);

		assertThat(row.isReady()).isTrue();
		assertThat(row.getCandidate().getCandidateid()).isNull();
		assertThat(row.getCandidate().getEmail()).isEqualTo("carla@example.test");
	}

	@Test
	void importAddsReadyLinesAndDuplicatesOnlyWhenAsked() {
		CandidateInfo existing = new CandidateInfo();
		existing.setCandidateid("C3");
		when(candidateservice.findPossibleDuplicates(argThat(c -> c != null && "Nora".equals(c.getFirstname()))))
				.thenReturn(List.of(existing));
		when(candidateservice.addCandidate(any(CandidateInfo.class))).thenReturn("C7", "C8", "C9");
		List<List<String>> cells = file(
				line("Nora", "Newcomer", "SOFTWARE ENGINEER", "JAVA", "", "", "", "", ""),
				line("Ivo", "Ivanov", "SOFTWARE ENGINEER", "JAVA", "", "", "", "", ""),
				line("", "Nobody", "SOFTWARE ENGINEER", "JAVA", "", "", "", "", ""));

		assertThat(service.importCandidates(cells, false, "U3").message())
				.isEqualTo("1 candidate imported; 2 lines skipped.");
		ArgumentCaptor<CandidateInfo> added = ArgumentCaptor.forClass(CandidateInfo.class);
		verify(candidateservice).addCandidate(added.capture());
		assertThat(added.getValue().getFirstname()).isEqualTo("Ivo");
		verify(activityservice).record("U3", "CANDIDATE_ADDED", "CANDIDATE", "C7", "import");

		assertThat(service.importCandidates(cells, true, "U3").message())
				.isEqualTo("2 candidates imported; 1 line skipped.");
		verify(candidateservice, times(3)).addCandidate(any(CandidateInfo.class));
		verify(activityservice).record("U3", "CANDIDATE_ADDED", "CANDIDATE", "C8", "import, possible duplicate");

		assertThat(service.importCandidates(List.of(), true, "U3").done()).isFalse();
	}

	@Test
	void sourcesAreReadByLabelOrCode() {
		assertThat(CandidateImportServiceImpl.source("walk-in")).isEqualTo(rms.model.CandidateSource.WALK_IN);
		assertThat(CandidateImportServiceImpl.source("JOB_BOARD")).isEqualTo(rms.model.CandidateSource.JOB_BOARD);
		assertThat(CandidateImportServiceImpl.source("job board")).isEqualTo(rms.model.CandidateSource.JOB_BOARD);
		assertThat(CandidateImportServiceImpl.source("Newspaper")).isNull();
	}

	@SafeVarargs
	private static List<List<String>> file(List<String>... lines) {
		List<List<String>> cells = new ArrayList<>();
		cells.add(HEADER);
		cells.addAll(Arrays.asList(lines));
		return cells;
	}

	private static List<String> line(String... cells) {
		return List.of(cells);
	}
}
