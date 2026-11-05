package rms.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import rms.dao.CandidateDao;
import rms.dao.ScheduleDao;
import rms.dao.UserDao;
import rms.model.CandidateInfo;
import rms.model.InterviewRound;
import rms.model.Role;
import rms.model.ScheduleInfo;
import rms.model.UserInfo;

/**
 * Staff e-mails (F1). One background thread sends them in order, so the user's request never waits on the mail server
 * and a mail failure never undoes the action. Logs carry keys and counts only, never addresses or names.
 */
@Service
public class NotificationServiceImpl implements NotificationService, DisposableBean {
	private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

	static final String SUBJECT_PREFIX = "RMS: ";

	private final MailSettings settings;
	private final JavaMailSender sender;
	private final Executor executor;

	UserDao userdao;
	CandidateDao candidatedao;
	ScheduleDao scheduledao;

	public NotificationServiceImpl() {
		this(MailSettings.fromEnvironment());
	}

	NotificationServiceImpl(MailSettings settings) {
		this(settings, settings.enabled() ? mailSender(settings) : null, settings.enabled() ? backgroundThread() : null);
	}

	/** For tests: a mail sender and an executor that runs the task at once. */
	NotificationServiceImpl(MailSettings settings, JavaMailSender sender, Executor executor) {
		this.settings = settings;
		this.sender = sender;
		this.executor = executor;
		if (!settings.configured()) {
			log.info("E-mail notifications are off (RMS_MAIL_HOST is not set)");
		} else if (settings.problem() != null) {
			log.warn("E-mail notifications are off: {}", settings.problem());
		} else {
			log.info("E-mail notifications are on");
		}
	}

	@Autowired
	public void setUserDao(UserDao userdao) {
		this.userdao = userdao;
	}

	@Autowired
	public void setCandidateDao(CandidateDao candidatedao) {
		this.candidatedao = candidatedao;
	}

	@Autowired
	public void setScheduleDao(ScheduleDao scheduledao) {
		this.scheduledao = scheduledao;
	}

	@Override
	public void interviewerAssigned(String candidateid, String interviewerid) {
		submit("ASSIGNED", candidateid, () -> {
			UserInfo interviewer = recipient(interviewerid);
			if (interviewer == null) {
				return;
			}
			String candidate = candidateLabel(candidateid);
			send(interviewer, "you are assigned to " + candidate,
					greeting(interviewer)
							+ "You are now an interviewer of " + candidate + ".\n"
							+ "Please evaluate the candidate under My Evaluations when you have interviewed them."
							+ link("/myevaluations"),
					null);
		});
	}

	@Override
	public void interviewScheduled(int schedulekey) {
		submit("INTERVIEW_SCHEDULED", "interview " + schedulekey, () -> sendInterview(schedulekey, "New interview",
				"An interview is scheduled for you."));
	}

	@Override
	public void interviewChanged(int schedulekey, String previousinterviewerid) {
		submit("INTERVIEW_CHANGED", "interview " + schedulekey, () -> {
			ScheduleInfo schedule = sendInterview(schedulekey, "Interview changed",
					"An interview of yours was changed. The details below are the new ones.");
			if (schedule != null && previousinterviewerid != null
					&& !previousinterviewerid.equals(schedule.getInterviewerid())) {
				UserInfo previous = recipient(previousinterviewerid);
				if (previous != null) {
					send(previous, "interview moved to another interviewer",
							greeting(previous) + "The interview with " + candidateLabel(schedule) + " at "
									+ schedule.getStartlabel() + " is now with another interviewer. "
									+ "Please remove it from your calendar." + link("/myschedule"),
							null);
				}
			}
		});
	}

	@Override
	public void interviewCancelled(int schedulekey) {
		submit("INTERVIEW_CANCELLED", "interview " + schedulekey, () -> sendInterview(schedulekey, "Interview cancelled",
				"An interview of yours was cancelled. The attached calendar file removes it from your calendar."));
	}

	@Override
	public void evaluationSaved(String candidateid, String interviewerid, String interviewround) {
		submit("EVALUATION_SAVED", candidateid, () -> {
			List<UserInfo> hr = hrRecipients();
			if (hr.isEmpty()) {
				log.info("Notification EVALUATION_SAVED for {}: no active HR user with an e-mail address", candidateid);
				return;
			}
			UserInfo interviewer = userdao.findUserById(interviewerid);
			String by = interviewer == null ? interviewerid : name(interviewer) + " (" + interviewerid + ")";
			String candidate = candidateLabel(candidateid);
			for (UserInfo user : hr) {
				send(user, "evaluation of " + candidate,
						greeting(user) + by + " saved an evaluation of " + candidate
								+ (InterviewRound.parse(interviewround) == null ? ""
										: " (" + InterviewRound.label(interviewround) + ")")
								+ ".\n"
								+ "The averages are under Candidate Status." + link("/adminviewmarks"),
						null);
			}
		});
	}

	/** The interview's details and calendar file to its interviewer; returns the interview, or null when it's gone. */
	private ScheduleInfo sendInterview(int schedulekey, String subject, String intro) {
		ScheduleInfo schedule = scheduledao.findScheduleById(schedulekey);
		if (schedule == null || schedule.getStartat() == null) {
			log.info("Notification for interview {}: the interview is gone", schedulekey);
			return null;
		}
		UserInfo interviewer = recipient(schedule.getInterviewerid());
		if (interviewer != null) {
			String location = schedule.getLocation() == null ? "not given" : schedule.getLocation();
			send(interviewer, subject.toLowerCase() + " with " + candidateLabel(schedule),
					greeting(interviewer) + intro + "\n\n"
							+ "Candidate: " + candidateLabel(schedule) + "\n"
							+ (schedule.getInterviewround() == null ? "" : "Round: " + schedule.getRoundlabel() + "\n")
							+ "When: " + schedule.getStartlabel() + " (one hour)\n"
							+ "Where: " + location + "\n"
							+ "Status: " + schedule.getStatus() + "\n\n"
							+ "Open the attached file to add or update the interview in your calendar."
							+ link("/myschedule"),
					schedule);
		}
		return schedule;
	}

