package rms.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import rms.model.ActivityInfo;
import rms.service.ActivityService;

/** The activity log page (Super Admin only, SecurityConfig). */
@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

	@Mock
	ActivityService activityservice;

	@InjectMocks
	ActivityController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void theLogShowsTheNewestEntriesAndTheActionFilter() throws Exception {
		List<ActivityInfo> entries = List.of(new ActivityInfo());
		when(activityservice.getRecent(null)).thenReturn(entries);
		when(activityservice.getActions()).thenReturn(List.of("MARKED", "SELECTED"));

		mockMvc.perform(get("/viewactivity"))
				.andExpect(status().isOk())
				.andExpect(view().name("viewactivity"))
				.andExpect(model().attribute("activitylist", entries))
				.andExpect(model().attribute("actions", List.of("MARKED", "SELECTED")))
				.andExpect(model().attribute("selectedaction", ""));
	}

	@Test
	void anActionFiltersTheLog() throws Exception {
		when(activityservice.getRecent(" SELECTED ")).thenReturn(List.of());

		mockMvc.perform(get("/viewactivity").param("action", " SELECTED "))
				.andExpect(model().attribute("selectedaction", "SELECTED"));

		verify(activityservice).getRecent(" SELECTED ");
	}
}
