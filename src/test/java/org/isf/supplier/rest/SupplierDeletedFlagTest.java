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
package org.isf.supplier.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.supplier.manager.SupplierBrowserManager;
import org.isf.supplier.mapper.SupplierMapper;
import org.isf.supplier.model.Supplier;
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

/**
 * The deleted flag of suppliers.
 */
class SupplierDeletedFlagTest {

	@Mock
	private SupplierBrowserManager managerMock;

	private final SupplierMapper mapper = new SupplierMapper();

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new SupplierController(managerMock, mapper))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		ReflectionTestUtils.setField(mapper, "modelMapper", new ModelMapper());
		when(managerMock.saveOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private static Supplier supplier(int id, char deleted) {
		Supplier supplier = new Supplier(id, "Supplier " + id, null, null, null, null, null, null);
		supplier.setSupDeleted(deleted);
		return supplier;
	}

	@Test
	void listIncludingDeletedShowsFlag() throws Exception {
		when(managerMock.getAll()).thenReturn(List.of(supplier(1, 'N'), supplier(2, 'Y')));

		mockMvc.perform(get("/suppliers").param("exclude_deleted", "false"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].supDeleted").value(false))
			.andExpect(jsonPath("$[1].supDeleted").value(true));
	}

	@Test
	void updateWithoutFlagKeepsDeletedSupplierDeleted() throws Exception {
		// the real getByID() does not find deleted suppliers
		when(managerMock.getAll()).thenReturn(List.of(supplier(1, 'N'), supplier(2, 'Y')));

		mockMvc.perform(put("/suppliers")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"supId\":2,\"supName\":\"Renamed\",\"lock\":0}"))
			.andExpect(status().isOk());

		ArgumentCaptor<Supplier> saved = ArgumentCaptor.forClass(Supplier.class);
		verify(managerMock).saveOrUpdate(saved.capture());
		assertThat(saved.getValue().getSupDeleted()).isEqualTo('Y');
		assertThat(saved.getValue().getSupName()).isEqualTo("Renamed");
	}

	@Test
	void updateCanRestoreDeletedSupplier() throws Exception {
		when(managerMock.getAll()).thenReturn(List.of(supplier(2, 'Y')));

		mockMvc.perform(put("/suppliers")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"supId\":2,\"supName\":\"Supplier 2\",\"supDeleted\":false,\"lock\":0}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.supDeleted").value(false));
	}
}
