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
package org.isf.opd.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.isf.opd.data.OpdHelper;
import org.isf.opd.dto.OpdDTO;
import org.isf.opd.manager.OpdBrowserManager;
import org.isf.opd.mapper.OpdMapper;
import org.isf.opd.model.Opd;
import org.isf.operation.manager.OperationRowBrowserManager;
import org.isf.operation.mapper.OperationRowMapper;
import org.isf.patient.data.PatientHelper;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.shared.mapper.converter.BlobToByteArrayConverter;
import org.isf.shared.mapper.converter.ByteArrayToBlobConverter;
import org.isf.visits.model.Visit;
import org.isf.ward.manager.WardBrowserManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OpdControllerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpdControllerTest.class);

	@Mock
	protected OpdBrowserManager opdBrowserManagerMock;

	@Mock
	protected PatientBrowserManager patientBrowserManagerMock;

	protected OpdMapper opdMapper = new OpdMapper();

	@Mock
	protected OperationRowBrowserManager operationRowBrowserManagerMock;

	protected OperationRowMapper opRowMapper = new OperationRowMapper();

	@Mock
	protected WardBrowserManager wardBrowserManager;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new OpdController(opdBrowserManagerMock, opdMapper, patientBrowserManagerMock, operationRowBrowserManagerMock,
				opRowMapper, wardBrowserManager))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.addConverter(new BlobToByteArrayConverter());
		modelMapper.addConverter(new ByteArrayToBlobConverter());
		ReflectionTestUtils.setField(opdMapper, "modelMapper", modelMapper);
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void testNewOpd_201() throws Exception {
		String request = "/opds";
		Patient patient = PatientHelper.setup();
		Integer patientCode = 1;
		patient.setCode(patientCode);

		Opd opd = OpdHelper.setup();
		opd.setPatient(patient);

		OpdDTO body = opdMapper.map2DTO(opd);

		when(patientBrowserManagerMock.getPatientById(patientCode)).thenReturn(patient);

		when(opdBrowserManagerMock.newOpd(opdMapper.map2Model(body))).thenReturn(opd);

		MvcResult result = this.mockMvc
			.perform(post(request)
				.contentType(MediaType.APPLICATION_JSON)
				.content(Objects.requireNonNull(OpdHelper.asJsonString(body))))
			.andDo(log())
			.andExpect(status().is2xxSuccessful())
			.andExpect(status().isCreated())
			.andReturn();

		LOGGER.debug("result: {}", result);
	}

	@Test
	void testOpdNumberAndNextVisitDateMapping() throws Exception {
		Opd opd = OpdHelper.setup();
		opd.setProgYear(42);
		Visit nextVisit = new Visit();
		LocalDateTime nextVisitDate = LocalDateTime.of(2026, 10, 9, 10, 30);
		nextVisit.setDate(nextVisitDate);
		opd.setNextVisit(nextVisit);

		OpdDTO dto = opdMapper.map2DTO(opd);
		assertThat(dto.getProg_year()).isEqualTo(42);
		assertThat(dto.getNextVisitDate()).isEqualTo(nextVisitDate);

		// the OPD number is kept; the next visit date is read-only and does not fail the mapping
		Opd model = opdMapper.map2Model(dto);
		assertThat(model.getProgYear()).isEqualTo(42);
		assertThat(model.getNextVisit()).isNull();
	}

	@Test
	void testUpdateOpd_keepsStoredNextVisit() throws Exception {
		int code = 5;
		Patient patient = PatientHelper.setup();
		patient.setCode(1);
		Opd stored = OpdHelper.setup();
		stored.setCode(code);
		stored.setPatient(patient);
		Visit nextVisit = new Visit();
		nextVisit.setDate(LocalDateTime.of(2026, 10, 9, 10, 30));
		stored.setNextVisit(nextVisit);
		OpdDTO body = opdMapper.map2DTO(stored);

		when(opdBrowserManagerMock.getOpdById(code)).thenReturn(Optional.of(stored));
		when(patientBrowserManagerMock.getPatientById(1)).thenReturn(patient);
		when(opdBrowserManagerMock.updateOpd(any(Opd.class))).thenAnswer(invocation -> invocation.getArgument(0));

		this.mockMvc
			.perform(put("/opds/{code}", code)
				.contentType(MediaType.APPLICATION_JSON)
				.content(Objects.requireNonNull(OpdHelper.asJsonString(body))))
			.andExpect(status().isOk());

		ArgumentCaptor<Opd> updated = ArgumentCaptor.forClass(Opd.class);
		verify(opdBrowserManagerMock).updateOpd(updated.capture());
		assertThat(updated.getValue().getNextVisit()).isSameAs(nextVisit);
	}

	@Test
	void testDeleteOpd_deletesStoredOpd() throws Exception {
		int code = 5;
		Opd stored = OpdHelper.setup();
		stored.setCode(code);
		stored.setLock(2);
		when(opdBrowserManagerMock.getOpdById(code)).thenReturn(Optional.of(stored));

		this.mockMvc.perform(delete("/opds/{code}", code))
			.andExpect(status().isOk());

		verify(opdBrowserManagerMock).deleteOpd(stored);
	}

	@Test
	void testDeleteOpd_404() throws Exception {
		when(opdBrowserManagerMock.getOpdById(5)).thenReturn(Optional.empty());

		this.mockMvc.perform(delete("/opds/{code}", 5))
			.andExpect(status().isNotFound());

		verify(opdBrowserManagerMock, never()).deleteOpd(any(Opd.class));
	}

}
