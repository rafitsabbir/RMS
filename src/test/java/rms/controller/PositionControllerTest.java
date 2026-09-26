package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rms.config.WebConfig;
import rms.model.PositionInfo;
import rms.service.PositionService;

/** Characterization tests: pin current Position master behaviour before any upgrade. */
@ExtendWith(MockitoExtension.class)
class PositionControllerTest {

	@Mock
	PositionService positionservice;

	@InjectMocks
	PositionController controller;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setViewResolvers(new WebConfig().viewResolver()).build();
	}

	@Test
	void createFormHasEmptyPositionInfo() throws Exception {
		mockMvc.perform(get("/createposition"))
				.andExpect(view().name("createposition"))
				.andExpect(model().attributeExists("positioninfo"));
	}

	@Test
	void saveWithoutKeyAddsAndRedirectsToList() throws Exception {
		mockMvc.perform(post("/saveposition").param("positionkey", "0").param("positionname", "dev ops"))
				.andExpect(redirectedUrl("/viewpositionlist"));

		ArgumentCaptor<PositionInfo> captor = ArgumentCaptor.forClass(PositionInfo.class);
		verify(positionservice).addPosition(captor.capture());
		verify(positionservice, never()).updatePosition(any(PositionInfo.class));
		assertThat(captor.getValue().getPositionname()).isEqualTo("dev ops");
	}

	@Test
	void saveWithKeyUpdatesAndRedirectsToList() throws Exception {
		mockMvc.perform(post("/saveposition").param("positionkey", "5").param("positionname", "lead"))
				.andExpect(redirectedUrl("/viewpositionlist"));

		ArgumentCaptor<PositionInfo> captor = ArgumentCaptor.forClass(PositionInfo.class);
		verify(positionservice).updatePosition(captor.capture());
		verify(positionservice, never()).addPosition(any(PositionInfo.class));
		assertThat(captor.getValue().getPositionkey()).isEqualTo(5);
		assertThat(captor.getValue().getPositionname()).isEqualTo("lead");
	}

	@Test
	void listShowsAllPositionsFromService() throws Exception {
		List<PositionInfo> positions = Arrays.asList(new PositionInfo(), new PositionInfo());
		when(positionservice.getAllPosition()).thenReturn(positions);

		mockMvc.perform(get("/viewpositionlist"))
				.andExpect(view().name("viewposition"))
				.andExpect(model().attribute("positionlist", positions));
	}

	@Test
	void editLoadsPositionIntoCreateForm() throws Exception {
		PositionInfo position = new PositionInfo();
		position.setPositionkey(5);
		when(positionservice.findPositionById(5)).thenReturn(position);

		mockMvc.perform(get("/updateposition/5"))
				.andExpect(view().name("createposition"))
				.andExpect(model().attribute("positioninfo", position));
	}

	@Test
	void deleteIsAGetThatRedirectsToList() throws Exception {
		// characterizes G17: delete is a GET and a hard delete
		mockMvc.perform(get("/deleteposition/5"))
				.andExpect(redirectedUrl("/viewpositionlist"));

		verify(positionservice).deletePosition(5);
	}

}
