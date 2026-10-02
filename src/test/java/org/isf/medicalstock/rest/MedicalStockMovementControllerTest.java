/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2024 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.medicalstock.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicalstock.manager.MovBrowserManager;
import org.isf.medicalstock.manager.MovStockInsertingManager;
import org.isf.medicalstock.mapper.LotMapper;
import org.isf.medicalstock.mapper.MovementMapper;
import org.isf.medicalstock.model.Movement;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MedicalStockMovementControllerTest {

	@Mock
	private MovBrowserManager movBrowserManagerMock;

	@Mock
	private MovStockInsertingManager movStockInsertingManagerMock;

	@Mock
	private MedicalBrowsingManager medicalBrowsingManagerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new MedicalStockMovementController(new MovementMapper(), new LotMapper(), movBrowserManagerMock,
				movStockInsertingManagerMock, medicalBrowsingManagerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private static Movement movement(int code) {
		Movement movement = new Movement();
		movement.setCode(code);
		return movement;
	}

	@Test
	void testDeleteLastMovement() throws Exception {
		Movement last = movement(12);
		when(movBrowserManagerMock.getLastMovement()).thenReturn(last);

		this.mockMvc.perform(delete("/medicalstockmovements/{code}", 12).param("reason", " wrong quantity "))
			.andExpect(status().isOk())
			.andExpect(content().string("true"));

		verify(movBrowserManagerMock).deleteLastMovement(last, "wrong quantity");
	}

	@Test
	void testDeleteLastMovement_notTheLast() throws Exception {
		when(movBrowserManagerMock.getLastMovement()).thenReturn(movement(12));

		this.mockMvc.perform(delete("/medicalstockmovements/{code}", 11).param("reason", "wrong quantity"))
			.andExpect(status().isBadRequest())
			.andExpect(content().string(containsString("Only the last movement can be deleted.")));

		verify(movBrowserManagerMock, never()).deleteLastMovement(any(), anyString());
	}

	@Test
	void testDeleteLastMovement_reasonRequired() throws Exception {
		when(movBrowserManagerMock.getLastMovement()).thenReturn(movement(12));

		this.mockMvc.perform(delete("/medicalstockmovements/{code}", 12).param("reason", " "))
			.andExpect(status().isBadRequest())
			.andExpect(content().string(containsString("angal.medicalstock.deletemovementreasonrequired.msg")));

		verify(movBrowserManagerMock, never()).deleteLastMovement(any(), anyString());
	}
}
