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
package org.isf.patvac.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.isf.patient.data.PatientHelper;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.mapper.PatientMapper;
import org.isf.patient.model.Patient;
import org.isf.patvac.manager.PatVacManager;
import org.isf.patvac.mapper.PatVacMapper;
import org.isf.patvac.model.PatientVaccine;
import org.isf.patvac.service.PatVacIoOperations;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.shared.mapper.converter.BlobToByteArrayConverter;
import org.isf.shared.mapper.converter.ByteArrayToBlobConverter;
import org.isf.shared.mapper.mappings.PatientMapping;
import org.isf.utils.exception.OHDataValidationException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.vaccine.manager.VaccineBrowserManager;
import org.isf.vaccine.mapper.VaccineMapper;
import org.isf.vaccine.model.Vaccine;
import org.isf.vactype.model.VaccineType;
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

class PatVacControllerTest {

	@Mock
	private PatVacManager patVacManagerMock;

	@Mock
	private PatVacIoOperations patVacIoOperationsMock;

	@Mock
	private PatientBrowserManager patientManagerMock;

	@Mock
	private VaccineBrowserManager vaccineManagerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	private Patient patient;

	private Vaccine vaccine;

	private static final String BODY = """
		{"progr":3,"vaccineDate":"2026-10-02T10:00:00","patient":{"code":301},"vaccine":{"code":"1"}}""";

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		PatientMapping.addMapping(modelMapper);
		modelMapper.addConverter(new BlobToByteArrayConverter());
		modelMapper.addConverter(new ByteArrayToBlobConverter());
		PatientMapper patientMapper = new PatientMapper();
		VaccineMapper vaccineMapper = new VaccineMapper();
		ReflectionTestUtils.setField(patientMapper, "modelMapper", modelMapper);
		ReflectionTestUtils.setField(vaccineMapper, "modelMapper", modelMapper);
		PatVacMapper mapper = new PatVacMapper(patientMapper, vaccineMapper);
		ReflectionTestUtils.setField(mapper, "modelMapper", modelMapper);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new PatVacController(patVacManagerMock, mapper, patVacIoOperationsMock, patientManagerMock, vaccineManagerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		patient = PatientHelper.setup();
		patient.setCode(301);
		vaccine = new Vaccine("1", "BCG", new VaccineType("C", "Child"));
		when(patientManagerMock.getPatientById(301)).thenReturn(patient);
		when(vaccineManagerMock.findVaccine("1")).thenReturn(vaccine);
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void testNewPatientVaccine_mapsTheDateAndUsesTheStoredPatientAndVaccine() throws Exception {
		when(patVacManagerMock.newPatientVaccine(any())).thenAnswer(invocation -> invocation.getArgument(0));

		this.mockMvc.perform(post("/patientvaccines").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.progr").value(3));

		ArgumentCaptor<PatientVaccine> created = ArgumentCaptor.forClass(PatientVaccine.class);
		verify(patVacManagerMock).newPatientVaccine(created.capture());
		assertThat(created.getValue().getVaccineDate()).isEqualTo(LocalDateTime.of(2026, 10, 2, 10, 0));
		assertThat(created.getValue().getPatient()).isSameAs(patient);
		assertThat(created.getValue().getVaccine()).isSameAs(vaccine);
	}

	@Test
	void testNewPatientVaccine_returnsTheCoresMessage() throws Exception {
		when(patVacManagerMock.newPatientVaccine(any()))
			.thenThrow(new OHDataValidationException(new OHExceptionMessage("angal.patvac.pleaseinsertavalidprogressive.msg")));

		this.mockMvc.perform(post("/patientvaccines").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isBadRequest())
			.andExpect(content().string(containsString("angal.patvac.pleaseinsertavalidprogressive.msg")));
	}

	@Test
	void testDeletePatientVaccine_theStoredOne() throws Exception {
		PatientVaccine stored = new PatientVaccine(7, 3, LocalDateTime.of(2026, 10, 2, 10, 0), patient, vaccine, 2);
		when(patVacManagerMock.getPatientVaccine(7)).thenReturn(Optional.of(stored));

		this.mockMvc.perform(delete("/patientvaccines/{code}", 7)).andExpect(status().isOk());
		verify(patVacManagerMock).deletePatientVaccine(stored);

		when(patVacManagerMock.getPatientVaccine(8)).thenReturn(Optional.empty());
		this.mockMvc.perform(delete("/patientvaccines/{code}", 8)).andExpect(status().isNotFound());
	}

	@Test
	void testGetPatientVaccinesOfPatient() throws Exception {
		when(patVacIoOperationsMock.findForPatient(301))
			.thenReturn(List.of(new PatientVaccine(7, 3, LocalDateTime.of(2026, 10, 2, 10, 0), patient, vaccine, 0)));

		this.mockMvc.perform(get("/patientvaccines/patient/{patientCode}", 301))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value(7))
			.andExpect(jsonPath("$[0].vaccine.code").value("1"));
	}

	@Test
	void testFilter_includesTheLastDay() throws Exception {
		when(patVacManagerMock.getPatientVaccine(any(), any(), any(), any(), anyChar(), anyInt(), anyInt())).thenReturn(List.of());

		this.mockMvc.perform(get("/patientvaccines/filter").param("vaccineTypeCode", "").param("vaccineCode", "")
				.param("dateFrom", "2026-10-01").param("dateTo", "2026-10-02").param("sex", "A").param("ageFrom", "0").param("ageTo", "200"))
			.andExpect(status().isOk());

		// empty: all types and vaccines
		verify(patVacManagerMock).getPatientVaccine(eq(null), eq(null), eq(LocalDate.of(2026, 10, 1).atStartOfDay()),
			eq(LocalDate.of(2026, 10, 2).atTime(LocalTime.MAX)), eq('A'), eq(0), eq(200));
	}
}
