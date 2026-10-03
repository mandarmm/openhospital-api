/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2026 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.accounting.rest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.isf.OpenHospitalApiApplication;
import org.isf.accounting.manager.BillBrowserManager;
import org.isf.accounting.model.Bill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The bills are protected by the {@code bills.*} permissions.
 */
@SpringBootTest(classes = OpenHospitalApiApplication.class)
@AutoConfigureMockMvc
class BillSecurityTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private BillBrowserManager billManager;

	@Test
	@WithMockUser(username = "guest", authorities = { "patients.read" })
	void withoutTheBillPermissions_forbidden() throws Exception {
		mvc.perform(get("/bills/{id}", 1)).andExpect(status().isForbidden());
		mvc.perform(post("/bills/search/by/payments").contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isForbidden());
		mvc.perform(post("/bills").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
		mvc.perform(delete("/bills/{id}", 1)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "cashier", authorities = { "bills.read" })
	void readingNeedsBillsRead_notTheOthers() throws Exception {
		when(billManager.getBill(anyInt())).thenReturn(new Bill());
		mvc.perform(get("/bills/{id}", 1)).andExpect(status().isOk());
		mvc.perform(delete("/bills/{id}", 1)).andExpect(status().isForbidden());
		mvc.perform(post("/bills").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
	}
}
