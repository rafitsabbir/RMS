package rms.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.service.HealthService;

/** The health check: 200 "ok" with the database, 503 "unavailable" without, plain text and nothing more. */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

	@Mock
	HealthService healthservice;

	@InjectMocks
	HealthController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void upIsOk() throws Exception {
		when(healthservice.isDatabaseUp()).thenReturn(true);

		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
				.andExpect(content().string("ok"));
	}

	@Test
	void withoutTheDatabaseItIsUnavailable() throws Exception {
		when(healthservice.isDatabaseUp()).thenReturn(false);

		mockMvc.perform(get("/health"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().string("unavailable"));
	}
}
