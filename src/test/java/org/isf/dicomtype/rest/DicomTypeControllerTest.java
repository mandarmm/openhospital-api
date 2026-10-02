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
package org.isf.dicomtype.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.isf.dicomtype.manager.DicomTypeBrowserManager;
import org.isf.dicomtype.mapper.DicomTypeMapper;
import org.isf.dicomtype.model.DicomType;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class DicomTypeControllerTest {

	@Mock
	private DicomTypeBrowserManager managerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	private final DicomType ctScan = new DicomType("CTS", "CT-Scan");

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
			.standaloneSetup(new DicomTypeController(managerMock, new DicomTypeMapper()))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.setValidator(validator)
			.build();
		when(managerMock.getDicomType()).thenReturn(List.of(ctScan));
		when(managerMock.newDicomType(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(managerMock.updateDicomType(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void listDicomTypes() throws Exception {
		mockMvc.perform(get("/dicomtypes"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("CTS"))
			.andExpect(jsonPath("$[0].description").value("CT-Scan"));
	}

	@Test
	void createDicomType() throws Exception {
		mockMvc.perform(post("/dicomtypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"MRI\",\"description\":\"MRI\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("MRI"));
	}

	@Test
	void codeLongerThanThreeCharactersIsRejected() throws Exception {
		mockMvc.perform(post("/dicomtypes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"TOOLONG\",\"description\":\"x\"}"))
			.andExpect(status().isBadRequest());
		verify(managerMock, never()).newDicomType(any());
	}

	@Test
	void updateDicomType() throws Exception {
		mockMvc.perform(put("/dicomtypes/CTS")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"CTS\",\"description\":\"CT scan\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.description").value("CT scan"));
	}

	@Test
	void updateUnknownOrMismatchedTypeIsRejected() throws Exception {
		mockMvc.perform(put("/dicomtypes/XYZ")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"XYZ\",\"description\":\"x\"}"))
			.andExpect(status().isNotFound());
		mockMvc.perform(put("/dicomtypes/CTS")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"GEN\",\"description\":\"x\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void deleteDicomType() throws Exception {
		mockMvc.perform(delete("/dicomtypes/CTS")).andExpect(status().isNoContent());
		verify(managerMock).deleteDicomType(ctScan);
		mockMvc.perform(delete("/dicomtypes/XYZ")).andExpect(status().isNotFound());
	}
}
