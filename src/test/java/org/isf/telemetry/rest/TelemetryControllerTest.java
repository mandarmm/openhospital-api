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
package org.isf.telemetry.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.telemetry.envdatacollector.AbstractDataCollector;
import org.isf.telemetry.manager.TelemetryManager;
import org.isf.telemetry.model.Telemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TelemetryControllerTest {

	@Mock
	private TelemetryManager managerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new TelemetryController(managerMock, List.of(collector("TEL_ID", "Telemetry Unique ID"), collector("HW", "Hardware"))))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		when(managerMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private static AbstractDataCollector collector(String id, String description) {
		AbstractDataCollector collector = mock(AbstractDataCollector.class);
		when(collector.getId()).thenReturn(id);
		when(collector.getDescription()).thenReturn(description);
		when(collector.isSelected(anyMap())).thenAnswer(invocation -> {
			Map<String, Boolean> consent = invocation.getArgument(0);
			return Boolean.TRUE.equals(consent.get(id));
		});
		return collector;
	}

	@Test
	void getWithoutSettingsShowsDisabledAndCategories() throws Exception {
		mockMvc.perform(get("/telemetry"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false))
			.andExpect(jsonPath("$.categories[0].id").value("TEL_ID"))
			.andExpect(jsonPath("$.categories[0].mandatory").value(true))
			.andExpect(jsonPath("$.categories[1].description").value("Hardware"))
			.andExpect(jsonPath("$.categories[1].selected").value(false));
	}

	@Test
	void enableStoresConsentWithMandatoryCategory() throws Exception {
		Telemetry enabled = new Telemetry();
		enabled.setActive(true);
		when(managerMock.enable(anyMap())).thenAnswer(invocation -> {
			enabled.setConsentMap(invocation.getArgument(0));
			return enabled;
		});

		mockMvc.perform(put("/telemetry")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"active\":true,\"consent\":{\"HW\":true}}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.categories[1].selected").value(true));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Boolean>> consent = ArgumentCaptor.forClass(Map.class);
		verify(managerMock).enable(consent.capture());
		assertThat(consent.getValue()).containsEntry("TEL_ID", true).containsEntry("HW", true);
	}

	@Test
	void disable() throws Exception {
		Telemetry disabled = new Telemetry();
		disabled.setActive(false);
		when(managerMock.disable(anyMap())).thenReturn(disabled);

		mockMvc.perform(put("/telemetry")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"active\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
		verify(managerMock).save(disabled);
	}

	@Test
	void unknownCategoryIsRejected() throws Exception {
		mockMvc.perform(put("/telemetry")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"active\":true,\"consent\":{\"NOPE\":true}}"))
			.andExpect(status().isBadRequest());
		verify(managerMock, never()).save(any());
	}
}
