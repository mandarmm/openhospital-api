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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.isf.accounting.manager.BillBrowserManager;
import org.isf.accounting.model.Bill;
import org.isf.admission.manager.AdmissionBrowserManager;
import org.isf.admission.model.Admission;
import org.isf.examination.manager.ExaminationBrowserManager;
import org.isf.generaldata.GeneralData;
import org.isf.hospital.manager.HospitalBrowsingManager;
import org.isf.lab.manager.LabManager;
import org.isf.medicalinventory.manager.MedicalInventoryManager;
import org.isf.medtype.manager.MedicalTypeBrowserManager;
import org.isf.medtype.model.MedicalType;
import org.isf.opd.manager.OpdBrowserManager;
import org.isf.opd.model.Opd;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.priceslist.manager.PriceListManager;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.stat.dto.JasperReportResultDto;
import org.isf.stat.manager.JasperReportsManager;
import org.isf.ward.manager.WardBrowserManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.sf.jasperreports.engine.JasperPrint;

class ScreenReportsControllerTest {

	@Mock
	private JasperReportsManager reportsManagerMock;

	@Mock
	private OpdBrowserManager opdManagerMock;

	@Mock
	private AdmissionBrowserManager admissionManagerMock;

	@Mock
	private WardBrowserManager wardManagerMock;

	@Mock
	private MedicalTypeBrowserManager medicalTypeManagerMock;

	@Mock
	private MedicalInventoryManager inventoryManagerMock;

	@Mock
	private PatientBrowserManager patientManagerMock;

	@Mock
	private LabManager labManagerMock;

	@Mock
	private PriceListManager priceListManagerMock;

	@Mock
	private HospitalBrowsingManager hospitalManagerMock;

	@Mock
	private BillBrowserManager billManagerMock;

	@Mock
	private ExaminationBrowserManager examinationManagerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		GeneralData.OPDCHART = "patient_opd_chart";
		GeneralData.DISCHART = "patient_dis_chart";
		GeneralData.PHARMACEUTICALSTOCK = "PharmaceuticalStock_ver4";
		this.mockMvc = MockMvcBuilders
			.standaloneSetup(new ScreenReportsController(reportsManagerMock, opdManagerMock, admissionManagerMock, wardManagerMock,
				medicalTypeManagerMock, inventoryManagerMock, patientManagerMock, labManagerMock, priceListManagerMock, hospitalManagerMock,
				billManagerMock, examinationManagerMock))
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

	private static Patient patient(int code) {
		Patient patient = new Patient();
		patient.setCode(code);
		return patient;
	}

	@Test
	void testOpd_theReportOfTheVisitAndItsPatient() throws Exception {
		Opd opd = new Opd();
		opd.setCode(7);
		opd.setPatient(patient(301));
		when(opdManagerMock.getOpdById(7)).thenReturn(Optional.of(opd));
		when(reportsManagerMock.getGenericReportOpdPdf(7, 301, "patient_opd_chart")).thenReturn(emptyReport());

		this.mockMvc.perform(get("/reports/opd/{code}", 7))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF));

		when(opdManagerMock.getOpdById(8)).thenReturn(Optional.empty());
		this.mockMvc.perform(get("/reports/opd/{code}", 8)).andExpect(status().isNotFound());
	}

	@Test
	void testDischarge_onlyOfADischargedPatient() throws Exception {
		Admission admission = new Admission();
		admission.setId(5);
		admission.setPatient(patient(301));
		when(admissionManagerMock.getAdmission(5)).thenReturn(admission);

		this.mockMvc.perform(get("/reports/discharge/{id}", 5)).andExpect(status().isBadRequest());
		verify(reportsManagerMock, never()).getGenericReportDischargePdf(anyInt(), anyInt(), any());

		admission.setDisDate(LocalDateTime.of(2026, 10, 1, 10, 0));
		when(reportsManagerMock.getGenericReportDischargePdf(5, 301, "patient_dis_chart")).thenReturn(emptyReport());
		this.mockMvc.perform(get("/reports/discharge/{id}", 5)).andExpect(status().isOk());
	}

	@Test
	void testStock_theTypesDescriptionAndTheDefaultOrder() throws Exception {
		MedicalType type = new MedicalType("K", "Drugs");
		when(medicalTypeManagerMock.getMedicalType()).thenReturn(List.of(type));
		when(reportsManagerMock.getGenericReportPharmaceuticalStockPdf(any(), any(), any(), any(), any())).thenReturn(emptyReport());

		this.mockMvc.perform(get("/reports/pharmaceutical-stock").param("filter", "amox").param("medicalType", "K")
				.param("sortBy", "1; DROP TABLE OH_MEDICALDSR"))
			.andExpect(status().isOk());
		verify(reportsManagerMock).getGenericReportPharmaceuticalStockPdf(eq(null), eq("PharmaceuticalStock_ver4"), eq("%amox%"), eq("Drugs"),
			eq("MDSRT_DESC, MDSR_DESC"));

		this.mockMvc.perform(get("/reports/pharmaceutical-stock").param("medicalType", "ZZ")).andExpect(status().isNotFound());
	}

	@Test
	void testWardVisits_ofAKnownWard() throws Exception {
		this.mockMvc.perform(get("/reports/ward-visits").param("wardCode", "ZZ").param("date", "2026-10-01")).andExpect(status().isNotFound());
	}

	@Test
	void testPatient_theSectionsAmongSwingsOptions() throws Exception {
		GeneralData.PATIENTSHEET = "patient_clinical_sheet_ver3";
		when(patientManagerMock.getPatientById(301)).thenReturn(patient(301));
		when(reportsManagerMock.getGenericReportPatientVersion2Pdf(any(), any(), any(), any(), any())).thenReturn(emptyReport());

		this.mockMvc.perform(get("/reports/patient/{code}", 301).param("dateFrom", "2022-01-01").param("dateTo", "2022-12-31")
				.param("sections", "Opd", "Laboratory"))
			.andExpect(status().isOk());
		verify(reportsManagerMock).getGenericReportPatientVersion2Pdf(eq(301), eq("OpdLaboratory"), any(), any(), eq("patient_clinical_sheet_ver3"));

		this.mockMvc.perform(get("/reports/patient/{code}", 301).param("dateFrom", "2022-01-01").param("dateTo", "2022-12-31")
				.param("sections", "Opd", "Bills"))
			.andExpect(status().isBadRequest());
	}

	@AfterEach
	void logout() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void testBillReports_needTheBillsPermission() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("clerk", null,
				List.of(new SimpleGrantedAuthority("reports.read"))));
		this.mockMvc.perform(get("/reports/bill/{id}", 1)).andExpect(status().isForbidden());

		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("cashier", null,
				List.of(new SimpleGrantedAuthority("bills.read"))));
		when(billManagerMock.getBill(1)).thenReturn(new Bill());
		when(reportsManagerMock.getGenericReportBillPdf(eq(1), any(), eq(false), eq(false))).thenReturn(emptyReport());
		this.mockMvc.perform(get("/reports/bill/{id}", 1)).andExpect(status().isOk());
		this.mockMvc.perform(get("/reports/bill/{id}", 2)).andExpect(status().isNotFound());
	}
}
