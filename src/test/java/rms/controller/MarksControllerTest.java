package rms.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Arrays;
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
import rms.model.MarksInfo;
import rms.service.MarksService;

/** Characterization tests: pin the admin results page model before any upgrade. */
@ExtendWith(MockitoExtension.class)
class MarksControllerTest {

	@Mock
	MarksService marksservice;

	@InjectMocks
	MarksController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void adminViewMarksShowsAllMarks() throws Exception {
		List<MarksInfo> marks = Arrays.asList(new MarksInfo());
		when(marksservice.getAllMarksByAdmin()).thenReturn(marks);

		mockMvc.perform(get("/adminviewmarks"))
				.andExpect(status().isOk())
				.andExpect(view().name("viewmarks"))
				.andExpect(model().attribute("markslist", marks));
	}

}
