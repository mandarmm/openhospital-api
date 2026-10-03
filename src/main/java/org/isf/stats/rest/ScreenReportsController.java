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

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.isf.admission.manager.AdmissionBrowserManager;
import org.isf.admission.model.Admission;
import org.isf.generaldata.GeneralData;
import org.isf.hospital.manager.HospitalBrowsingManager;
import org.isf.hospital.model.Hospital;
import org.isf.lab.manager.LabManager;
import org.isf.medicalinventory.manager.MedicalInventoryManager;
import org.isf.medicalinventory.model.MedicalInventory;
import org.isf.medtype.manager.MedicalTypeBrowserManager;
import org.isf.medtype.model.MedicalType;
import org.isf.opd.manager.OpdBrowserManager;
import org.isf.opd.model.Opd;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.priceslist.manager.PriceListManager;
import org.isf.priceslist.model.PriceList;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.stat.dto.JasperReportResultDto;
import org.isf.stat.manager.JasperReportsManager;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.ward.manager.WardBrowserManager;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 * The prints of the desktop client's screens, as PDF: a patient's OPD visit, admission and discharge (patient folder),
 * the visits of a ward on a day, the main store's stock, average monthly consumption, order list and expiring lots
 * (pharmaceuticals), an inventory, a patient's report (version 2), the lab exams and a price list. The report files are
 * the ones the installation's settings name.
 */
@RestController
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
public class ScreenReportsController {

	/** The order of the stock report, as the desktop client's default (by type, then medical). */
	private static final String STOCK_SORT = "MDSRT_DESC, MDSR_DESC";

	private final JasperReportsManager reportsManager;

	private final OpdBrowserManager opdManager;

	private final AdmissionBrowserManager admissionManager;

	private final WardBrowserManager wardManager;

	private final MedicalTypeBrowserManager medicalTypeManager;

	private final MedicalInventoryManager inventoryManager;

	private final PatientBrowserManager patientManager;

	private final LabManager labManager;

	private final PriceListManager priceListManager;

	private final HospitalBrowsingManager hospitalManager;

	/** The sections of the patient report (Swing's patient report options); {@code All} for all of them. */
	static final Set<String> PATIENT_SECTIONS = Set.of("All", "Drugs", "Examination", "Admission", "Opd", "Laboratory", "Operations");

	public ScreenReportsController(JasperReportsManager reportsManager, OpdBrowserManager opdManager, AdmissionBrowserManager admissionManager,
		WardBrowserManager wardManager, MedicalTypeBrowserManager medicalTypeManager, MedicalInventoryManager inventoryManager,
		PatientBrowserManager patientManager, LabManager labManager, PriceListManager priceListManager, HospitalBrowsingManager hospitalManager) {
		this.reportsManager = reportsManager;
		this.opdManager = opdManager;
		this.admissionManager = admissionManager;
		this.wardManager = wardManager;
		this.medicalTypeManager = medicalTypeManager;
		this.inventoryManager = inventoryManager;
		this.patientManager = patientManager;
		this.labManager = labManager;
		this.priceListManager = priceListManager;
		this.hospitalManager = hospitalManager;
	}

	/** The report of an OPD visit (Swing patient folder: OPD chart). */
	@GetMapping(value = "/reports/opd/{code}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printOpd(@PathVariable int code) throws OHServiceException {
		Opd opd = opdManager.getOpdById(code).orElseThrow(() -> notFound("OPD"));
		// without the extended OPD, a visit has no patient record
		int patientCode = opd.getPatient() == null ? 0 : opd.getPatient().getCode();
		return ReportsController.pdf(reportsManager.getGenericReportOpdPdf(code, patientCode, GeneralData.OPDCHART),
			"OPD_" + code + ".pdf");
	}

	/** The report of an admission (Swing patient folder: admission chart). */
	@GetMapping(value = "/reports/admission/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printAdmission(@PathVariable int id) throws OHServiceException {
		Admission admission = findAdmission(id);
		return ReportsController.pdf(reportsManager.getGenericReportAdmissionPdf(id, admission.getPatient().getCode(), GeneralData.ADMCHART),
			"Admission_" + id + ".pdf");
	}

