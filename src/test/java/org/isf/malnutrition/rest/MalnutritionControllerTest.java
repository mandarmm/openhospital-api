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
package org.isf.malnutrition.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;

import org.isf.admission.model.Admission;
import org.isf.malnutrition.manager.MalnutritionManager;
import org.isf.malnutrition.mapper.MalnutritionMapper;
import org.isf.malnutrition.model.Malnutrition;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MalnutritionControllerTest {

	@Mock
	private MalnutritionManager managerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new MalnutritionController(new MalnutritionMapper(), managerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void testDelete_aControl() throws Exception {
		Malnutrition control = new Malnutrition();
		control.setCode(7);
		control.setAdmission(new Admission());
		when(managerMock.getMalnutrition(7)).thenReturn(control);

		this.mockMvc.perform(delete("/malnutritions").param("code", "7")).andExpect(status().isOk());
		verify(managerMock).deleteMalnutrition(control);
	}

	@Test
	void testDelete_aMissingControl() throws Exception {
		// the core gives a reference that fails when read
		Malnutrition missing = mock(Malnutrition.class);
		when(missing.getAdmission()).thenThrow(new EntityNotFoundException());
		when(managerMock.getMalnutrition(8)).thenReturn(missing);

		this.mockMvc.perform(delete("/malnutritions").param("code", "8")).andExpect(status().isNotFound());
		verify(managerMock, never()).deleteMalnutrition(any());
	}
}
