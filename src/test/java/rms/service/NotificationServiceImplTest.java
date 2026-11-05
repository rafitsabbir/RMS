package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import rms.dao.CandidateDao;
import rms.dao.ScheduleDao;
import rms.dao.UserDao;
import rms.model.CandidateInfo;
import rms.model.ScheduleInfo;
import rms.model.UserInfo;

/** F1: who gets which e-mail, what it says, and that a mail problem never reaches the caller. */
class NotificationServiceImplTest {

	static final MailSettings ON = MailSettings.from(Map.of("RMS_MAIL_HOST", "smtp.example.test", "RMS_MAIL_FROM",
			"rms@example.test", "RMS_APP_URL", "https://rms.example.test/rms"));

	JavaMailSender sender = mock(JavaMailSender.class);
	UserDao userdao = mock(UserDao.class);
	CandidateDao candidatedao = mock(CandidateDao.class);
	ScheduleDao scheduledao = mock(ScheduleDao.class);
	List<UserInfo> users = new ArrayList<>();

	NotificationServiceImpl service;

	@BeforeEach
	void setUp() {
		when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
		when(userdao.getAllUsers()).thenReturn(users);
		// The test runs the background task at once
		service = wire(new NotificationServiceImpl(ON, sender, Runnable::run));
		user("U2", "INTERVIEWER", "ivan@example.test", 1);
		CandidateInfo candidate = new CandidateInfo();
		candidate.setCandidateid("C3");
		candidate.setFirstname("Nora");
		candidate.setLastname("Newcomer");
		when(candidatedao.findCandidateById("C3")).thenReturn(candidate);
	}

	@Test
	void anAssignedInterviewerGetsAPlainMailWithALink() throws Exception {
		service.interviewerAssigned("C3", "U2");

		MimeMessage mail = sent().get(0);
		assertThat(to(mail)).containsExactly("ivan@example.test");
		assertThat(mail.getFrom()[0].toString()).isEqualTo("rms@example.test");
		assertThat(mail.getSubject()).isEqualTo("RMS: you are assigned to Nora Newcomer (C3)");
		assertThat(text(mail)).contains("Hello Ivan U2,", "interviewer of Nora Newcomer (C3)",
				"https://rms.example.test/rms/myevaluations").doesNotContain("text/calendar");
	}

	@Test
	void aScheduledInterviewCarriesTheCalendarFile() throws Exception {
		when(scheduledao.findScheduleById(12)).thenReturn(interview("U2", ScheduleInfo.SCHEDULED));

		service.interviewScheduled(12);

		MimeMessage mail = sent().get(0);
		assertThat(mail.getSubject()).isEqualTo("RMS: new interview with Nora Newcomer (C3)");
		assertThat(text(mail)).contains("When: 2030-06-01 10:00 (one hour)", "Where: Room 2", "text/calendar",
				"method=PUBLISH", "interview-C3-2030-06-01.ics");
	}

	@Test
	void aChangedInterviewAlsoTellsThePreviousInterviewer() throws Exception {
		user("U5", "INTERVIEWER", "petra@example.test", 1);
		when(scheduledao.findScheduleById(12)).thenReturn(interview("U5", ScheduleInfo.SCHEDULED));

		service.interviewChanged(12, "U2");

		List<MimeMessage> mails = sent();
		assertThat(mails).hasSize(2);
		assertThat(to(mails.get(0))).containsExactly("petra@example.test");
		assertThat(mails.get(0).getSubject()).startsWith("RMS: interview changed");
		assertThat(to(mails.get(1))).containsExactly("ivan@example.test");
		assertThat(text(mails.get(1))).contains("now with another interviewer").doesNotContain("text/calendar");
	}

	@Test
	void anUnchangedInterviewerGetsOneMail() throws Exception {
		when(scheduledao.findScheduleById(12)).thenReturn(interview("U2", ScheduleInfo.SCHEDULED));

		service.interviewChanged(12, "U2");

		assertThat(sent()).hasSize(1);
	}

	@Test
	void aCancelledInterviewSendsTheCancellingCalendarFile() throws Exception {
		when(scheduledao.findScheduleById(12)).thenReturn(interview("U2", ScheduleInfo.CANCELLED));

		service.interviewCancelled(12);

		MimeMessage mail = sent().get(0);
		assertThat(mail.getSubject()).isEqualTo("RMS: interview cancelled with Nora Newcomer (C3)");
		assertThat(text(mail)).contains("Status: CANCELLED", "text/calendar");
	}

