package rms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;

/**
 * The whole recruitment workflow in Chromium, across the four roles, against a deployed trial with a FRESH
 * synthetic seed (db/test-seed.sql), as the CI container job starts it (deploy/docker-compose.yml).
 * It adds, decides, deletes and purges a candidate, so it is enabled only when RMS_BASE_URL is set AND
 * RMS_WORKFLOW_SEEDED=true. Never set that against a real database. Before any write it also fails unless
 * RMS_BASE_URL is on this machine and the seed is untouched (the candidates are exactly C1 and C2 and the
 * activity log has no candidate added), so the candidate it adds is C3 and nothing else is changed.
 * The steps depend on each other and run in order; after a failure the rest are skipped. Screenshots of the
 * failing step go to target/workflow/.
 * Browser: PLAYWRIGHT_CHROMIUM_EXECUTABLE when set (a local Chromium or Chrome), else Playwright's own Chromium.
 * Without access to cdn.jsdelivr.net, RMS_CDN_MIRROR names a folder laid out like its npm paths
 * (bootstrap@5.3.8/dist/css/bootstrap.min.css, …): the pages' library files are then served from there.
 */
@EnabledIfEnvironmentVariable(named = "RMS_BASE_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RMS_WORKFLOW_SEEDED", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowTest {

	/** Seed users (db/test-seed.sql): username and password. */
	private static final Map<String, String[]> USERS = Map.of(
			"SA", new String[] { "test.admin", "test-only-1" },
			"HR", new String[] { "test.hr", "test-only-3" },
			"HM", new String[] { "test.manager", "test-only-4" },
			"IV", new String[] { "test.interviewer", "test-only-2" });

	private static final Pattern CDN = Pattern.compile("^https://cdn\\.jsdelivr\\.net/npm/.+");

	private static final Path OUT = Paths.get("target", "workflow");

	private final String base = baseUrl();
	private final Map<String, Page> pages = new LinkedHashMap<>();
	private final Map<Page, List<String>> errors = new LinkedHashMap<>();
	private Playwright playwright;
	private Browser browser;
	private Path cv;
	private String documentPath;
	private boolean broken;

	@RegisterExtension
	final TestWatcher screenshots = new TestWatcher() {
		@Override
		public void testFailed(ExtensionContext context, Throwable cause) {
			broken = true;
			pages.forEach((who, page) -> {
				try {
					page.screenshot(new Page.ScreenshotOptions().setFullPage(true)
							.setPath(OUT.resolve(context.getRequiredTestMethod().getName() + "-" + who + ".png")));
				} catch (PlaywrightException e) {
					// A closed or crashed page: the other roles still get theirs
				}
			});
		}
	};

	private static String baseUrl() {
		String url = System.getenv("RMS_BASE_URL");
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	@BeforeAll
	void start() throws IOException {
		Files.createDirectories(OUT);
		cv = OUT.resolve("cv.pdf");
		Files.write(cv, "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII));
		BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
		String executable = System.getenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE");
		if (executable != null && !executable.isBlank()) {
			options.setExecutablePath(Paths.get(executable));
			// The given browser is used: don't download Playwright's own
			playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
		} else {
			playwright = Playwright.create();
		}
		browser = playwright.chromium().launch(options);

		// Guards before any write: a local trial, and an untouched seed
		String host = URI.create(base).getHost();
		if (!List.of("127.0.0.1", "localhost", "[::1]").contains(host)) {
			fail("WorkflowTest writes data: RMS_BASE_URL must be a local trial, not " + host);
		}
		Page sa = signIn("SA");
		APIResponse candidates = sa.context().request().get(base + "/exportcandidates");
		assertThat(candidates.status()).isEqualTo(200);
		List<String> ids = candidates.text().lines().skip(1).map(row -> row.split(",", 2)[0]).toList();
		// A deleted candidate leaves the list but not the log: the seed's log has no candidate added
		long added = activity(sa, "CANDIDATE").stream().filter(row -> row.contains(",Candidate added,")).count();
		if (!ids.equals(List.of("C1", "C2")) || added > 0) {
			fail("WorkflowTest needs a fresh db/test-seed.sql (candidates C1 and C2 only, none added since), found "
					+ ids + " and " + added + " added");
		}
	}

	@AfterAll
	void stop() {
		if (playwright != null) {
			playwright.close();
		}
	}

	@BeforeEach
	void notBroken() {
		assumeFalse(broken, "an earlier step failed");
	}

	@Test
	@Order(1)
	void hrAddsACandidate() {
		Page hr = signIn("HR");
		hr.navigate(base + "/createcandidate");
		hr.fill("#firstname", "Zoë");
		hr.fill("#lastname", "Newcomer");
		hr.selectOption("#jobkey", "1");
		hr.selectOption("#languagekey", "1");
		submit(hr, "form#candidate button[type=submit]");
		// Only English text is accepted (EnglishText)
		assertThat(path(hr)).matches("/(createcandidate|savecandidate).*");
		assertThat(text(hr)).containsIgnoringCase("English");

		hr.navigate(base + "/createcandidate");
		hr.fill("#firstname", "Nora");
		hr.fill("#lastname", "Newcomer");
		hr.fill("#email", "nora@example.test");
		hr.fill("#phone", "000-2001");
		hr.selectOption("#source", "REFERRAL");
		hr.selectOption("#jobkey", "1");
		hr.selectOption("#languagekey", "1");
		hr.fill("#applieddate", "2026-10-15");
		submit(hr, "form#candidate button[type=submit]");
		assertThat(path(hr)).isEqualTo("/viewcandidatelist");
		Locator row = hr.locator("#candidatetable tbody tr", new Page.LocatorOptions().setHasText("Newcomer"));
		assertThat(row.count()).isEqualTo(1);
		assertThat(row.locator("td").first().innerText().trim()).isEqualTo("C3");
	}

	@Test
	@Order(2)
	void hrUploadsTheCvAndAssignsTheInterviewer() {
		Page hr = pages.get("HR");
		hr.navigate(base + "/viewcandidate?candidateid=C3");
		assertThat(text(hr)).contains("No CV");
		hr.selectOption("#doctype", "CV");
		hr.setInputFiles("#file", cv);
		submit(hr, "form#uploaddocument button[type=submit]");
		assertThat(text(hr)).contains("CV ✓").contains("cv.pdf");

		hr.selectOption("#interviewerid", "U2");
		submit(hr, "form#assigninterviewer button[type=submit]");
		// The 2026-10-20 workflow run found HTTP 500 here (a lost @Autowired, see WiringTest)
		assertThat(text(hr, "#assignmenttable")).contains("Ivan Interviewer");
	}

	@Test
	@Order(3)
	void hrSchedulesTheInterview() {
		Page hr = pages.get("HR");
		schedule(hr, "2020-01-01T10:00", "Room 2");
		assertThat(path(hr)).doesNotEndWith("/viewschedulelist");
		assertThat(text(hr)).containsIgnoringCase("hasn't passed");

		String next = LocalDate.now().plusYears(1) + "T10:00";
		schedule(hr, next, "Room 2");
		assertThat(path(hr)).isEqualTo("/viewschedulelist");
		assertThat(text(hr, "#scheduletable")).contains("Room 2");

		// The same interviewer at the same minute again
		schedule(hr, next, "Room 3");
		assertThat(path(hr)).doesNotEndWith("/viewschedulelist");
	}

	@Test
	@Order(4)
	void theInterviewerEvaluates() throws IOException {
		Page iv = signIn("IV");
		iv.navigate(base + "/myschedule");
		assertThat(text(iv)).contains("Nora Newcomer").contains("Room 2");
		iv.navigate(base + "/myevaluations");
		assertThat(text(iv)).contains("Nora Newcomer");

		iv.navigate(base + "/viewcandidate?candidateid=C3");
		Locator link = iv.locator("#documenttable a[href*=\"downloaddocument\"]").first();
		documentPath = ((String) link.evaluate("a => a.href")).replace(base, "");
		Download download = iv.waitForDownload(link::click);
		assertThat(Files.readAllBytes(download.path())).isEqualTo(Files.readAllBytes(cv));

		iv.navigate(base + "/evaluate?candidateid=C3");
		Map<String, String> scores = new LinkedHashMap<>();
		scores.put("workexp", "8");
		scores.put("techknowledge", "9");
		scores.put("leadership", "7");
		scores.put("decision", "8");
		scores.put("probsolving", "9");
		scores.put("stress", "7");
		scores.put("education", "8");
		scores.put("comskill", "9");
		scores.put("attitude", "9");
		scores.put("personality", "8");
		scores.forEach((field, score) -> iv.selectOption("#" + field, score));
		iv.fill("#comments", "Clear thinker; strong Java.");
		submit(iv, "form#evaluation button[type=submit]");
		assertThat(path(iv)).startsWith("/myevaluations");
	}

	@Test
	@Order(5)
	void theHiringManagerReadsButCannotDecide() {
		Page hm = signIn("HM");
		hm.navigate(base + "/adminviewmarks");
		// 82 = the sum of the ten scores (each out of 10)
		assertThat(hm.locator("#markstable tbody tr", new Page.LocatorOptions().setHasText("Newcomer")).innerText())
				.containsPattern("\\b82\\b");
		hm.navigate(base + "/viewevaluations?candidateid=C3");
		assertThat(text(hm)).contains("Clear thinker");
		assertThat(hm.locator("form#savedecision").count()).isZero();

		// With a valid CSRF token, so the 403 comes from the role rule, not from the CSRF filter
		String token = hm.locator("form#logout input[name=_csrf]").inputValue();
		APIResponse post = hm.context().request().post(base + "/savedecision", RequestOptions.create()
				.setForm(FormData.create().set("_csrf", token).set("candidateid", "C3").set("status", "S")).setMaxRedirects(0));
		assertThat(post.status()).isEqualTo(403);
	}

	@Test
	@Order(6)
	void hrSelectsAndTheEvaluationLocks() {
		Page hr = pages.get("HR");
		hr.navigate(base + "/viewevaluations?candidateid=C3");
		hr.selectOption("#status", "S");
		hr.fill("#decisiondate", "2026-10-20");
		hr.fill("#reason", "Best technical interview this round");
		submit(hr, "form#savedecision button[type=submit]");
		assertThat(text(hr)).contains("Selected").contains("Best technical interview");

		Page iv = pages.get("IV");
		iv.navigate(base + "/evaluate?candidateid=C3");
		assertThat(iv.locator("form#evaluation button[type=submit]").count()).isZero();
		assertThat(text(iv)).contains("the evaluations can't be changed");
	}

	@Test
	@Order(7)
	void reportsAndTheActivityLog() {
		Page sa = pages.get("SA");
		APIResponse csv = sa.context().request().get(base + "/exportresults");
		assertThat(csv.status()).isEqualTo(200);
		assertThat(csv.text()).contains("Nora").contains("Selected");

		// The CSV holds the whole log; the page shows only its first 10 rows
		assertThat(activity(sa, "C3")).anyMatch(row -> row.contains(",Marked,")).anyMatch(row -> row.contains(",Selected,"));
	}

	@Test
	@Order(8)
	void deleteAndPurge() {
		Page hr = pages.get("HR");
		hr.navigate(base + "/viewcandidatelist");
		Locator row = hr.locator("#candidatetable tr", new Page.LocatorOptions().setHasText("Nora"));
		submit(hr, row.locator("form[id^=delete] button"));
		assertThat(text(hr, "#candidatetable")).doesNotContain("Nora");

		Page sa = pages.get("SA");
		sa.navigate(base + "/viewdeletedcandidates");
		assertThat(text(sa)).contains("Nora");
		sa.fill("form#purge-C3 input[name=reason]", "Retention period ended");
		submit(sa, sa.locator("form#purge-C3 button[type=submit]"));
		APIResponse download = sa.context().request().get(base + documentPath, RequestOptions.create().setMaxRedirects(0));
		assertThat(download.status()).isEqualTo(404);

		// The reason is kept on the document record (purgereason), the log has the count
		assertThat(activity(sa, "C3")).anyMatch(line -> line.contains(",Documents purged,") && line.contains("1 file"));
	}

	@Test
	@Order(9)
	void noConsoleErrors() {
		errors.forEach((page, list) -> assertThat(list).as("console errors").isEmpty());
	}

	/** A new browser context signed in as the seed user of that role. */
	private Page signIn(String who) {
		BrowserContext context = browser.newContext(new Browser.NewContextOptions()
				.setViewportSize(1366, 900).setAcceptDownloads(true));
		String mirror = System.getenv("RMS_CDN_MIRROR");
		if (mirror != null && !mirror.isBlank()) {
			context.route(CDN, route -> {
				Path file = Paths.get(mirror).resolve(route.request().url().substring("https://cdn.jsdelivr.net/npm/".length()));
				if (Files.isRegularFile(file)) {
					route.fulfill(new Route.FulfillOptions().setPath(file).setHeaders(Map.of("access-control-allow-origin", "*")));
				} else {
					route.abort();
				}
			});
		}
		Page page = context.newPage();
		List<String> list = new ArrayList<>();
		errors.put(page, list);
		page.onDialog(dialog -> dialog.accept());
		page.onConsoleMessage(message -> {
			if ("error".equals(message.type()) && !message.text().contains("Failed to load resource")) {
				list.add(message.text());
			}
		});
		page.onPageError(error -> list.add(error));
		pages.put(who, page);
		page.navigate(base + "/login");
		page.fill("#username", USERS.get(who)[0]);
		page.fill("#password", USERS.get(who)[1]);
		submit(page, "#Login button[type=submit]");
		assertThat(path(page)).as("signed in as " + USERS.get(who)[0]).isEqualTo("/home");
		return page;
	}

	/** The rows of the activity log's CSV (Activity Log, Download CSV) with that field, e.g. a record ID. */
	private List<String> activity(Page sa, String record) {
		APIResponse csv = sa.context().request().get(base + "/exportactivity");
		assertThat(csv.status()).isEqualTo(200);
		return csv.text().lines().filter(row -> row.contains("," + record + ",")).toList();
	}

	private void schedule(Page hr, String startAt, String location) {
		hr.navigate(base + "/createschedule?candidateid=C3");
		hr.selectOption("#interviewerid", "U2");
		hr.fill("#startat", startAt);
		hr.fill("#location", location);
		submit(hr, "form#schedule button[type=submit]");
	}

	private static void submit(Page page, String selector) {
		submit(page, page.locator(selector));
	}

	/**
	 * Clicks and waits until the next page has loaded (a POST, then its redirect or its own page); fails at once
	 * when that page is a server error (RMS's error page) instead of waiting for what isn't on it.
	 */
	private static void submit(Page page, Locator button) {
		List<Integer> statuses = new ArrayList<>();
		Consumer<Response> navigation = response -> {
			if (response.request().isNavigationRequest() && response.frame() == page.mainFrame()) {
				statuses.add(response.status());
			}
		};
		page.onResponse(navigation);
		try {
			page.evaluate("window.__rmsBefore = true");
			button.click();
			// A submit that loads no page (a browser-side validation stopped it) fails here after 15 s
			page.waitForFunction("!window.__rmsBefore", null, new Page.WaitForFunctionOptions().setTimeout(15000));
			page.waitForLoadState(LoadState.NETWORKIDLE);
		} finally {
			page.offResponse(navigation);
		}
		assertThat(statuses).as("HTTP status of the pages after the submit").allMatch(status -> status < 500);
	}

	private String path(Page page) {
		return page.url().replace(base, "");
	}

	private static String text(Page page) {
		return text(page, "body");
	}

	private static String text(Page page, String selector) {
		return page.innerText(selector).replaceAll("\\s+", " ");
	}
}
