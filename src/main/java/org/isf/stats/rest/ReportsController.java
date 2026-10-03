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
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.poi.util.IOUtils;
import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicals.model.Medical;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.stat.dto.JasperReportResultDto;
import org.isf.stat.manager.JasperReportsManager;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.ward.manager.WardBrowserManager;
import org.isf.ward.model.Ward;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;

@RestController
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ReportsController {

	private static final MediaType EXCEL = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final JasperReportsManager reportsManager;

	private final WardBrowserManager wardManager;

	private final MedicalBrowsingManager medicalManager;

	public ReportsController(JasperReportsManager reportsManager, WardBrowserManager wardManager, MedicalBrowsingManager medicalManager) {
		this.reportsManager = reportsManager;
		this.wardManager = wardManager;
		this.medicalManager = medicalManager;
	}

	@GetMapping("/reports/exams-list")
	public ResponseEntity<byte[]> printExamsListPdf(HttpServletRequest request) throws OHServiceException, IOException {
		return getReport(reportsManager.getExamsListPdf(), request);
	}

	@GetMapping("/reports/diseases-list")
	public ResponseEntity<byte[]> printDiseasesListPdf(HttpServletRequest request) throws OHServiceException, IOException {
		return getReport(reportsManager.getDiseasesListPdf(), request);
	}

	/**
	 * The stock of a ward (Swing ward pharmacy's report {@code PharmaceuticalStockWard}), today or at the end of a day.
	 */
	@GetMapping(value = "/reports/ward-stock", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> printWardStockPdf(
		@RequestParam String wardCode,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) throws OHServiceException {
		Ward ward = findWard(wardCode);
		JasperReportResultDto result = reportsManager.getGenericReportPharmaceuticalStockWardPdf(
			date == null ? null : date.atTime(LocalTime.MAX), "PharmaceuticalStockWard", ward);
		return pdf(result, "PharmaceuticalStockWard_" + wardCode + ".pdf");
	}

	/**
	 * The stock card of a medical (Swing's Stock card, {@code ProductLedger}) or, without a medical, the stock ledger of
	 * all medicals ({@code ProductLedger_multi}): of the main store, or with {@code wardCode} of a ward
	 * ({@code ProductLedgerWard}, {@code ProductLedgerWard_multi}). As PDF, or with {@code format=excel} the report's
	 * rows as a spreadsheet.
	 */
	@GetMapping(value = "/reports/stock-card")
	public ResponseEntity<byte[]> printStockCard(
		@RequestParam(required = false) String wardCode,
		@RequestParam(required = false) Integer medicalCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
		@RequestParam(defaultValue = "pdf") String format
	) throws OHServiceException, IOException {
		if (dateTo.isBefore(dateFrom)) {
			throw new OHAPIException(new OHExceptionMessage("angal.opd.datefrommustbebefordateto.msg"));
		}
		Ward ward = wardCode == null || wardCode.isBlank() ? null : findWard(wardCode);
		Medical medical = null;
		if (medicalCode != null) {
			medical = medicalManager.getMedical(medicalCode);
			if (medical == null) {
				throw new OHAPIException(new OHExceptionMessage("Medical not found."), HttpStatus.NOT_FOUND);
			}
		}
		String jasperFileName = "ProductLedger" + (ward == null ? "" : "Ward") + (medical == null ? "_multi" : "");
		String name = jasperFileName + (ward == null ? "" : '_' + ward.getCode()) + (medical == null ? "" : "_" + medical.getCode());
		if ("excel".equals(format)) {
			// the core writes the spreadsheet to a file
			File file = Files.createTempFile("oh-report-", ".xlsx").toFile();
			try {
				reportsManager.getGenericReportPharmaceuticalStockCardExcel(jasperFileName, file.getAbsolutePath(), dateFrom.atStartOfDay(),
					dateTo.atTime(LocalTime.MAX), medical, ward);
				return ResponseEntity.ok()
					.contentType(EXCEL)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".xlsx\"")
					.body(Files.readAllBytes(file.toPath()));
			} finally {
				Files.deleteIfExists(file.toPath());
			}
		}
		if (!"pdf".equals(format)) {
			throw new OHAPIException(new OHExceptionMessage("The format must be pdf or excel."));
		}
		JasperReportResultDto result = reportsManager.getGenericReportPharmaceuticalStockCardPdf(jasperFileName, null, dateFrom.atStartOfDay(),
			dateTo.atTime(LocalTime.MAX), medical, ward);
		return pdf(result, name + ".pdf");
	}

	private Ward findWard(String wardCode) throws OHServiceException {
		Ward ward = wardManager.findWard(wardCode);
		if (ward == null) {
			throw new OHAPIException(new OHExceptionMessage("Ward not found."), HttpStatus.NOT_FOUND);
		}
		return ward;
	}

	/**
	 * The report as PDF, from the filled report rather than from the file the core also writes (its name is shared by
	 * all the requests for the report).
	 */
	private static ResponseEntity<byte[]> pdf(JasperReportResultDto result, String filename) throws OHAPIException {
		try {
			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + '"')
				.body(JasperExportManager.exportReportToPdf(result.getJasperPrint()));
		} catch (JRException e) {
			throw new OHAPIException(new OHExceptionMessage("angal.stat.reporterror.msg"));
		}
	}

	private ResponseEntity<byte[]> getReport(
		JasperReportResultDto resultDto, HttpServletRequest request
	) throws OHServiceException, IOException {
		Path report = Paths.get(resultDto.getFilename()).normalize();
		Resource resource;
		try {
			resource = new UrlResource(report.toUri());
			if (!resource.exists()) {
				throw new OHAPIException(new OHExceptionMessage("File not found."));
			}
		} catch (MalformedURLException e) {
			throw new OHAPIException(new OHExceptionMessage("File not found."));
		}

		String contentType;
		try {
			contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			throw new OHAPIException(new OHExceptionMessage("Failed to load the file's type."));
		}

		// Fallback to the default content type if type could not be determined
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		byte[] out = IOUtils.toByteArray(resource.getInputStream());

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(contentType))
			.header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + resource.getFilename() + '"')
			.body(out);
	}
}
