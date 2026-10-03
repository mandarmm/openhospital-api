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
package org.isf.stats.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.isf.generaldata.GeneralData;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.stat.dto.JasperReportResultDto;
import org.isf.stat.manager.JasperReportsManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JasperDesign;

class StatisticsReportsControllerTest {

	@Mock
	private JasperReportsManager reportsManagerMock;

	@TempDir
	Path installation;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		GeneralData.LANGUAGE = "en";
		// rpt_stat: a period report (with a French title), a month report and a sub-report without a title
		report("rpt_stat", "OH001_Patients", new String[] {"fromdate", "todate"},
			"OH001 - Registered Patients");
		Files.createDirectories(installation.resolve("rpt_stat/fr"));
		Files.writeString(installation.resolve("rpt_stat/fr/OH001_Patients.properties"), "jTitle = OH001 - Patients enregistrés\n");
		report("rpt_extra", "HMIS108", new String[] {"month", "year"},
			"HMIS 108 - Inpatient Monthly Report");
		report("rpt_stat", "OH001_Patients_subreport", new String[0], null);
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new StatisticsReportsController(reportsManagerMock, installation))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	/** A report with the parameters, saved compiled as the installation has it, with its title if any. */
	private void report(String folder, String name, String[] parameters, String title) throws Exception {
		Path dir = Files.createDirectories(installation.resolve(folder));
		JasperDesign design = new JasperDesign();
		design.setName(name);
		for (String parameterName : parameters) {
			JRDesignParameter parameter = new JRDesignParameter();
			parameter.setName(parameterName);
			parameter.setValueClass(String.class);
			design.addParameter(parameter);
		}
		JasperCompileManager.compileReportToFile(design, dir.resolve(name + ".jasper").toString());
		if (title != null) {
			Files.writeString(dir.resolve(name + ".properties"), "jTitle = " + title + "\n");
		}
	}

	private static JasperReportResultDto emptyReport() {
		JasperPrint print = new JasperPrint();
		print.setPageWidth(595);
		print.setPageHeight(842);
		return new JasperReportResultDto(print, "x.jasper", "x.pdf");
	}

	@Test
	void testList_theTitledReportsWithTheirKind() throws Exception {
		this.mockMvc.perform(get("/reports/statistics"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].name").value("HMIS108"))
			.andExpect(jsonPath("$[0].folder").value("rpt_extra"))
			.andExpect(jsonPath("$[0].kind").value("month"))
			.andExpect(jsonPath("$[1].name").value("OH001_Patients"))
			.andExpect(jsonPath("$[1].kind").value("period"))
			.andExpect(jsonPath("$[1].title").value("OH001 - Registered Patients"));

		this.mockMvc.perform(get("/reports/statistics").param("language", "fr"))
			.andExpect(jsonPath("$[1].title").value("OH001 - Patients enregistrés"));
		// not a language: the hospital's
		this.mockMvc.perform(get("/reports/statistics").param("language", "../.."))
			.andExpect(jsonPath("$[1].title").value("OH001 - Registered Patients"));
	}

	@Test
	void testPrint_aPeriodReportAsPdf() throws Exception {
		when(reportsManagerMock.getGenericReportFromDateToDatePdf(any(LocalDate.class), any(LocalDate.class), any(), any())).thenReturn(emptyReport());

		this.mockMvc.perform(get("/reports/statistics/{name}", "OH001_Patients").param("dateFrom", "2026-09-01").param("dateTo", "2026-09-30"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF));
		verify(reportsManagerMock).getGenericReportFromDateToDatePdf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "rpt_stat", "OH001_Patients");
	}

	@Test
	void testPrint_aMonthReportAsPdf() throws Exception {
		when(reportsManagerMock.getGenericReportMYPdf(anyInt(), anyInt(), any(), any())).thenReturn(emptyReport());

		this.mockMvc.perform(get("/reports/statistics/{name}", "HMIS108").param("month", "9").param("year", "2026"))
			.andExpect(status().isOk());
		verify(reportsManagerMock).getGenericReportMYPdf(9, 2026, "rpt_extra", "HMIS108");
	}

	@Test
	void testPrint_theParametersOfItsKind() throws Exception {
		this.mockMvc.perform(get("/reports/statistics/{name}", "OH001_Patients").param("month", "9").param("year", "2026"))
			.andExpect(status().isBadRequest());
		this.mockMvc.perform(get("/reports/statistics/{name}", "HMIS108").param("month", "13").param("year", "2026"))
			.andExpect(status().isBadRequest());
		verify(reportsManagerMock, never()).getGenericReportMYPdf(anyInt(), anyInt(), any(), any());
	}

	@Test
	void testPrint_onlyTheReportsFound() throws Exception {
		this.mockMvc.perform(get("/reports/statistics/{name}", "NoSuchReport").param("month", "9").param("year", "2026"))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(get("/reports/statistics/{name}", "OH001_Patients_subreport").param("month", "9").param("year", "2026"))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(get("/reports/statistics/{name}", "..%2Frpt_base%2Fexamslist").param("month", "9").param("year", "2026"))
			.andExpect(status().isNotFound());
		verify(reportsManagerMock, never()).getGenericReportMYPdf(anyInt(), anyInt(), any(), eq("examslist"));
	}
}
