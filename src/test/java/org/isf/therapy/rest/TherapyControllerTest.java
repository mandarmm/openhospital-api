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
package org.isf.therapy.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.isf.medical.mapper.MedicalMapper;
import org.isf.medicals.model.Medical;
import org.isf.patient.data.PatientHelper;
import org.isf.patient.mapper.PatientMapper;
import org.isf.patient.model.Patient;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.shared.mapper.converter.BlobToByteArrayConverter;
import org.isf.shared.mapper.converter.ByteArrayToBlobConverter;
import org.isf.shared.mapper.mappings.PatientMapping;
import org.isf.sms.service.SmsOperations;
import org.isf.therapy.dto.TherapyRowDTO;
import org.isf.therapy.manager.TherapyManager;
import org.isf.therapy.mapper.TherapyMapper;
import org.isf.therapy.mapper.TherapyRowMapper;
import org.isf.therapy.model.TherapyRow;
import org.isf.therapy.service.TherapyIoOperationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class TherapyControllerTest {

	@Mock
	private TherapyManager managerMock;

	@Mock
	private TherapyIoOperationRepository repositoryMock;

	@Mock
	private SmsOperations smsOperationsMock;

	private final TherapyRowMapper therapyRowMapper = new TherapyRowMapper();

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	private Patient patient;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		PatientMapping.addMapping(modelMapper);
		modelMapper.addConverter(new BlobToByteArrayConverter());
		modelMapper.addConverter(new ByteArrayToBlobConverter());
		PatientMapper patientMapper = new PatientMapper();
		ReflectionTestUtils.setField(patientMapper, "modelMapper", modelMapper);
		ReflectionTestUtils.setField(therapyRowMapper, "modelMapper", modelMapper);
		ReflectionTestUtils.setField(therapyRowMapper, "patientMapper", patientMapper);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new TherapyController(managerMock, new TherapyMapper(), therapyRowMapper, new MedicalMapper(), repositoryMock, smsOperationsMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		patient = PatientHelper.setup();
		patient.setCode(7);
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private TherapyRow therapy(int id) {
		Medical medical = new Medical();
		medical.setCode(12);
		return new TherapyRow(id, patient, LocalDateTime.of(2026, 10, 1, 8, 0), LocalDateTime.of(2026, 10, 7, 8, 0), medical, 1.0, 0, 2, 1,
			"note", false, false);
	}

	private String json(Object body) throws Exception {
		return objectMapper.writeValueAsString(body);
	}

	@Test
	void testReplaceTherapies_replacesAllOfThePatient() throws Exception {
		List<TherapyRowDTO> body = List.of(therapyRowMapper.map2DTO(therapy(3)), therapyRowMapper.map2DTO(therapy(4)));
		when(managerMock.getTherapyRows(7)).thenReturn(List.of(therapy(10), therapy(11)));

		this.mockMvc.perform(post("/therapies/replace").contentType(MediaType.APPLICATION_JSON).content(json(body)))
			.andExpect(status().isCreated());

		InOrder order = inOrder(managerMock);
		order.verify(managerMock).deleteAllTherapies(7);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<TherapyRow>> inserted = ArgumentCaptor.forClass(List.class);
		order.verify(managerMock).newTherapies(inserted.capture());
		assertThat(inserted.getValue()).hasSize(2).allMatch(row -> row.getTherapyID() == 0);
	}

	@Test
	void testReplaceTherapies_empty_400() throws Exception {
		this.mockMvc.perform(post("/therapies/replace").contentType(MediaType.APPLICATION_JSON).content("[]"))
			.andExpect(status().isBadRequest());
		verify(managerMock, never()).deleteAllTherapies(any());
		verify(managerMock, never()).newTherapies(anyList());
	}

	@Test
	void testUpdateTherapy_keepsThePatient() throws Exception {
		TherapyRow stored = therapy(5);
		when(repositoryMock.findById(5)).thenReturn(Optional.of(stored));
		when(managerMock.newTherapy(any(TherapyRow.class))).thenAnswer(invocation -> invocation.getArgument(0));
		TherapyRowDTO body = therapyRowMapper.map2DTO(therapy(5));
		body.setQty(3.0);

		this.mockMvc.perform(put("/therapies/rows/{therapyID}", 5).contentType(MediaType.APPLICATION_JSON).content(json(body)))
			.andExpect(status().isOk());

		ArgumentCaptor<TherapyRow> saved = ArgumentCaptor.forClass(TherapyRow.class);
		verify(managerMock).newTherapy(saved.capture());
		assertThat(saved.getValue().getTherapyID()).isEqualTo(5);
		assertThat(saved.getValue().getQty()).isEqualTo(3.0);
		assertThat(saved.getValue().getPatient()).isSameAs(stored.getPatient());
	}

	@Test
	void testNewTherapy_reschedulesTheSmsOfThePatient() throws Exception {
		TherapyRow created = therapy(9);
		when(managerMock.newTherapy(any(TherapyRow.class))).thenReturn(created);
		List<TherapyRow> patientTherapies = List.of(therapy(8), created);
		when(managerMock.getTherapyRows(7)).thenReturn(patientTherapies);

		this.mockMvc.perform(post("/therapies").contentType(MediaType.APPLICATION_JSON).content(json(therapyRowMapper.map2DTO(therapy(0)))))
			.andExpect(status().isCreated());

		verify(managerMock).newTherapies(patientTherapies);
	}

	@Test
	void testUpdateTherapy_404() throws Exception {
		when(repositoryMock.findById(anyInt())).thenReturn(Optional.empty());
		this.mockMvc.perform(put("/therapies/rows/{therapyID}", 5).contentType(MediaType.APPLICATION_JSON)
				.content(json(therapyRowMapper.map2DTO(therapy(5)))))
			.andExpect(status().isNotFound());
		verify(managerMock, never()).newTherapy(any(TherapyRow.class));
	}

	@Test
	void testDeleteTherapy() throws Exception {
		TherapyRow stored = therapy(5);
		when(repositoryMock.findById(5)).thenReturn(Optional.of(stored));
		when(managerMock.getTherapyRows(7)).thenReturn(List.of());
		this.mockMvc.perform(delete("/therapies/rows/{therapyID}", 5))
			.andExpect(status().isOk());
		verify(repositoryMock).delete(stored);
		// the last therapy: its reminders go too
		verify(smsOperationsMock).deleteByModuleModuleID("therapy", "7");
	}

	@Test
	void testDeleteTherapy_404() throws Exception {
		when(repositoryMock.findById(anyInt())).thenReturn(Optional.empty());
		this.mockMvc.perform(delete("/therapies/rows/{therapyID}", 5))
			.andExpect(status().isNotFound());
		verify(repositoryMock, never()).delete(any(TherapyRow.class));
	}
}
