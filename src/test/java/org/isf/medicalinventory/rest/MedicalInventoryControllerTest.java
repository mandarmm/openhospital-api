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
package org.isf.medicalinventory.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.isf.medical.mapper.MedicalMapper;
import org.isf.medicalinventory.manager.MedicalInventoryManager;
import org.isf.medicalinventory.manager.MedicalInventoryRowManager;
import org.isf.medicalinventory.model.MedicalInventory;
import org.isf.medicalinventory.model.MedicalInventoryRow;
import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicals.model.Medical;
import org.isf.medicalstock.manager.MovStockInsertingManager;
import org.isf.medicalstock.mapper.LotMapper;
import org.isf.medicalstock.model.Lot;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.shared.mapper.mappings.LotMapping;
import org.isf.utils.exception.OHDataValidationException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.modelmapper.module.jsr310.Jsr310Module;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MedicalInventoryControllerTest {

	@Mock
	private MedicalInventoryManager inventoryManagerMock;

	@Mock
	private MedicalInventoryRowManager rowManagerMock;

	@Mock
	private MedicalBrowsingManager medicalManagerMock;

	@Mock
	private MovStockInsertingManager movStockInsertingManagerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.registerModule(new Jsr310Module());
		LotMapping.addMapping(modelMapper);
		LotMapper lotMapper = new LotMapper();
		ReflectionTestUtils.setField(lotMapper, "modelMapper", modelMapper);
		MedicalMapper medicalMapper = new MedicalMapper();
		ReflectionTestUtils.setField(medicalMapper, "modelMapper", modelMapper);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new MedicalInventoryController(inventoryManagerMock, rowManagerMock, medicalManagerMock, movStockInsertingManagerMock,
				medicalMapper, lotMapper))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		login("medicalstockmovements.read", "medicalstockmovements.create", "medicalstockmovements.update", "medicalstockmovements.delete");
	}

	@AfterEach
	void closeService() throws Exception {
		SecurityContextHolder.clearContext();
		closeable.close();
	}

	private static void login(String... permissions) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("storekeeper", null,
			java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
	}

	private static MedicalInventory inventory(String status) {
		MedicalInventory inventory = new MedicalInventory(5, status, LocalDateTime.of(2026, 10, 1, 9, 0), "storekeeper", "INV-1", "main", null);
		inventory.setChargeType("charge");
		inventory.setDischargeType("discharge");
		inventory.setSupplier(1);
		inventory.setDestination("I");
		return inventory;
	}

	private static MedicalInventoryRow row(MedicalInventory inventory) {
		Lot lot = new Lot("L1");
		return new MedicalInventoryRow(3, BigDecimal.TEN, BigDecimal.valueOf(8), inventory, new Medical(40), lot);
	}

	@Test
	void testNewInventory_aDraftOfTheUser() throws Exception {
		when(medicalManagerMock.getMedical(40)).thenReturn(new Medical(40));
		when(movStockInsertingManagerMock.getLot("L1")).thenReturn(new Lot("L1"));
		when(inventoryManagerMock.newMedicalInventory(any(), anyList())).thenAnswer(invocation -> {
			MedicalInventory saved = invocation.getArgument(0);
			saved.setId(5);
			return saved;
		});
		String body = """
			{"inventory":{"inventoryDate":"2026-10-01T09:00:00","inventoryReference":"INV-1","inventoryType":"main"},
			 "rows":[{"theoreticQty":10,"realQty":8,"medical":{"code":40},"lot":{"code":"L1"}}]}""";

		this.mockMvc.perform(post("/medicalinventories").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated());

		ArgumentCaptor<MedicalInventory> inventory = ArgumentCaptor.forClass(MedicalInventory.class);
		verify(inventoryManagerMock).newMedicalInventory(inventory.capture(), anyList());
		assertThat(inventory.getValue().getStatus()).isEqualTo("draft");
		assertThat(inventory.getValue().getUser()).isEqualTo("storekeeper");
		ArgumentCaptor<MedicalInventoryRow> row = ArgumentCaptor.forClass(MedicalInventoryRow.class);
		verify(rowManagerMock).newMedicalInventoryRow(row.capture());
		assertThat(row.getValue().getLot().getCode()).isEqualTo("L1");
		assertThat(row.getValue().getRealQty()).isEqualByComparingTo("8");
	}

	@Test
	void testNewInventory_needsTheStockPermissionOfItsType() throws Exception {
		login("medicalstockmovements.create");
		String body = """
			{"inventory":{"inventoryDate":"2026-10-01T09:00:00","inventoryReference":"INV-W","inventoryType":"ward","wardCode":"I"}}""";

		this.mockMvc.perform(post("/medicalinventories").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isForbidden());
		verify(inventoryManagerMock, never()).newMedicalInventory(any(), anyList());
	}

	@Test
	void testValidate_reportsChangesOrActualizes() throws Exception {
		MedicalInventory inventory = inventory("draft");
		when(inventoryManagerMock.getInventoryById(5)).thenReturn(inventory);
		when(rowManagerMock.getMedicalInventoryRowByInventoryId(5)).thenReturn(List.of(row(inventory)));
		doThrow(new OHDataValidationException(new OHExceptionMessage("angal.inventory.theoreticalqtyhavebeenupdatedforsomemedical.fmt.msg")))
			.when(inventoryManagerMock).validateMedicalInventoryRow(any(), anyList(), anyBoolean());

		this.mockMvc.perform(post("/medicalinventories/{id}/validate", 5))
			.andExpect(status().isConflict())
			.andExpect(content().string(containsString("theoreticalqtyhavebeenupdated")));
		verify(inventoryManagerMock, never()).actualizeMedicalInventoryRow(any(), anyBoolean());

		this.mockMvc.perform(post("/medicalinventories/{id}/validate", 5).param("actualize", "true"))
			.andExpect(status().isOk());
		verify(inventoryManagerMock).actualizeMedicalInventoryRow(inventory, false);
	}

	@Test
	void testValidate_needsTheParameters() throws Exception {
		MedicalInventory inventory = inventory("draft");
		inventory.setSupplier(null);
		when(inventoryManagerMock.getInventoryById(5)).thenReturn(inventory);
		when(rowManagerMock.getMedicalInventoryRowByInventoryId(5)).thenReturn(List.of(row(inventory)));

		this.mockMvc.perform(post("/medicalinventories/{id}/validate", 5))
			.andExpect(status().isBadRequest())
			.andExpect(content().string(containsString("angal.inventory.choosesupplierbeforevalidation.msg")));
	}

	@Test
	void testConfirm_onlyValidated() throws Exception {
		MedicalInventory draft = inventory("draft");
		when(inventoryManagerMock.getInventoryById(5)).thenReturn(draft);
		this.mockMvc.perform(post("/medicalinventories/{id}/confirm", 5))
			.andExpect(status().isBadRequest());
		verify(inventoryManagerMock, never()).confirmMedicalInventoryRow(any(), anyList(), anyBoolean());

		MedicalInventory validated = inventory("validated");
		when(inventoryManagerMock.getInventoryById(5)).thenReturn(validated);
		when(rowManagerMock.getMedicalInventoryRowByInventoryId(5)).thenReturn(List.of(row(validated)));
		this.mockMvc.perform(post("/medicalinventories/{id}/confirm", 5))
			.andExpect(status().isOk());
		verify(inventoryManagerMock).confirmMedicalInventoryRow(eq(validated), anyList(), eq(false));
	}

	@Test
	void testDelete_notWhenDone() throws Exception {
		when(inventoryManagerMock.getInventoryById(5)).thenReturn(inventory("done"));
		this.mockMvc.perform(delete("/medicalinventories/{id}", 5))
			.andExpect(status().isBadRequest());
		verify(inventoryManagerMock, never()).deleteInventory(any());
	}
}
