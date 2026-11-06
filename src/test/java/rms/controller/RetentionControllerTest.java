package rms.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.RetentionInfo;
import rms.model.Role;
import rms.service.Outcome;
import rms.service.RetentionService;

/** F11: the page lists who is due; the post cleans up the chosen ones with the Super Admin as the user. */
@ExtendWith(MockitoExtension.class)
class RetentionControllerTest {

	@Mock
	RetentionService retentionservice;

	@InjectMocks
	RetentionController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void thePageListsWhoIsDue() throws Exception {
		List<RetentionInfo> due = List.of(new RetentionInfo());
		when(retentionservice.getMonths()).thenReturn(24);
		when(retentionservice.getDue()).thenReturn(due);

		mockMvc.perform(get("/viewretention")).andExpect(view().name("viewretention"))
				.andExpect(model().attribute("months", 24)).andExpect(model().attribute("duelist", due));
	}

	@Test
	void theChosenCandidatesAreCleanedUp() throws Exception {
		when(retentionservice.cleanUp(List.of("C2", "C5"), "U1")).thenReturn(Outcome.ok("2 candidates' personal data removed."));

		mockMvc.perform(post("/applyretention").param("candidateid", "C2", "C5")
				.session(EvaluationControllerTest.session("U1", Role.SUPER_ADMIN)))
				.andExpect(redirectedUrl("/viewretention"))
				.andExpect(flash().attribute("message", "2 candidates' personal data removed."));
		verify(retentionservice).cleanUp(eq(List.of("C2", "C5")), eq("U1"));
	}
}
