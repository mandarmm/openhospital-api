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
package org.isf.patient.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.isf.admission.manager.AdmissionBrowserManager;
import org.isf.admission.model.Admission;
import org.isf.patconsensus.manager.PatientConsensusBrowserManager;
import org.isf.patient.data.PatientHelper;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.mapper.PatientMapper;
import org.isf.patient.model.Patient;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.shared.mapper.converter.BlobToByteArrayConverter;
import org.isf.shared.mapper.converter.ByteArrayToBlobConverter;
import org.isf.shared.mapper.mappings.PatientMapping;
import org.isf.utils.pagination.PageInfo;
import org.isf.utils.pagination.PagedResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@code GET /patients/find}: paged free-text search for the reception desk.
 */
class PatientFindTest {

	@Mock
	private PatientBrowserManager patientManagerMock;
	@Mock
	private AdmissionBrowserManager admissionManagerMock;
	@Mock
	private PatientConsensusBrowserManager consensusManagerMock;

	private final PatientMapper patientMapper = new PatientMapper();

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new PatientController(patientManagerMock, admissionManagerMock, patientMapper, consensusManagerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		ModelMapper modelMapper = new ModelMapper();
		PatientMapping.addMapping(modelMapper);
		modelMapper.addConverter(new BlobToByteArrayConverter());
		modelMapper.addConverter(new ByteArrayToBlobConverter());
		ReflectionTestUtils.setField(patientMapper, "modelMapper", modelMapper);
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void searchIsPagedAndShowsAdmissionStatus() throws Exception {
		List<Patient> matches = PatientHelper.setupPatientList(25);
		when(patientManagerMock.getPatientsByOneOfFieldsLike("kruse")).thenReturn(matches);
		when(admissionManagerMock.getCurrentAdmission(argThat(patient -> patient != null && patient.getCode() == 15)))
			.thenReturn(new Admission());

		mockMvc.perform(get("/patients/find").param("q", " kruse ").param("page", "1").param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(10))
			.andExpect(jsonPath("$.data[0].code").value(11))
			.andExpect(jsonPath("$.data[0].blobPhoto").doesNotExist())
			.andExpect(jsonPath("$.data[0].status").value("O"))
			.andExpect(jsonPath("$.data[?(@.code==15)].status").value("I"))
			.andExpect(jsonPath("$.pageInfo.totalNbOfElements").value(25))
			.andExpect(jsonPath("$.pageInfo.totalPages").value(3))
			.andExpect(jsonPath("$.pageInfo.hasPreviousPage").value(true))
			.andExpect(jsonPath("$.pageInfo.hasNextPage").value(true));
	}

	@Test
	void lastPageOfSearch() throws Exception {
		when(patientManagerMock.getPatientsByOneOfFieldsLike("kruse")).thenReturn(PatientHelper.setupPatientList(25));

		mockMvc.perform(get("/patients/find").param("q", "kruse").param("page", "2").param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(5))
			.andExpect(jsonPath("$.pageInfo.hasNextPage").value(false));
	}

	@Test
	void withoutSearchAllPatientsArePagedByTheCore() throws Exception {
		PagedResponse<Patient> paged = new PagedResponse<>();
		paged.setData(PatientHelper.setupPatientList(3));
		PageInfo pageInfo = new PageInfo();
		pageInfo.setPage(0);
		pageInfo.setSize(3);
		pageInfo.setNbOfElements(3);
		pageInfo.setTotalNbOfElements(600);
		pageInfo.setTotalPages(200);
		pageInfo.setHasNextPage(true);
		paged.setPageInfo(pageInfo);
		when(patientManagerMock.getPatientsPageable(0, 3)).thenReturn(paged);

		mockMvc.perform(get("/patients/find").param("size", "3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(3))
			.andExpect(jsonPath("$.pageInfo.totalNbOfElements").value(600));
		verify(patientManagerMock, never()).getPatientsByOneOfFieldsLike(any());
	}

	@Test
	void invalidPageSizeIsRejected() throws Exception {
		mockMvc.perform(get("/patients/find").param("q", "x").param("size", "500"))
			.andExpect(status().isBadRequest());
	}
}
