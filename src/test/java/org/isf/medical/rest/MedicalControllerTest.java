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
package org.isf.medical.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.isf.medical.dto.MedicalDTO;
import org.isf.medical.mapper.MedicalMapper;
import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicals.model.Medical;
import org.isf.medtype.mapper.MedicalTypeMapper;
import org.isf.medtype.model.MedicalType;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.utils.exception.OHDataIntegrityViolationException;
import org.isf.utils.exception.OHDataValidationException;
import org.isf.utils.exception.model.OHExceptionMessage;
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

import com.fasterxml.jackson.databind.ObjectMapper;

class MedicalControllerTest {

	@Mock
	private MedicalBrowsingManager managerMock;

	private final MedicalTypeMapper medicalTypeMapper = new MedicalTypeMapper();

	private final MedicalMapper mapper = new MedicalMapper(medicalTypeMapper);

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		ReflectionTestUtils.setField(medicalTypeMapper, "modelMapper", modelMapper);
		ReflectionTestUtils.setField(mapper, "modelMapper", modelMapper);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new MedicalController(managerMock, mapper))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private static Medical medical() {
		Medical medical = new Medical(7, new MedicalType("K", "Chemical"), "AB12", "Test medical", 10, 5, 0, 0);
		medical.setLock(0);
		return medical;
	}

	@Test
	void testGetMedical_returnsTheProductCode() throws Exception {
		when(managerMock.getMedical(7)).thenReturn(medical());

		this.mockMvc.perform(get("/medicals/{code}", 7))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.prod_code").value("AB12"));
	}

	@Test
	void testNewMedical_passesTheProductCode() throws Exception {
		when(managerMock.newMedical(any(Medical.class), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
		MedicalDTO body = mapper.map2DTO(medical());
		body.setCode(null);

		this.mockMvc.perform(post("/medicals").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.prod_code").value("AB12"));

		ArgumentCaptor<Medical> created = ArgumentCaptor.forClass(Medical.class);
		verify(managerMock).newMedical(created.capture(), anyBoolean());
		assertThat(created.getValue().getProdCode()).isEqualTo("AB12");
	}

	@Test
	void testNewMedical_withoutProductCode() throws Exception {
		when(managerMock.newMedical(any(Medical.class), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
		MedicalDTO body = mapper.map2DTO(medical());
		body.setCode(null);
		body.setProd_code(null);

		this.mockMvc.perform(post("/medicals").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated());

		ArgumentCaptor<Medical> created = ArgumentCaptor.forClass(Medical.class);
		verify(managerMock).newMedical(created.capture(), anyBoolean());
		assertThat(created.getValue().getProdCode()).isEmpty();
	}

	@Test
	void testNewMedical_returnsTheCoresMessage() throws Exception {
		when(managerMock.newMedical(any(Medical.class), anyBoolean()))
			.thenThrow(new OHDataValidationException(new OHExceptionMessage("angal.common.thecodeisalreadyinuse.msg")));

		this.mockMvc.perform(post("/medicals").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapper.map2DTO(medical()))))
			.andExpect(status().isBadRequest())
			.andExpect(content().string(containsString("angal.common.thecodeisalreadyinuse.msg")));
	}

	@Test
	void testUpdateMedical_passesTheProductCode() throws Exception {
		when(managerMock.updateMedical(any(Medical.class), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));

		this.mockMvc.perform(put("/medicals").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapper.map2DTO(medical()))))
			.andExpect(status().isOk());

		ArgumentCaptor<Medical> updated = ArgumentCaptor.forClass(Medical.class);
		verify(managerMock).updateMedical(updated.capture(), anyBoolean());
		assertThat(updated.getValue().getProdCode()).isEqualTo("AB12");
	}

	@Test
	void testDeleteMedical_returnsTheCoresMessage() throws Exception {
		Medical medical = medical();
		when(managerMock.getMedical(7)).thenReturn(medical);
		org.mockito.Mockito.doThrow(new OHDataIntegrityViolationException(
				new OHExceptionMessage("angal.medicals.therearestockmovementsreferredtothismedical.msg")))
			.when(managerMock).deleteMedical(medical);

		this.mockMvc.perform(delete("/medicals/{code}", 7))
			.andExpect(content().string(containsString("angal.medicals.therearestockmovementsreferredtothismedical.msg")));
	}
}
