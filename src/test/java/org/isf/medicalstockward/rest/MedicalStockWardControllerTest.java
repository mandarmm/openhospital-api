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
package org.isf.medicalstockward.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicals.model.Medical;
import org.isf.medicalstock.mapper.LotMapper;
import org.isf.medicalstock.model.Lot;
import org.isf.medicalstockward.manager.MovWardBrowserManager;
import org.isf.medicalstockward.mapper.MedicalWardMapper;
import org.isf.medicalstockward.mapper.MovementWardMapper;
import org.isf.medicalstockward.model.MedicalWard;
import org.isf.medicalstockward.model.MovementWard;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.shared.mapper.mappings.LotMapping;
import org.isf.shared.mapper.mappings.PatientMapping;
import org.isf.ward.manager.WardBrowserManager;
import org.isf.ward.model.Ward;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.modelmapper.module.jsr310.Jsr310Module;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MedicalStockWardControllerTest {

	@Mock
	private MovWardBrowserManager movWardBrowserManagerMock;

	@Mock
	private MedicalBrowsingManager medicalBrowsingManagerMock;

	@Mock
	private WardBrowserManager wardBrowserManagerMock;

	private final MedicalWardMapper medicalWardMapper = new MedicalWardMapper();

	private final MovementWardMapper movementWardMapper = new MovementWardMapper();

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.registerModule(new Jsr310Module());
		LotMapping.addMapping(modelMapper);
		PatientMapping.addMapping(modelMapper);
		LotMapper lotMapper = new LotMapper();
		ReflectionTestUtils.setField(lotMapper, "modelMapper", modelMapper);
		for (Object mapper : List.of(medicalWardMapper, movementWardMapper)) {
			ReflectionTestUtils.setField(mapper, "modelMapper", modelMapper);
			ReflectionTestUtils.setField(mapper, "lotMapper", lotMapper);
		}
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new MedicalStockWardController(medicalWardMapper, movementWardMapper, movWardBrowserManagerMock,
				medicalBrowsingManagerMock, wardBrowserManagerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private static Ward ward() {
		Ward ward = new Ward();
		ward.setCode("I");
		ward.setDescription("Male ward");
		return ward;
	}

	private static Medical medical() {
		Medical medical = new Medical(40);
		medical.setDescription("Test medical");
		return medical;
	}

	private static Lot lot(String code, LocalDate due) {
		return new Lot(medical(), code, due.minusYears(1).atStartOfDay(), due.atStartOfDay(), BigDecimal.ONE);
	}

	@Test
	void testGetMedicalsWard_withTheLot() throws Exception {
		when(movWardBrowserManagerMock.getMedicalsWard("I", true))
			.thenReturn(List.of(new MedicalWard(ward(), medical(), 100, BigDecimal.ONE, lot("L1", LocalDate.of(2027, 3, 1)))));

		this.mockMvc.perform(get("/medicalstockward/{ward_code}", "I"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id.lot.code").value("L1"))
			// the standalone set-up writes dates as [year, month, day]
			.andExpect(jsonPath("$[0].id.lot.dueDate[0]").value(2027))
			.andExpect(jsonPath("$[0].in_quantity").value(100));
	}

	@Test
	void testNewMovementWard_keepsTheLot() throws Exception {
		String body = """
			{"ward":{"code":"I","description":"Male ward"},"date":"2026-10-02T10:00:00","isPatient":false,
			 "description":"stock count","medical":{"code":40,"description":"Test medical"},"quantity":2,"units":"pcs",
			 "lot":{"code":"L1","preparationDate":"2026-03-01","dueDate":"2027-03-01","cost":1}}""";

		this.mockMvc.perform(post("/medicalstockward/movements").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated());

		ArgumentCaptor<MovementWard> saved = ArgumentCaptor.forClass(MovementWard.class);
		verify(movWardBrowserManagerMock).newMovementWard(saved.capture());
		assertThat(saved.getValue().getLot()).isNotNull();
		assertThat(saved.getValue().getLot().getCode()).isEqualTo("L1");
	}

	@Test
	@SuppressWarnings("unchecked")
	void testNewMovementsWard_severalLotsAtOnce() throws Exception {
		String body = """
			[{"ward":{"code":"I"},"date":"2026-10-02T10:00:00","isPatient":false,"description":"to ward",
			  "medical":{"code":40},"quantity":3,"units":"pcs","lot":{"code":"L1"}},
			 {"ward":{"code":"I"},"date":"2026-10-02T10:00:00","isPatient":false,"description":"to ward",
			  "medical":{"code":40},"quantity":2,"units":"pcs","lot":{"code":"L2"}}]""";

		this.mockMvc.perform(post("/medicalstockward/movements/list").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated());

		ArgumentCaptor<List<MovementWard>> saved = ArgumentCaptor.forClass(List.class);
		verify(movWardBrowserManagerMock).newMovementWard(saved.capture());
		assertThat(saved.getValue()).extracting(movement -> movement.getLot().getCode()).containsExactly("L1", "L2");
	}

	@Test
	void testGetMovementWard_includesTheLastDay() throws Exception {
		when(movWardBrowserManagerMock.getMovementWard(eq("I"), any(), any())).thenReturn(List.of());

		this.mockMvc.perform(get("/medicalstockward/movements/{ward_code}", "I").param("from", "2026-10-01").param("to", "2026-10-02"))
			.andExpect(status().isOk());

		verify(movWardBrowserManagerMock).getMovementWard("I", LocalDateTime.of(2026, 10, 1, 0, 0),
			LocalDate.of(2026, 10, 2).atTime(LocalTime.MAX));
	}

	@Test
	void testDeleteLastMovementWard() throws Exception {
		Ward ward = ward();
		MovementWard last = new MovementWard();
		last.setCode(9);
		when(wardBrowserManagerMock.getWards()).thenReturn(List.of(ward));
		when(movWardBrowserManagerMock.getLastMovementWard(ward)).thenReturn(last);

		this.mockMvc.perform(delete("/medicalstockward/movements/{ward_code}/{code}", "I", 9).param("reason", " wrong patient "))
			.andExpect(status().isOk());
		verify(movWardBrowserManagerMock).deleteLastMovementWard(last, "wrong patient");

		this.mockMvc.perform(delete("/medicalstockward/movements/{ward_code}/{code}", "I", 8).param("reason", "x"))
			.andExpect(status().isBadRequest());
		this.mockMvc.perform(delete("/medicalstockward/movements/{ward_code}/{code}", "I", 9).param("reason", " "))
			.andExpect(status().isBadRequest());
		verify(movWardBrowserManagerMock).deleteLastMovementWard(any(), any());
	}
}