	private void submit(String event, String key, Runnable task) {
		if (sender == null || executor == null) {
			return;
		}
		try {
			executor.execute(() -> {
				try {
					task.run();
				} catch (RuntimeException e) {
					// Keys only: the exception message may hold an address or the server's name
					log.warn("Notification {} for {} failed: {}", event, key, e.getClass().getName());
				}
			});
		} catch (RejectedExecutionException e) {
			log.warn("Notification {} for {} not sent: the mail thread has stopped", event, key);
		}
	}

	private void send(UserInfo to, String subject, String body, ScheduleInfo invite) {
		try {
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, invite != null, "UTF-8");
			helper.setFrom(settings.from());
			helper.setTo(to.getEmail().trim());
			helper.setSubject(SUBJECT_PREFIX + subject);
			helper.setText(body + "\n\n-- \nThis message was sent by RMS. Please don't reply to it.\n");
			if (invite != null) {
				helper.addAttachment(CalendarInvite.fileName(invite),
						new ByteArrayResource(CalendarInvite.of(invite, LocalDateTime.now(ZoneOffset.UTC))),
						"text/calendar; charset=UTF-8; method=PUBLISH");
			}
			sender.send(message);
			log.info("Notification sent to {}", to.getUserid());
		} catch (Exception e) {
			log.warn("Notification to {} failed: {}", to.getUserid(), e.getClass().getName());
		}
	}

	/** The active user with an e-mail address, or null. */
	private UserInfo recipient(String userid) {
		if (userid == null) {
			return null;
		}
		UserInfo user = userdao.findUserById(userid);
		if (user == null || user.getIsactive() != 1 || !hasAddress(user)) {
			log.info("No notification to {}: inactive or no e-mail address", userid);
			return null;
		}
		return user;
	}

	/** Active HR users with an e-mail address; Super Admins when there is no such HR user. */
	List<UserInfo> hrRecipients() {
		List<UserInfo> hr = new ArrayList<>();
		List<UserInfo> admins = new ArrayList<>();
		for (UserInfo user : userdao.getAllUsers()) {
			if (user.getIsactive() != 1 || !hasAddress(user)) {
				continue;
			}
			Role role = Role.of(user.getRole(), user.getIsinterviewer());
			if (role == Role.HR) {
				hr.add(user);
			} else if (role == Role.SUPER_ADMIN) {
				admins.add(user);
			}
		}
		return hr.isEmpty() ? admins : hr;
	}

	private static boolean hasAddress(UserInfo user) {
		String email = user.getEmail();
		return email != null && email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
	}

	private String candidateLabel(String candidateid) {
		CandidateInfo candidate = candidatedao.findCandidateById(candidateid);
		if (candidate == null) {
			return candidateid;
		}
		String name = join(candidate.getFirstname(), candidate.getLastname());
		return name.isEmpty() ? candidateid : name + " (" + candidateid + ")";
	}

	private static String candidateLabel(ScheduleInfo schedule) {
		String name = schedule.getCandidatename();
		return name == null || name.isBlank() ? schedule.getCandidateid()
				: name.trim() + " (" + schedule.getCandidateid() + ")";
	}

	private static String greeting(UserInfo user) {
		String name = name(user);
		return (name.isEmpty() ? "Hello" : "Hello " + name) + ",\n\n";
	}

	private static String name(UserInfo user) {
		return join(user.getFirstname(), user.getLastname());
	}

	private static String join(String first, String last) {
		return ((first == null ? "" : first.trim()) + " " + (last == null ? "" : last.trim())).trim();
	}

	private String link(String path) {
		return settings.appurl() == null ? "" : "\n\nOpen RMS: " + settings.appurl() + path;
	}

	private static JavaMailSender mailSender(MailSettings settings) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(settings.host());
		sender.setPort(settings.port());
		sender.setUsername(settings.user());
		sender.setPassword(settings.password());
		sender.setDefaultEncoding("UTF-8");
		Properties properties = sender.getJavaMailProperties();
		properties.put("mail.smtp.auth", String.valueOf(settings.user() != null));
		// A slow or silent server must not hold the mail thread for long
		properties.put("mail.smtp.connectiontimeout", "10000");
		properties.put("mail.smtp.timeout", "20000");
		properties.put("mail.smtp.writetimeout", "20000");
		if (MailSettings.STARTTLS.equals(settings.security())) {
			properties.put("mail.smtp.starttls.enable", "true");
			properties.put("mail.smtp.starttls.required", "true");
			properties.put("mail.smtp.ssl.checkserveridentity", "true");
		} else if (MailSettings.SSL.equals(settings.security())) {
			properties.put("mail.smtp.ssl.enable", "true");
			properties.put("mail.smtp.ssl.checkserveridentity", "true");
		}
		return sender;
	}

	private static ExecutorService backgroundThread() {
		return Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "rms-mail");
			thread.setDaemon(true);
			return thread;
		});
	}

	@Override
	public void destroy() {
		if (executor instanceof ExecutorService service) {
			service.shutdown();
		}
	}
}
