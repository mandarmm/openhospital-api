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
package org.isf.hospital.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Time;

import org.isf.hospital.manager.HospitalBrowsingManager;
import org.isf.hospital.mapper.HospitalMapper;
import org.isf.hospital.model.Hospital;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
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

class HospitalControllerTest {

	private static final String BASE = "\"code\":\"STLUKE\",\"description\":\"St. Luke\",\"address\":\"A\",\"city\":\"C\",\"lock\":0";

	@Mock
	private HospitalBrowsingManager managerMock;

	private final HospitalMapper mapper = new HospitalMapper();

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new HospitalController(managerMock, mapper))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		ReflectionTestUtils.setField(mapper, "modelMapper", new ModelMapper());
		Hospital stored = new Hospital("STLUKE", "St. Luke", "A", "C", null, null, null, "EUR",
			Time.valueOf("07:00:00"), Time.valueOf("19:00:00"), 15, 30);
		stored.setLock(0);
		when(managerMock.getHospital()).thenReturn(stored);
		when(managerMock.updateHospital(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void getReturnsVisitingHours() throws Exception {
		mockMvc.perform(get("/hospitals"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.visitStartTime").value("07:00:00"))
			.andExpect(jsonPath("$.visitEndTime").value("19:00:00"))
			.andExpect(jsonPath("$.visitIncrement").value(15))
			.andExpect(jsonPath("$.visitDuration").value(30));
	}

	@Test
	void updateChangesVisitingHours() throws Exception {
		mockMvc.perform(put("/hospitals/STLUKE")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + BASE + ",\"visitStartTime\":\"08:00:00\",\"visitEndTime\":\"18:00:00\",\"visitIncrement\":20,\"visitDuration\":40}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.visitStartTime").value("08:00:00"))
			.andExpect(jsonPath("$.visitDuration").value(40));
	}

	@Test
	void updateWithoutVisitingHoursKeepsStoredOnes() throws Exception {
		mockMvc.perform(put("/hospitals/STLUKE")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + BASE + ",\"fax\":\"123\"}"))
			.andExpect(status().isOk());

		ArgumentCaptor<Hospital> saved = ArgumentCaptor.forClass(Hospital.class);
		verify(managerMock).updateHospital(saved.capture());
		assertThat(saved.getValue().getVisitStartTime()).isEqualTo(Time.valueOf("07:00:00"));
		assertThat(saved.getValue().getVisitEndTime()).isEqualTo(Time.valueOf("19:00:00"));
		assertThat(saved.getValue().getVisitIncrement()).isEqualTo(15);
		assertThat(saved.getValue().getVisitDuration()).isEqualTo(30);
		assertThat(saved.getValue().getFax()).isEqualTo("123");
	}

	@Test
	void startAfterEndIsRejected() throws Exception {
		mockMvc.perform(put("/hospitals/STLUKE")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + BASE + ",\"visitStartTime\":\"20:00:00\",\"visitEndTime\":\"08:00:00\"}"))
			.andExpect(status().isBadRequest());
		verify(managerMock, never()).updateHospital(any());
	}

	@Test
	void visitLongerThanVisitingHoursIsRejected() throws Exception {
		mockMvc.perform(put("/hospitals/STLUKE")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{" + BASE + ",\"visitStartTime\":\"08:00:00\",\"visitEndTime\":\"09:00:00\",\"visitDuration\":90}"))
			.andExpect(status().isBadRequest());
		verify(managerMock, never()).updateHospital(any());
	}
}