	@Test
	void hrHearsOfASavedEvaluationWithoutScoresOrComments() throws Exception {
		user("U3", "HR", "hanna@example.test", 1);
		user("U6", "HR", "inactive@example.test", 0);
		user("U1", "SUPER_ADMIN", "admin@example.test", 1);

		service.evaluationSaved("C3", "U2", null);

		MimeMessage mail = sent().get(0);
		assertThat(sent()).hasSize(1);
		assertThat(to(mail)).containsExactly("hanna@example.test");
		assertThat(text(mail)).contains("Ivan U2 (U2) saved an evaluation of Nora Newcomer (C3)",
				"https://rms.example.test/rms/adminviewmarks");
	}

	@Test
	void withoutAnHrUserTheSuperAdminsHear() throws Exception {
		user("U1", "SUPER_ADMIN", "admin@example.test", 1);
		user("U7", "HR", null, 1);

		service.evaluationSaved("C3", "U2", null);

		assertThat(to(sent().get(0))).containsExactly("admin@example.test");
	}

	@Test
	void inactiveUsersAndBadAddressesGetNothing() {
		user("U8", "INTERVIEWER", "ivan@example.test", 0);
		user("U9", "INTERVIEWER", "not an address", 1);

		service.interviewerAssigned("C3", "U8");
		service.interviewerAssigned("C3", "U9");
		service.interviewerAssigned("C3", null);

		verify(sender, never()).send(any(MimeMessage.class));
	}

	@Test
	void aMailServerFailureNeverReachesTheCaller() {
		doThrow(new MailSendException("refused by smtp.example.test")).when(sender).send(any(MimeMessage.class));
		when(userdao.findUserById("U2")).thenThrow(new IllegalStateException("database gone"));

		assertThatCode(() -> service.interviewerAssigned("C3", "U2")).doesNotThrowAnyException();
		assertThatCode(() -> service.interviewScheduled(12)).doesNotThrowAnyException();
	}

	@Test
	void withoutSettingsNothingIsReadOrSent() {
		NotificationServiceImpl off = wire(new NotificationServiceImpl(MailSettings.OFF));

		off.interviewerAssigned("C3", "U2");
		off.interviewScheduled(12);
		off.evaluationSaved("C3", "U2", null);

		verifyNoInteractions(userdao, scheduledao, candidatedao);
		off.destroy();
	}

	@Test
	void withoutAnAppUrlTheMailHasNoLink() throws Exception {
		NotificationServiceImpl nolink = wire(new NotificationServiceImpl(
				MailSettings.from(Map.of("RMS_MAIL_HOST", "h", "RMS_MAIL_FROM", "rms@example.test")), sender,
				Runnable::run));

		nolink.interviewerAssigned("C3", "U2");

		assertThat(text(sent().get(0))).doesNotContain("Open RMS");
	}

	private NotificationServiceImpl wire(NotificationServiceImpl notifications) {
		notifications.setUserDao(userdao);
		notifications.setCandidateDao(candidatedao);
		notifications.setScheduleDao(scheduledao);
		return notifications;
	}

	private void user(String userid, String role, String email, int active) {
		UserInfo user = new UserInfo();
		user.setUserid(userid);
		user.setRole(role);
		user.setEmail(email);
		user.setIsactive(active);
		user.setFirstname(role.equals("INTERVIEWER") ? "Ivan" : null);
		user.setLastname(userid);
		users.add(user);
		when(userdao.findUserById(userid)).thenReturn(user);
	}

	private static ScheduleInfo interview(String interviewerid, String status) {
		ScheduleInfo schedule = new ScheduleInfo();
		schedule.setSchedulekey(12);
		schedule.setCandidateid("C3");
		schedule.setCandidatename("Nora Newcomer");
		schedule.setInterviewerid(interviewerid);
		schedule.setStartat(LocalDateTime.of(2030, 6, 1, 10, 0));
		schedule.setLocation("Room 2");
		schedule.setStatus(status);
		return schedule;
	}

	private List<MimeMessage> sent() {
		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(sender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
		return captor.getAllValues();
	}

	private static List<String> to(MimeMessage mail) throws Exception {
		Address[] to = mail.getRecipients(Message.RecipientType.TO);
		return Arrays.stream(to).map(Address::toString).toList();
	}

	/** The whole message as sent, headers and parts (the text parts are plain 7bit or quoted-printable). */
	private static String text(MimeMessage mail) throws Exception {
		mail.saveChanges();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mail.writeTo(out);
		return out.toString(StandardCharsets.UTF_8).replace("=\r\n", "");
	}
}
