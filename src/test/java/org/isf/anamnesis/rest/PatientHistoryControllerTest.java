/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2023 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.anamnesis.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.isf.OpenHospitalApiApplication;
import org.isf.anamnesis.dto.PatientHistoryDTO;
import org.isf.anamnesis.manager.PatientHistoryManager;
import org.isf.anamnesis.model.PatientHistory;
import org.isf.patient.data.PatientHelper;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.utils.exception.OHDataLockFailureException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = OpenHospitalApiApplication.class)
@AutoConfigureMockMvc
class PatientHistoryControllerTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper objectMapper;
	@MockitoBean
	private PatientHistoryManager manager;
	@MockitoBean
	private PatientBrowserManager patientBrowserManager;

	@Test
	@WithMockUser(username = "admin", authorities = { "patients.read" })
	void a_patient_without_history_gets_the_defaults() throws Exception {
		when(patientBrowserManager.getPatientById(7)).thenReturn(PatientHelper.setup());
		mvc.perform(get("/patients/{code}/history", 7))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(0))
			.andExpect(jsonPath("$.patientId").value(7))
			.andExpect(jsonPath("$.phyNutritionNormal").value(true))
			.andExpect(jsonPath("$.familyCancer").value(false));
	}

	@Test
	@WithMockUser(username = "admin", authorities = { "patients.read" })
	void an_unknown_patient_is_404() throws Exception {
		mvc.perform(get("/patients/{code}/history", 8)).andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "admin", authorities = { "patients.update" })
	void saving_keeps_one_history_per_patient() throws Exception {
		when(patientBrowserManager.getPatientById(7)).thenReturn(PatientHelper.setup());
		PatientHistory stored = new PatientHistory();
		stored.setId(12);
		stored.setPatientId(7);
		when(manager.getByPatientId(7)).thenReturn(stored);
		when(manager.saveOrUpdate(any(PatientHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
		PatientHistoryDTO dto = new PatientHistoryDTO();
		dto.setId(99);
		dto.setPatientId(5);
		dto.setFamilyCancer(true);
		dto.setPatAllergy("penicillin");
		dto.setLock(3);

		mvc.perform(put("/patients/{code}/history", 7).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.patAllergy").value("penicillin"));
		ArgumentCaptor<PatientHistory> saved = ArgumentCaptor.forClass(PatientHistory.class);
		verify(manager).saveOrUpdate(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(12);
		assertThat(saved.getValue().getPatientId()).isEqualTo(7);
		assertThat(saved.getValue().isFamilyCancer()).isTrue();
		assertThat(saved.getValue().getLock()).isEqualTo(3);
	}

	@Test
	@WithMockUser(username = "admin", authorities = { "patients.update" })
	void someone_elses_change_is_a_conflict() throws Exception {
		when(patientBrowserManager.getPatientById(anyInt())).thenReturn(PatientHelper.setup());
		when(manager.saveOrUpdate(any(PatientHistory.class))).thenThrow(new OptimisticLockingFailureException("changed"));
		mvc.perform(put("/patients/{code}/history", 7).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new PatientHistoryDTO())))
			.andExpect(status().isConflict());

		// as the core's aspect throws it: checked, not declared
		when(manager.saveOrUpdate(any(PatientHistory.class))).thenAnswer(invocation -> {
			throw new OHDataLockFailureException(
				new org.isf.utils.exception.model.OHExceptionMessage("angal.sql.thedatahasbeenupdatedbysomeoneelse.msg"));
		});
		mvc.perform(put("/patients/{code}/history", 7).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new PatientHistoryDTO())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("angal.sql.thedatahasbeenupdatedbysomeoneelse.msg"));
	}

	@Test
	@WithMockUser(username = "reader", authorities = { "patients.read" })
	void saving_needs_patients_update() throws Exception {
		mvc.perform(put("/patients/{code}/history", 7).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new PatientHistoryDTO())))
			.andExpect(status().isForbidden());
		verify(manager, never()).saveOrUpdate(any(PatientHistory.class));
	}
}
