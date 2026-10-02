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
package org.isf.sms.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.sms.manager.SmsManager;
import org.isf.sms.mapper.SmsMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SmsControllerTest {

	@Mock
	private SmsManager managerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		SmsMapper mapper = new SmsMapper();
		ReflectionTestUtils.setField(mapper, "modelMapper", new ModelMapper());
		mockMvc = MockMvcBuilders
			.standaloneSetup(new SmsController(managerMock, mapper))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		when(managerMock.getAll(any(), any())).thenReturn(List.of());
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void dateRangeIncludesBothDays() throws Exception {
		mockMvc.perform(get("/sms").param("dateFrom", "2026-10-01").param("dateTo", "2026-10-02"))
			.andExpect(status().isOk());
		verify(managerMock).getAll(LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0).with(LocalTime.MAX));
	}

	@Test
	void singleDayCoversTheWholeDay() throws Exception {
		mockMvc.perform(get("/sms").param("dateFrom", "2026-10-02").param("dateTo", "2026-10-02"))
			.andExpect(status().isOk());
		verify(managerMock).getAll(LocalDateTime.of(2026, 10, 2, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0).with(LocalTime.MAX));
	}
}
