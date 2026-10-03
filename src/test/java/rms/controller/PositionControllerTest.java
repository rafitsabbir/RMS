package rms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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

/** Characterization tests: pin current Position master behaviour. */
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
		when(positionservice.addPosition(any(PositionInfo.class))).thenReturn(true);

		mockMvc.perform(post("/saveposition").param("positionkey", "0").param("positionname", "dev ops"))
				.andExpect(redirectedUrl("/viewpositionlist"));

		ArgumentCaptor<PositionInfo> captor = ArgumentCaptor.forClass(PositionInfo.class);
		verify(positionservice).addPosition(captor.capture());
		verify(positionservice, never()).updatePosition(any(PositionInfo.class));
		assertThat(captor.getValue().getPositionname()).isEqualTo("dev ops");
	}

	@Test
	void saveWithKeyUpdatesAndRedirectsToList() throws Exception {
		when(positionservice.findPositionById(5)).thenReturn(new PositionInfo());
		when(positionservice.updatePosition(any(PositionInfo.class))).thenReturn(true);

		mockMvc.perform(post("/saveposition").param("positionkey", "5").param("positionname", "lead"))
				.andExpect(redirectedUrl("/viewpositionlist"));

		ArgumentCaptor<PositionInfo> captor = ArgumentCaptor.forClass(PositionInfo.class);
		verify(positionservice).updatePosition(captor.capture());
		verify(positionservice, never()).addPosition(any(PositionInfo.class));
		assertThat(captor.getValue().getPositionkey()).isEqualTo(5);
		assertThat(captor.getValue().getPositionname()).isEqualTo("lead");
	}

	@Test
	void saveWithBlankNameShowsFormWithError() throws Exception {
		mockMvc.perform(post("/saveposition").param("positionkey", "0").param("positionname", "   "))
				.andExpect(status().isOk())
				.andExpect(view().name("createposition"))
				.andExpect(model().attribute("errorMessage", "Please enter a position name."));

		verifyNoInteractions(positionservice);
	}

	@Test
	void saveWithoutNameShowsFormWithError() throws Exception {
		mockMvc.perform(post("/saveposition").param("positionkey", "0"))
				.andExpect(view().name("createposition"))
				.andExpect(model().attribute("errorMessage", "Please enter a position name."));

		verifyNoInteractions(positionservice);
	}

	@Test
	void addOfDuplicateShowsFormWithErrorAndKeepsInput() throws Exception {
		when(positionservice.addPosition(any(PositionInfo.class))).thenReturn(false);

		mockMvc.perform(post("/saveposition").param("positionkey", "0").param("positionname", " dev ops "))
				.andExpect(status().isOk())
				.andExpect(view().name("createposition"))
				.andExpect(model().attribute("errorMessage", "Position DEV OPS already exists."))
				.andExpect(model().attribute("positioninfo", hasProperty("positionname", is(" dev ops "))));
	}

	@Test
	void updateToDuplicateShowsFormWithErrorAndKeepsKey() throws Exception {
		when(positionservice.findPositionById(5)).thenReturn(new PositionInfo());
		when(positionservice.updatePosition(any(PositionInfo.class))).thenReturn(false);

		mockMvc.perform(post("/saveposition").param("positionkey", "5").param("positionname", "lead"))
				.andExpect(view().name("createposition"))
				.andExpect(model().attribute("errorMessage", "Position LEAD already exists."))
				.andExpect(model().attribute("positioninfo", hasProperty("positionkey", is(5))));
	}

	@Test
	void saveOfDeletedPositionIs404() throws Exception {
		when(positionservice.findPositionById(5)).thenReturn(null);

		mockMvc.perform(post("/saveposition").param("positionkey", "5").param("positionname", "x"))
				.andExpect(status().isNotFound());

		verify(positionservice, never()).updatePosition(any(PositionInfo.class));
		verify(positionservice, never()).addPosition(any(PositionInfo.class));
	}

	@Test
	void listShowsAllPositionsFromService() throws Exception {
		List<PositionInfo> items = Arrays.asList(new PositionInfo(), new PositionInfo());
		when(positionservice.getAllPosition()).thenReturn(items);

		mockMvc.perform(get("/viewpositionlist"))
				.andExpect(view().name("viewposition"))
				.andExpect(model().attribute("positionlist", items));
	}

	@Test
	void editLoadsPositionIntoCreateForm() throws Exception {
		PositionInfo item = new PositionInfo();
		item.setPositionkey(5);
		when(positionservice.findPositionById(5)).thenReturn(item);

		mockMvc.perform(get("/updateposition/5"))
				.andExpect(view().name("createposition"))
				.andExpect(model().attribute("positioninfo", item));
	}

	@Test
	void editOfMissingOrDeletedPositionIs404() throws Exception {
		when(positionservice.findPositionById(99)).thenReturn(null);

		mockMvc.perform(get("/updateposition/99")).andExpect(status().isNotFound());
	}

	@Test
	void deleteIsAPostThatRedirectsToList() throws Exception {
		mockMvc.perform(post("/deleteposition/5"))
				.andExpect(redirectedUrl("/viewpositionlist"));

		verify(positionservice).deletePosition(5);
	}

	@Test
	void deleteByGetIsRefused() throws Exception {
		mockMvc.perform(get("/deleteposition/5")).andExpect(status().isMethodNotAllowed());

		verifyNoInteractions(positionservice);
	}


	@Test
	void theNameMustBeEnglish() throws Exception {
		mockMvc.perform(post("/saveposition").param("positionkey", "0").param("positionname", "Ingénieur"))
				.andExpect(status().isOk())
				.andExpect(view().name("createposition"));

		verifyNoInteractions(positionservice);
	}
}