	/** The discharge report of an admission (Swing patient folder: discharge chart); the admission must be closed. */
	@GetMapping(value = "/reports/discharge/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printDischarge(@PathVariable int id) throws OHServiceException {
		Admission admission = findAdmission(id);
		if (admission.getDisDate() == null) {
			throw new OHAPIException(new OHExceptionMessage("The patient is not discharged."));
		}
		return ReportsController.pdf(reportsManager.getGenericReportDischargePdf(id, admission.getPatient().getCode(), GeneralData.DISCHART),
			"Discharge_" + id + ".pdf");
	}

	/** The visits of a ward on a day (Swing ward visits: sheet of the day). */
	@GetMapping(value = "/reports/ward-visits", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printWardVisits(
		@RequestParam String wardCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) throws OHServiceException {
		if (wardManager.findWard(wardCode) == null) {
			throw notFound("Ward");
		}
		return ReportsController.pdf(reportsManager.getGenericReportWardVisitPdf(wardCode, date.atStartOfDay(), GeneralData.VISITSHEET),
			"WardVisits_" + wardCode + "_" + date + ".pdf");
	}

	/**
	 * The main store's stock today or at the end of a day (Swing pharmaceuticals: stock report), optionally with its lots,
	 * of the medicals whose description contains a text, of a medical type.
	 *
	 * @param medicalType the code of a medical type, or all
	 */
	@GetMapping(value = "/reports/pharmaceutical-stock", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printPharmaceuticalStock(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		@RequestParam(defaultValue = "false") boolean lots,
		@RequestParam(required = false) String filter,
		@RequestParam(required = false) String medicalType
	) throws OHServiceException {
		// the type is a parameter of the report's query: its description
		String groupBy = "%%";
		if (medicalType != null && !medicalType.isBlank()) {
			MedicalType type = medicalTypeManager.getMedicalType().stream()
				.filter(candidate -> candidate.getCode().equals(medicalType))
				.findFirst()
				.orElseThrow(() -> notFound("Medical type"));
			groupBy = type.getDescription();
		}
		// the order is pasted into the report's query: never from the client
		return ReportsController.pdf(reportsManager.getGenericReportPharmaceuticalStockPdf(date == null ? null : date.atTime(LocalTime.MAX),
				lots ? GeneralData.PHARMACEUTICALSTOCKLOT : GeneralData.PHARMACEUTICALSTOCK, '%' + (filter == null ? "" : filter.trim()) + '%', groupBy,
				STOCK_SORT),
			"PharmaceuticalStock" + (date == null ? "" : "_" + date) + ".pdf");
	}

	/** The average monthly consumption of the medicals, today or at the end of a day (Swing pharmaceuticals: AMC). */
	@GetMapping(value = "/reports/pharmaceutical-amc", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printPharmaceuticalAmc(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) throws OHServiceException {
		return ReportsController.pdf(reportsManager.GenericReportPharmaceuticalAMCPdf(date == null ? null : date.atTime(LocalTime.MAX),
			GeneralData.PHARMACEUTICALAMC), "PharmaceuticalAMC" + (date == null ? "" : "_" + date) + ".pdf");
	}

	/** The medicals to order: under their critical level (Swing pharmaceuticals: order list). */
	@GetMapping(value = "/reports/pharmaceutical-order", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printPharmaceuticalOrder() throws OHServiceException {
		return ReportsController.pdf(reportsManager.getGenericReportPharmaceuticalOrderPdf(GeneralData.PHARMACEUTICALORDER), "PharmaceuticalOrder.pdf");
	}

	/** The lots that expire between two days (Swing pharmaceuticals: expiring report). */
	@GetMapping(value = "/reports/pharmaceutical-expiration", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printPharmaceuticalExpiration(
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
	) throws OHServiceException {
		if (dateTo.isBefore(dateFrom)) {
			throw new OHAPIException(new OHExceptionMessage("angal.opd.datefrommustbebefordateto.msg"));
		}
		return ReportsController.pdf(reportsManager.getGenericReportFromDateToDatePdf(dateFrom, dateTo, "rpt_base", "PharmaceuticalExpiration"),
			"PharmaceuticalExpiration_" + dateFrom + "_" + dateTo + ".pdf");
	}

	/**
	 * An inventory, of the main store or a ward (Swing inventory: print), with or without the counted quantities.
	 */
	@GetMapping(value = "/reports/inventory/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printInventory(@PathVariable int id, @RequestParam(defaultValue = "true") boolean realQty) throws OHServiceException {
		MedicalInventory inventory = inventoryManager.getInventoryById(id);
		if (inventory == null) {
			throw notFound("Inventory");
		}
		return ReportsController.pdf(reportsManager.getInventoryReportPdf(inventory, "Inventory", realQty ? 1 : 0), "Inventory_" + id + ".pdf");
	}

	/**
	 * A patient's report of a period (Swing patient folder: patient report, version 2), with the chosen sections.
	 *
	 * @param sections {@code All} (default) or some of {@code Drugs}, {@code Examination}, {@code Admission}, {@code Opd},
	 *            {@code Laboratory}, {@code Operations}
	 */
	@GetMapping(value = "/reports/patient/{code}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printPatient(
		@PathVariable int code,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
		@RequestParam(defaultValue = "All") List<String> sections
	) throws OHServiceException {
		if (patientManager.getPatientById(code) == null) {
			throw notFound("Patient");
		}
		if (dateTo.isBefore(dateFrom)) {
			throw new OHAPIException(new OHExceptionMessage("angal.opd.datefrommustbebefordateto.msg"));
		}
		if (sections.isEmpty() || !PATIENT_SECTIONS.containsAll(sections)) {
			throw new OHAPIException(new OHExceptionMessage("The sections must be among " + PATIENT_SECTIONS + "."));
		}
		return ReportsController.pdf(reportsManager.getGenericReportPatientVersion2Pdf(code, String.join("", sections), dateFrom.atStartOfDay(),
			dateTo.atStartOfDay(), GeneralData.PATIENTSHEET), "Patient_" + code + "_" + dateFrom + "_" + dateTo + ".pdf");
	}

	/**
	 * The lab exams of a period (Swing laboratory: print the table), optionally of an exam (its description) or of a
	 * patient.
	 */
	@GetMapping(value = "/reports/laboratory", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printLaboratory(
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
		@RequestParam(required = false) String exam,
		@RequestParam(required = false) Integer patientCode
	) throws OHServiceException {
		Patient patient = null;
		if (patientCode != null) {
			patient = patientManager.getPatientById(patientCode);
			if (patient == null) {
				throw notFound("Patient");
			}
		}
		List<?> labs = labManager.getLaboratoryForPrint(exam == null || exam.isBlank() ? null : exam, dateFrom.atStartOfDay(), dateTo.atTime(LocalTime.MAX),
			patient);
		return ReportsController.pdf(fill("Laboratory", labs), "Laboratory_" + dateFrom + "_" + dateTo + ".pdf");
	}

	/** The prices of a list (Swing price lists: print). */
	@GetMapping(value = "/reports/pricelist/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printPriceList(@PathVariable int id) throws OHServiceException {
		PriceList list = priceListManager.getLists().stream().filter(candidate -> candidate.getId() == id).findFirst()
			.orElseThrow(() -> notFound("Price list"));
		return ReportsController.pdf(fill("PriceList", priceListManager.convertPrice(list, priceListManager.getPrices())),
			"PriceList_" + list.getCode() + ".pdf");
	}

	/**
	 * A report filled with rows the core prepared, as the desktop client's {@code PrintManager} does (which shows it in
	 * a viewer on the computer instead).
	 */
	private JasperReportResultDto fill(String jasperFileName, List<?> rows) throws OHServiceException {
		Hospital hospital = hospitalManager.getHospital();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("ospedaleNome", hospital.getDescription());
		parameters.put("ospedaleIndirizzo", hospital.getAddress());
		parameters.put("ospedaleCitta", hospital.getCity());
		parameters.put("ospedaleTel", hospital.getTelephone());
		parameters.put("ospedaleFax", hospital.getFax());
		parameters.put("ospedaleMail", hospital.getEmail());
		try {
			JasperReport report = (JasperReport) JRLoader.loadObject(new File("rpt_base/" + jasperFileName + ".jasper"));
			return new JasperReportResultDto(JasperFillManager.fillReport(report, parameters, new JRBeanCollectionDataSource(rows)), jasperFileName, null);
		} catch (JRException e) {
			throw new OHAPIException(new OHExceptionMessage("angal.stat.reporterror.msg"));
		}
	}

	private Admission findAdmission(int id) throws OHServiceException {
		Admission admission = admissionManager.getAdmission(id);
		if (admission == null) {
			throw notFound("Admission");
		}
		return admission;
	}

	private static OHAPIException notFound(String what) {
		return new OHAPIException(new OHExceptionMessage(what + " not found."), HttpStatus.NOT_FOUND);
	}
}
