package rms.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.model.CandidateInfo;
import rms.model.CandidateSource;
import rms.model.ImportPreview;
import rms.model.ImportRow;
import rms.model.LanguageInfo;
import rms.model.PositionInfo;

/** The candidate CSV import (F4). Logs carry counts and candidate IDs only. */
@Service
public class CandidateImportServiceImpl implements CandidateImportService {
	private static final Logger log = LoggerFactory.getLogger(CandidateImportServiceImpl.class);

	CandidateService candidateservice;
	PositionService positionservice;
	LanguageService languageservice;
	JobService jobservice;
	ActivityService activityservice;

	@Autowired
	public void setCandidateService(CandidateService candidateservice) {
		this.candidateservice = candidateservice;
	}

	@Autowired
	public void setPositionService(PositionService positionservice) {
		this.positionservice = positionservice;
	}

	@Autowired
	public void setLanguageService(LanguageService languageservice) {
		this.languageservice = languageservice;
	}

	@Autowired
	public void setJobService(JobService jobservice) {
		this.jobservice = jobservice;
	}

	@Autowired
	public void setActivityService(ActivityService activityservice) {
		this.activityservice = activityservice;
	}

	@Override
	public ImportPreview preview(List<List<String>> cells) {
		ImportPreview preview = new ImportPreview();
		if (cells == null || cells.isEmpty()) {
			preview.setProblem("The file is empty. Use the template: a header line, then one candidate per line.");
			return preview;
		}
		Map<String, Integer> columns = new HashMap<String, Integer>();
		List<String> header = cells.get(0);
		for (int i = 0; i < header.size(); i++) {
			columns.putIfAbsent(key(header.get(i)), i);
		}
		for (String required : List.of("First name", "Last name", "Language")) {
			if (!columns.containsKey(key(required))) {
				preview.setProblem("The first line must name the columns, with at least First name, Last name, "
						+ "Position (or Job Id) and Language. \"" + required + "\" is missing. Use the template.");
				return preview;
			}
		}
		if (!columns.containsKey(key("Position")) && !columns.containsKey(key("Job Id"))) {
			preview.setProblem("The first line must name a Position or a Job Id column. Use the template.");
			return preview;
		}
		if (cells.size() - 1 > MAX_ROWS) {
			preview.setProblem("The file has " + (cells.size() - 1) + " candidates; at most " + MAX_ROWS
					+ " can be imported at once. Please split it.");
			return preview;
		}
		if (cells.size() == 1) {
			preview.setProblem("The file has a header but no candidates.");
			return preview;
		}

		Map<String, Integer> positions = new HashMap<String, Integer>();
		for (PositionInfo position : positionservice.getAllPosition()) {
			positions.putIfAbsent(name(position.getPositionname()), position.getPositionkey());
		}
		Map<String, Integer> languages = new HashMap<String, Integer>();
		for (LanguageInfo language : languageservice.getAllLanguage()) {
			languages.putIfAbsent(name(language.getLanguagename()), language.getLanguagekey());
		}
		CandidateRules rules = new CandidateRules(positionservice, languageservice, jobservice);
		Map<String, Integer> emails = new HashMap<String, Integer>();
		Map<String, Integer> phones = new HashMap<String, Integer>();
		Map<String, Integer> names = new HashMap<String, Integer>();

		for (int i = 1; i < cells.size(); i++) {
			List<String> line = cells.get(i);
			CandidateInfo candidate = new CandidateInfo();
			ImportRow row = new ImportRow(i + 1, candidate);
			preview.getRows().add(row);
			candidate.setFirstname(cell(line, columns, "First name"));
			candidate.setLastname(cell(line, columns, "Last name"));
			candidate.setEmail(cell(line, columns, "E-mail"));
			candidate.setPhone(cell(line, columns, "Phone"));
			String position = cell(line, columns, "Position");
			String language = cell(line, columns, "Language");
			candidate.setPositionname(position);
			candidate.setLanguagename(language);
			boolean badjob = false;
			String job = cell(line, columns, "Job Id");
			if (!job.isEmpty()) {
				try {
					candidate.setJobkey(Integer.parseInt(job));
				} catch (NumberFormatException e) {
					badjob = true;
				}
			}
			String source = cell(line, columns, "Source");
			CandidateSource parsed = source(source);
			candidate.setSource(parsed == null ? source : parsed.name());
			boolean baddate = false;
			String applied = cell(line, columns, "Applied date");
			if (!applied.isEmpty()) {
				try {
					candidate.setApplieddate(LocalDate.parse(applied));
				} catch (DateTimeParseException e) {
					baddate = true;
				}
			}
			// A name the lists don't have reads as "none", which the rules then refuse with the form's message;
			// the name is said first, so the line can be corrected
			if (candidate.getJobkey() <= 0 && !position.isEmpty() && !positions.containsKey(name(position))) {
				row.setProblem("Position \"" + position + "\" isn't in RMS. Add it under Positions first, or correct it.");
				continue;
			}
			candidate.setPositionkey(positions.getOrDefault(name(position), 0));
			if (!language.isEmpty() && !languages.containsKey(name(language))) {
				row.setProblem("Language \"" + language + "\" isn't in RMS. Add it under Languages first, or correct it.");
				continue;
			}
			candidate.setLanguagekey(languages.getOrDefault(name(language), 0));
			if (!source.isEmpty() && parsed == null) {
				row.setProblem("Source \"" + source + "\" isn't one of: " + String.join(", ",
						Arrays.stream(CandidateSource.values()).map(CandidateSource::getLabel).toList()) + ".");
				continue;
			}
			if (baddate) {
				row.setProblem("Applied date \"" + applied + "\" isn't a date in the form YYYY-MM-DD (e.g. 2026-11-05).");
				continue;
			}
			String problem = rules.check(candidate, null, badjob, false);
			if (problem != null) {
				row.setProblem(problem);
				continue;
			}
			if (candidate.getJobkey() > 0 && position.isEmpty()) {
				candidate.setPositionname(positionservice.findPositionById(candidate.getPositionkey()) == null ? ""
						: positionservice.findPositionById(candidate.getPositionkey()).getPositionname());
			}
			for (CandidateInfo existing : candidateservice.findPossibleDuplicates(candidate)) {
				row.getDuplicates().add(existing.getCandidateid() + " " + join(existing.getFirstname(),
						existing.getLastname()) + (existing.getDuplicatereason() == null ? ""
								: " (" + existing.getDuplicatereason() + ")"));
			}
			earlier(row, emails, candidate.getEmail().toLowerCase(Locale.ROOT), "same e-mail");
			earlier(row, phones, candidate.getPhone().replaceAll("[^0-9]", ""), "same phone");
			earlier(row, names, name(join(candidate.getFirstname(), candidate.getLastname())), "same name");
		}
		return preview;
	}

