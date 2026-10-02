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
package org.isf.visits.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Objects;

import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.shared.mapper.converter.BlobToByteArrayConverter;
import org.isf.shared.mapper.converter.ByteArrayToBlobConverter;
import org.isf.shared.mapper.mappings.PatientMapping;
import org.isf.visits.data.VisitHelper;
import org.isf.visits.dto.VisitDTO;
import org.isf.visits.manager.VisitManager;
import org.isf.visits.mapper.VisitMapper;
import org.isf.visits.model.Visit;
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

class VisitsControllerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(VisitsControllerTest.class);

	@Mock
	protected VisitManager visitManagerMock;

	protected VisitMapper visitMapper = new VisitMapper();

	@Mock
	protected PatientBrowserManager patientManagerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		this.mockMvc = MockMvcBuilders
				.standaloneSetup(new VisitsController(visitManagerMock, visitMapper, patientManagerMock))
				.setControllerAdvice(new OHResponseEntityExceptionHandler())
				.build();
		ModelMapper modelMapper = new ModelMapper();
		PatientMapping.addMapping(modelMapper);
		modelMapper.addConverter(new BlobToByteArrayConverter());
		modelMapper.addConverter(new ByteArrayToBlobConverter());
		ReflectionTestUtils.setField(visitMapper, "modelMapper", modelMapper);
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void testGetVisit_200() throws Exception {
		String request = "/visits/patient/{patID}";

		int patID = 0;
		List<Visit> visitsList = VisitHelper.setupVisitList(4);

		when(visitManagerMock.getVisits(patID))
				.thenReturn(visitsList);

		List<VisitDTO> expectedVisitsDTOs = visitMapper.map2DTOList(visitsList);

		MvcResult result = this.mockMvc
				.perform(get(request, patID))
				.andDo(log())
				.andExpect(status().is2xxSuccessful())
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(VisitHelper.getObjectMapper().writeValueAsString(expectedVisitsDTOs))))
				.andReturn();

		LOGGER.debug("result: {}", result);
	}

	@Test
	void testNewVisit_201() throws Exception {
		String request = "/visits";
		int id = 1;
		Visit visit = VisitHelper.setup(id);
		VisitDTO body = visitMapper.map2DTO(visit);
		when(patientManagerMock.getPatientById(any())).thenReturn(visit.getPatient());

		when(visitManagerMock.newVisit(visitMapper.map2Model(body)))
				.thenReturn(visitMapper.map2Model(body));

		MvcResult result = this.mockMvc
				.perform(post(request)
						.contentType(MediaType.APPLICATION_JSON)
						.content(Objects.requireNonNull(VisitHelper.asJsonString(body)))
				)
				.andDo(log())
				.andExpect(status().is2xxSuccessful())
				.andExpect(status().isCreated())
				.andReturn();

		LOGGER.debug("result: {}", result);
	}

	@Test
	void testNewVisits_201() throws Exception {
		String request = "/visits/insertList";

		List<Visit> visitsList = VisitHelper.setupVisitList(4);

		List<VisitDTO> body = visitMapper.map2DTOList(visitsList);

		Boolean isCreated = true;
		when(visitManagerMock.newVisits(visitsList))
				.thenReturn(isCreated);

		MvcResult result = this.mockMvc
				.perform(post(request)
						.contentType(MediaType.APPLICATION_JSON)
						.content(Objects.requireNonNull(VisitHelper.asJsonString(body)))
				)
				.andDo(log())
				.andExpect(status().is2xxSuccessful())
				.andExpect(status().isCreated())
				.andExpect(content().string(containsString(isCreated.toString())))
				.andReturn();
		LOGGER.debug("result: {}", result);
	}

	@Test
	void testDeleteVisitsRelatedToPatient_200() throws Exception {
		String request = "/visits/delete/{patId}";

		int id = 1;

		Boolean isDeleted = true;
		when(visitManagerMock.deleteAllVisits(id))
				.thenReturn(isDeleted);

		MvcResult result = this.mockMvc
				.perform(delete(request, id))
				.andDo(log())
				.andExpect(status().is2xxSuccessful())
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(isDeleted.toString())))
				.andReturn();

		LOGGER.debug("result: {}", result);
	}

	@Test
	void testNewVisit_usesStoredPatient() throws Exception {
		Visit visit = VisitHelper.setup(1);
		Patient stored = visit.getPatient();
		VisitDTO body = visitMapper.map2DTO(visit);
		// a payload carrying only the patient's code
		body.getPatient().setFirstName(null);
		body.getPatient().setSex(' ');
		when(patientManagerMock.getPatientById(any())).thenReturn(stored);
		when(visitManagerMock.newVisit(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		this.mockMvc
				.perform(post("/visits")
						.contentType(MediaType.APPLICATION_JSON)
						.content(Objects.requireNonNull(VisitHelper.asJsonString(body))))
				.andExpect(status().isCreated());

		ArgumentCaptor<Visit> created = ArgumentCaptor.forClass(Visit.class);
		verify(visitManagerMock).newVisit(created.capture());
		assertThat(created.getValue().getPatient()).isSameAs(stored);
	}

	@Test
	void testNewVisit_unknownPatient_404() throws Exception {
		VisitDTO body = visitMapper.map2DTO(VisitHelper.setup(1));
		when(patientManagerMock.getPatientById(any())).thenReturn(null);

		this.mockMvc
				.perform(post("/visits")
						.contentType(MediaType.APPLICATION_JSON)
						.content(Objects.requireNonNull(VisitHelper.asJsonString(body))))
				.andExpect(status().isNotFound());

		verify(visitManagerMock, never()).newVisit(any(Visit.class));
	}

	@Test
	void testDeleteVisit_200() throws Exception {
		Visit visit = VisitHelper.setup(7);
		when(visitManagerMock.findVisit(7)).thenReturn(visit);

		this.mockMvc.perform(delete("/visits/{visitID}", 7))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		verify(visitManagerMock).deleteVisit(visit);
	}

	@Test
	void testDeleteVisit_404() throws Exception {
		when(visitManagerMock.findVisit(anyInt())).thenReturn(null);

		this.mockMvc.perform(delete("/visits/{visitID}", 7))
				.andExpect(status().isNotFound());

		verify(visitManagerMock, never()).deleteVisit(any(Visit.class));
	}

}
