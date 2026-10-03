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
package org.isf.stats.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicals.model.Medical;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.stat.dto.JasperReportResultDto;
import org.isf.stat.manager.JasperReportsManager;
import org.isf.ward.manager.WardBrowserManager;
import org.isf.ward.model.Ward;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.sf.jasperreports.engine.JasperPrint;

class ReportsControllerTest {

	@Mock
	private JasperReportsManager reportsManagerMock;

	@Mock
	private WardBrowserManager wardManagerMock;

	@Mock
	private MedicalBrowsingManager medicalManagerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new ReportsController(reportsManagerMock, wardManagerMock, medicalManagerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private static JasperReportResultDto emptyReport() {
		JasperPrint print = new JasperPrint();
		print.setPageWidth(595);
		print.setPageHeight(842);
		return new JasperReportResultDto(print, "x.jasper", "x.pdf");
	}

	@Test
	void testWardStock_thePdfOfTheWardAtTheEndOfTheDay() throws Exception {
		Ward ward = new Ward();
		ward.setCode("I");
		ward.setDescription("Internal medicine");
		when(wardManagerMock.findWard("I")).thenReturn(ward);
		when(reportsManagerMock.getGenericReportPharmaceuticalStockWardPdf(any(), eq("PharmaceuticalStockWard"), eq(ward))).thenReturn(emptyReport());

		this.mockMvc.perform(get("/reports/ward-stock").param("wardCode", "I").param("date", "2026-10-01"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF));

		ArgumentCaptor<LocalDateTime> date = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(reportsManagerMock).getGenericReportPharmaceuticalStockWardPdf(date.capture(), eq("PharmaceuticalStockWard"), eq(ward));
		assertThat(date.getValue().toLocalDate()).isEqualTo("2026-10-01");
		assertThat(date.getValue().getHour()).isEqualTo(23);
	}

	@Test
	void testWardStock_unknownWard() throws Exception {
		this.mockMvc.perform(get("/reports/ward-stock").param("wardCode", "ZZ"))
			.andExpect(status().isNotFound());
	}

	@Test
	void testStockCard_theReportOfTheStoreMedicalOrAll() throws Exception {
		Ward ward = new Ward();
		ward.setCode("I");
		ward.setDescription("Internal medicine");
		Medical medical = new Medical(40);
		when(wardManagerMock.findWard("I")).thenReturn(ward);
		when(medicalManagerMock.getMedical(40)).thenReturn(medical);
		when(reportsManagerMock.getGenericReportPharmaceuticalStockCardPdf(any(), any(), any(), any(), any(), any())).thenReturn(emptyReport());

		this.mockMvc.perform(get("/reports/stock-card").param("wardCode", "I").param("medicalCode", "40")
				.param("dateFrom", "2026-09-01").param("dateTo", "2026-09-30"))
			.andExpect(status().isOk());
		verify(reportsManagerMock).getGenericReportPharmaceuticalStockCardPdf(eq("ProductLedgerWard"), isNull(), any(), any(), eq(medical), eq(ward));

		this.mockMvc.perform(get("/reports/stock-card").param("dateFrom", "2026-09-01").param("dateTo", "2026-09-30"))
			.andExpect(status().isOk());
		verify(reportsManagerMock).getGenericReportPharmaceuticalStockCardPdf(eq("ProductLedger_multi"), isNull(), any(), any(), isNull(), isNull());
	}

	@Test
	void testStockCard_datesInOrder() throws Exception {
		this.mockMvc.perform(get("/reports/stock-card").param("dateFrom", "2026-09-30").param("dateTo", "2026-09-01"))
			.andExpect(status().isBadRequest());
		verify(reportsManagerMock, never()).getGenericReportPharmaceuticalStockCardPdf(any(), any(), any(), any(), any(), any());
	}
}