	@Override
	public Outcome importCandidates(List<List<String>> cells, boolean includeduplicates, String userid) {
		ImportPreview preview = preview(cells);
		if (preview.getProblem() != null) {
			return Outcome.refused(preview.getProblem());
		}
		int added = 0;
		for (ImportRow row : preview.getRows()) {
			if (row.isReady() || (includeduplicates && row.isDuplicate())) {
				CandidateInfo candidate = row.getCandidate();
				String candidateid = candidateservice.addCandidate(candidate);
				activityservice.record(userid, "CANDIDATE_ADDED", "CANDIDATE", candidateid,
						row.isDuplicate() ? "import, possible duplicate" : "import");
				added++;
			}
		}
		int skipped = preview.getRows().size() - added;
		log.info("Candidate import by {}: {} added, {} skipped", userid, added, skipped);
		return Outcome.ok((added == 1 ? "1 candidate imported" : added + " candidates imported")
				+ (skipped == 0 ? "." : "; " + skipped + (skipped == 1 ? " line" : " lines") + " skipped."));
	}

	/** A possible duplicate within the file: the earlier line with the same value. */
	private static void earlier(ImportRow row, Map<String, Integer> seen, String value, String reason) {
		if (value.isEmpty()) {
			return;
		}
		Integer line = seen.putIfAbsent(value, row.getLine());
		if (line != null) {
			row.getDuplicates().add("line " + line + " (" + reason + ")");
		}
	}

	/** A source by its label ("Job board") or code (JOB_BOARD), any case, or null. */
	static CandidateSource source(String text) {
		for (CandidateSource source : CandidateSource.values()) {
			if (source.name().equalsIgnoreCase(text.trim().replace(' ', '_').replace('-', '_'))
					|| source.getLabel().equalsIgnoreCase(text.trim())) {
				return source;
			}
		}
		return null;
	}

	private static String cell(List<String> line, Map<String, Integer> columns, String column) {
		Integer index = columns.get(key(column));
		return index == null || index >= line.size() || line.get(index) == null ? "" : line.get(index).trim();
	}

	/** A header as a key: letters and digits only, lower case ("E-mail" and "email" are the same column). */
	static String key(String header) {
		return header == null ? "" : header.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
	}

	/** A name as compared: trimmed, single spaces, upper case (positions and languages are stored upper case). */
	private static String name(String text) {
		return text == null ? "" : text.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
	}

	private static String join(String first, String last) {
		return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
	}
}
