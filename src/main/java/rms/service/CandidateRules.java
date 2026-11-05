package rms.service;

import java.time.LocalDate;
import java.util.regex.Pattern;

import rms.model.CandidateInfo;
import rms.model.CandidateSource;
import rms.model.JobInfo;

/**
 * The checks of a candidate's fields, shared by the Add/Edit Candidate form (CandidateController) and the CSV import
 * (CandidateImportServiceImpl, F4), so both refuse exactly the same things. Built from the services the caller
 * already has; holds no state of its own.
 */
public class CandidateRules {

	public static final int MAX_EMAIL = 150;
	public static final int MAX_PHONE = 30;
	/** Something@something.something, no spaces: a typing check, not a proof the address works. */
	private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
	private static final Pattern PHONE = Pattern.compile("[0-9+()./ -]{3,30}");

	private final PositionService positionservice;
	private final LanguageService languageservice;
	private final JobService jobservice;

	public CandidateRules(PositionService positionservice, LanguageService languageservice, JobService jobservice) {
		this.positionservice = positionservice;
		this.languageservice = languageservice;
		this.jobservice = jobservice;
	}

	/**
	 * Why the candidate can't be saved, or null. Expects trimmed text fields (never null). stored is the candidate as
	 * saved, when editing; badjob and baddate say the form's job or date couldn't be read. With a job, the job's
	 * position is set on the candidate.
	 */
	public String check(CandidateInfo candidateinfo, CandidateInfo stored, boolean badjob, boolean baddate) {
		if (candidateinfo.getFirstname().isEmpty()) {
			return "Please enter the first name.";
		}
		if (candidateinfo.getLastname().isEmpty()) {
			return "Please enter the last name.";
		}
		if (!EnglishText.isEnglish(candidateinfo.getFirstname()) || !EnglishText.isEnglish(candidateinfo.getLastname())) {
			return "Names can use English letters only. " + EnglishText.PROBLEM;
		}
		String email = candidateinfo.getEmail();
		if (!email.isEmpty() && (email.length() > MAX_EMAIL || !EMAIL.matcher(email).matches())) {
			return "Please enter a valid e-mail address, or leave it empty.";
		}
		String phone = candidateinfo.getPhone();
		if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) {
			return "Please enter a valid phone number (digits, spaces and + ( ) - . /), or leave it empty.";
		}
		String jobProblem = checkJob(candidateinfo, stored, badjob);
		if (jobProblem != null) {
			return jobProblem;
		}
		// An active position, or the one the candidate already has (it may have been deleted since). With a job,
		// checkJob has set the job's position
		int positionkey = candidateinfo.getPositionkey();
		if (candidateinfo.getJobkey() <= 0 && (positionkey <= 0 || (positionservice.findPositionById(positionkey) == null
				&& (stored == null || stored.getPositionkey() != positionkey)))) {
			return "Please choose a position.";
		}
		int languagekey = candidateinfo.getLanguagekey();
		if (languagekey <= 0 || (languageservice.findLanguageById(languagekey) == null
				&& (stored == null || stored.getLanguagekey() != languagekey))) {
			return "Please choose a language.";
		}
		// A known source, or the one the candidate already has
		String source = candidateinfo.getSource();
		if (!source.isEmpty() && CandidateSource.parse(source) == null
				&& (stored == null || !source.equals(stored.getSource()))) {
			return "Please choose where the candidate came from, or leave it empty.";
		}
		if (baddate) {
			return "Please enter the applied date as a date, or leave it empty.";
		}
		LocalDate applied = candidateinfo.getApplieddate();
		if (applied != null && applied.isAfter(LocalDate.now())) {
			return "The applied date can't be in the future.";
		}
		return null;
	}

	/** An open job, or the one the candidate already has, or none; sets the job's position. */
	private String checkJob(CandidateInfo candidateinfo, CandidateInfo stored, boolean badjob) {
		if (badjob) {
			return "Please choose a job, or none.";
		}
		int jobkey = candidateinfo.getJobkey();
		if (jobkey <= 0) {
			candidateinfo.setJobkey(0);
			return null;
		}
		boolean kept = stored != null && stored.getJobkey() == jobkey;
		JobInfo job = jobservice.findJobById(jobkey);
		if (job == null) {
			if (!kept) {
				return "Please choose an open job, or none.";
			}
			candidateinfo.setPositionkey(stored.getPositionkey());
			return null;
		}
		if (!job.isOpen() && !kept) {
			return "That job is closed. Please choose an open job, or none.";
		}
		candidateinfo.setPositionkey(job.getPositionkey());
		return null;
	}
}
