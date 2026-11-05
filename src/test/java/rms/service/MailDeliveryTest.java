package rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import rms.dao.CandidateDao;
import rms.dao.ScheduleDao;
import rms.dao.UserDao;
import rms.model.ScheduleInfo;
import rms.model.UserInfo;

/**
 * F1 end to end: a real SMTP server (Mailpit in a throwaway container) receives the interview mail that the production
 * code path sends from its background thread, with the calendar file. Needs Docker, like the DAO tests; skipped
 * without it unless RMS_REQUIRE_DOCKER=true.
 */
class MailDeliveryTest {

	private static GenericContainer<?> mailpit;
	private static boolean docker;

	@BeforeAll
	@SuppressWarnings("resource")
	static void startMailServer() {
		docker = DockerClientFactory.instance().isDockerAvailable();
		if ("true".equals(System.getenv("RMS_REQUIRE_DOCKER"))) {
			assertTrue(docker, "Docker is required (RMS_REQUIRE_DOCKER=true) but not available");
		}
		if (docker) {
			mailpit = new GenericContainer<>("axllent/mailpit:v1.27").withExposedPorts(1025, 8025)
					.waitingFor(Wait.forHttp("/readyz").forPort(8025));
			mailpit.start();
		}
	}

	@AfterAll
	static void stopMailServer() {
		if (mailpit != null) {
			mailpit.stop();
		}
	}

	@Test
	void anInterviewMailWithItsCalendarFileReachesTheServer() throws Exception {
		assumeTrue(docker, "Docker not available: mail delivery test skipped");
		MailSettings settings = MailSettings.from(Map.of("RMS_MAIL_HOST", mailpit.getHost(), "RMS_MAIL_PORT",
				String.valueOf(mailpit.getMappedPort(1025)), "RMS_MAIL_FROM", "rms@example.test", "RMS_MAIL_SECURITY",
				"none", "RMS_APP_URL", "https://rms.example.test/rms"));
		// The production constructor: the real JavaMailSender and the background thread
		NotificationServiceImpl service = new NotificationServiceImpl(settings);
		UserDao userdao = mock(UserDao.class);
		ScheduleDao scheduledao = mock(ScheduleDao.class);
		service.setUserDao(userdao);
		service.setCandidateDao(mock(CandidateDao.class));
		service.setScheduleDao(scheduledao);
		UserInfo interviewer = new UserInfo();
		interviewer.setUserid("U2");
		interviewer.setFirstname("Ivan");
		interviewer.setEmail("ivan@example.test");
		interviewer.setIsactive(1);
		when(userdao.findUserById("U2")).thenReturn(interviewer);
		ScheduleInfo schedule = new ScheduleInfo();
		schedule.setSchedulekey(12);
		schedule.setCandidateid("C3");
		schedule.setCandidatename("Nora Newcomer");
		schedule.setInterviewerid("U2");
		schedule.setStartat(LocalDateTime.of(2030, 6, 1, 10, 0));
		schedule.setLocation("Room 2");
		when(scheduledao.findScheduleById(12)).thenReturn(schedule);

		service.interviewScheduled(12);

		String raw = latestMessage();
		service.destroy();
		assertThat(raw).contains("To: ivan@example.test", "From: rms@example.test",
				"Subject: RMS: new interview with Nora Newcomer (C3)", "text/calendar",
				"interview-C3-2030-06-01.ics");
		// The text part, possibly folded by quoted-printable
		assertThat(raw.replace("=\r\n", "")).contains("When: 2030-06-01 10:00 (one hour)");
	}

	/** The newest message's raw source, waiting up to 15 seconds for the background thread to deliver it. */
	private static String latestMessage() throws Exception {
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		URI uri = URI.create("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025)
				+ "/api/v1/message/latest/raw");
		long deadline = System.currentTimeMillis() + 15_000;
		while (System.currentTimeMillis() < deadline) {
			HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri).GET().build(),
					HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				return response.body();
			}
			Thread.sleep(200);
		}
		throw new AssertionError("no message arrived within 15 seconds");
	}
}
