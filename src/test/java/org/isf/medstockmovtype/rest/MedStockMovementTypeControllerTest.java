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
package org.isf.medstockmovtype.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.isf.medstockmovtype.manager.MedicalDsrStockMovementTypeBrowserManager;
import org.isf.medstockmovtype.mapper.MovementTypeMapper;
import org.isf.medstockmovtype.model.MovementType;
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

class MedStockMovementTypeControllerTest {

	@Mock
	private MedicalDsrStockMovementTypeBrowserManager managerMock;

	private final MovementTypeMapper mapper = new MovementTypeMapper();

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new MedStockMovementTypeController(mapper, managerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		ReflectionTestUtils.setField(mapper, "modelMapper", new ModelMapper());
		when(managerMock.newMedicalDsrStockMovementType(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(managerMock.updateMedicalDsrStockMovementType(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void newMovementTypeDefaultsToOperationalCategory() throws Exception {
		mockMvc.perform(post("/medstockmovementtypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"ZZ\",\"description\":\"Test\",\"type\":\"+\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.category").value("operational"));
	}

	@Test
	void newMovementTypeKeepsGivenCategory() throws Exception {
		mockMvc.perform(post("/medstockmovementtypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"ZZ\",\"description\":\"Test\",\"type\":\"-\",\"category\":\"non-operational\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.category").value("non-operational"));
	}

	@Test
	void newMovementTypeRejectsUnknownCategory() throws Exception {
		mockMvc.perform(post("/medstockmovementtypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"ZZ\",\"description\":\"Test\",\"type\":\"+\",\"category\":\"other\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void updateWithoutCategoryKeepsStoredCategory() throws Exception {
		when(managerMock.getMovementType("ZZ")).thenReturn(new MovementType("ZZ", "Test", "-", "non-operational"));

		mockMvc.perform(put("/medstockmovementtypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"ZZ\",\"description\":\"Updated\",\"type\":\"-\"}"))
			.andExpect(status().isOk());

		ArgumentCaptor<MovementType> saved = ArgumentCaptor.forClass(MovementType.class);
		verify(managerMock).updateMedicalDsrStockMovementType(saved.capture());
		assertThat(saved.getValue().getCategory()).isEqualTo("non-operational");
		assertThat(saved.getValue().getDescription()).isEqualTo("Updated");
	}

	@Test
	void updateUnknownMovementTypeIsNotFound() throws Exception {
		mockMvc.perform(put("/medstockmovementtypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"XX\",\"description\":\"Test\",\"type\":\"-\"}"))
			.andExpect(status().isNotFound());
	}
}
