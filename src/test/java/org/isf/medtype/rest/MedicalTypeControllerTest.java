/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2025 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.isf.medtype.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.isf.medtype.manager.MedicalTypeBrowserManager;
import org.isf.medtype.mapper.MedicalTypeMapper;
import org.isf.medtype.model.MedicalType;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MedicalTypeControllerTest {

	@Mock
	private MedicalTypeBrowserManager managerMock;

	private final MedicalTypeMapper mapper = new MedicalTypeMapper();

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new MedicalTypeController(managerMock, mapper))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		ReflectionTestUtils.setField(mapper, "modelMapper", new ModelMapper());
		when(managerMock.newMedicalType(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(managerMock.updateMedicalType(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void listShowsDeletedFlag() throws Exception {
		MedicalType deleted = new MedicalType("X", "Deleted type");
		deleted.setDeleted('Y');
		when(managerMock.getMedicalType()).thenReturn(List.of(new MedicalType("D", "Drugs"), deleted));

		mockMvc.perform(get("/medicaltypes"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].deleted").value(false))
			.andExpect(jsonPath("$[1].deleted").value(true));
	}

	@Test
	void newMedicalTypeIsNotDeletedByDefault() throws Exception {
		mockMvc.perform(post("/medicaltypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"Z\",\"description\":\"Test\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.deleted").value(false));
	}

	@Test
	void updateCanMarkDeleted() throws Exception {
		when(managerMock.getMedicalType()).thenReturn(List.of(new MedicalType("Z", "Test")));

		mockMvc.perform(put("/medicaltypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"Z\",\"description\":\"Test\",\"deleted\":true}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.deleted").value(true));
	}

	@Test
	void updateWithoutFlagKeepsStoredFlag() throws Exception {
		MedicalType stored = new MedicalType("Z", "Test");
		stored.setDeleted('Y');
		when(managerMock.getMedicalType()).thenReturn(List.of(stored));

		mockMvc.perform(put("/medicaltypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"Z\",\"description\":\"Updated\"}"))
			.andExpect(status().isOk());

		ArgumentCaptor<MedicalType> saved = ArgumentCaptor.forClass(MedicalType.class);
		verify(managerMock).updateMedicalType(saved.capture());
		assertThat(saved.getValue().getDeleted()).isEqualTo('Y');
		assertThat(saved.getValue().getDescription()).isEqualTo("Updated");
	}

	@Test
	void updateUnknownMedicalTypeIsNotFound() throws Exception {
		when(managerMock.getMedicalType()).thenReturn(List.of());

		mockMvc.perform(put("/medicaltypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"Q\",\"description\":\"Test\"}"))
			.andExpect(status().isNotFound());
	}
}
