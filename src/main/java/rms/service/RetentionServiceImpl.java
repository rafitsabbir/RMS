package rms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.DocumentDao;
import rms.dao.RetentionDao;
import rms.model.RetentionInfo;

/** Data retention (F11). Logs carry candidate IDs and counts only. */
@Service
public class RetentionServiceImpl implements RetentionService {
	private static final Logger log = LoggerFactory.getLogger(RetentionServiceImpl.class);

	/** The environment variable with the period in months (1 to 240); unset, blank or anything else: off. */
	public static final String ENVIRONMENT_VARIABLE = "RMS_RETENTION_MONTHS";
	static final String REASON = "Retention period ended";

	private final int months;

	RetentionDao retentiondao;
	DocumentDao documentdao;
	DocumentService documentservice;
	ActivityService activityservice;

	public RetentionServiceImpl() {
		this(System.getenv(ENVIRONMENT_VARIABLE));
	}

	RetentionServiceImpl(String setting) {
		this.months = months(setting);
		if (months > 0) {
			log.info("Data retention: candidates not hired are due after {} months without activity", months);
		} else if (setting != null && !setting.isBlank()) {
			log.warn("Data retention is off: {} must be a number of months from 1 to 240", ENVIRONMENT_VARIABLE);
		}
	}

	static int months(String setting) {
		if (setting == null || setting.isBlank()) {
			return 0;
		}
		try {
			int value = Integer.parseInt(setting.trim());
			return value >= 1 && value <= 240 ? value : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Autowired
	public void setRetentionDao(RetentionDao retentiondao) {
		this.retentiondao = retentiondao;
	}

	@Autowired
	public void setDocumentDao(DocumentDao documentdao) {
		this.documentdao = documentdao;
	}

	@Autowired
	public void setDocumentService(DocumentService documentservice) {
		this.documentservice = documentservice;
	}

	@Autowired
	public void setActivityService(ActivityService activityservice) {
		this.activityservice = activityservice;
	}

	@Override
	public int getMonths() {
		return months;
	}

	@Override
	public List<RetentionInfo> getDue() {
		return months == 0 ? List.of() : retentiondao.getDue(cutoff());
	}

	/** Due when the last activity is before this day. */
	LocalDate cutoff() {
		return LocalDate.now().minusMonths(months);
	}

	@Override
	public Outcome cleanUp(List<String> candidateids, String userid) {
		if (months == 0) {
			return Outcome.refused("Data retention is off.");
		}
		if (candidateids == null || candidateids.isEmpty()) {
			return Outcome.refused("Please choose the candidates to clean up.");
		}
		// Only those still due now: the list may have changed since the page was shown
		Map<String, RetentionInfo> due = getDue().stream()
				.collect(Collectors.toMap(RetentionInfo::getCandidateid, info -> info));
		Set<String> chosen = Set.copyOf(candidateids);
		int done = 0;
		int skipped = 0;
		for (String candidateid : chosen) {
			if (!due.containsKey(candidateid)) {
				skipped++;
				continue;
			}
			if (!documentdao.getUnpurgedDocuments(candidateid).isEmpty()) {
				Outcome purged = documentservice.purge(candidateid, REASON, userid);
				if (!purged.done()) {
					log.warn("Retention: candidate {} skipped, their documents couldn't be deleted", candidateid);
					skipped++;
					continue;
				}
			}
			if (retentiondao.anonymize(candidateid, userid)) {
				activityservice.record(userid, "CANDIDATE_ANONYMIZED", "CANDIDATE", candidateid, months + (months == 1 ? " month" : " months"));
				done++;
			} else {
				skipped++;
			}
		}
		log.info("Retention clean-up by {}: {} candidates, {} skipped", userid, done, skipped);
		String message = (done == 1 ? "1 candidate's" : done + " candidates'") + " personal data removed.";
		return skipped == 0 ? Outcome.ok(message)
				: done == 0 ? Outcome.refused("Nothing was removed: the candidates are no longer due, or their documents "
						+ "couldn't be deleted (see the log).")
						: Outcome.ok(message + " " + skipped + " skipped: no longer due, or documents couldn't be deleted.");
	}
}
