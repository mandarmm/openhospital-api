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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.isf.generaldata.GeneralData;
import org.isf.generaldata.MessageBundle;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.stat.dto.JasperReportResultDto;
import org.isf.stat.manager.JasperReportsManager;
import org.isf.stats.dto.StatisticsReportDTO;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 * The statistics reports of the installation, as the desktop client's Reports menu ({@code ReportLauncher}) has them:
 * every Jasper report in {@code rpt_stat} (OH's statistics) and {@code rpt_extra} (the country's forms) that has a
 * title. A report asks for a period or for a month and year, depending on its parameters, and is printed as PDF or as a
 * spreadsheet. Only the reports found there can be run.
 */
@RestController
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsReportsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsReportsController.class);

	static final List<String> FOLDERS = List.of("rpt_stat", "rpt_extra");

	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_\\-]+");

	private static final Pattern LANGUAGE = Pattern.compile("[A-Za-z]{2,3}(_[A-Za-z]{2})?");

	private static final MediaType EXCEL = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final JasperReportsManager reportsManager;

	private final Path baseFolder;

	@Autowired
	public StatisticsReportsController(JasperReportsManager reportsManager) {
		this(reportsManager, Path.of("."));
	}

	StatisticsReportsController(JasperReportsManager reportsManager, Path baseFolder) {
		this.reportsManager = reportsManager;
		this.baseFolder = baseFolder;
	}

	/**
	 * @param language the language of the titles (e.g. {@code fr}); by default the hospital's
	 * @return the reports, by title
	 */
	@GetMapping(value = "/reports/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<StatisticsReportDTO> getStatisticsReports(@RequestParam(required = false) String language) {
		// part of a file name: a language code only
		String lang = language != null && LANGUAGE.matcher(language).matches() ? language : GeneralData.LANGUAGE;
		List<StatisticsReportDTO> reports = new ArrayList<>();
		for (String folder : FOLDERS) {
			Path dir = baseFolder.resolve(folder);
			if (!Files.isDirectory(dir)) {
				continue;
			}
			try (Stream<Path> files = Files.list(dir)) {
				files.filter(file -> file.getFileName().toString().endsWith(".jasper"))
					.forEach(file -> describe(folder, file, lang).ifPresent(reports::add));
			} catch (IOException e) {
				LOGGER.error("Cannot list the reports in {}.", dir, e);
			}
		}
		reports.sort(Comparator.comparing(StatisticsReportDTO::title, String.CASE_INSENSITIVE_ORDER));
		return reports;
	}

	/**
	 * Prints a report: of a period ({@code dateFrom}, {@code dateTo}) or of a month ({@code month}, {@code year}), as
	 * its kind says.
	 *
	 * @param format {@code pdf} or {@code excel}
	 */
	@GetMapping("/reports/statistics/{name}")
	public ResponseEntity<byte[]> printStatisticsReport(
		@PathVariable String name,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
		@RequestParam(required = false) Integer month,
		@RequestParam(required = false) Integer year,
		@RequestParam(defaultValue = "pdf") String format
	) throws OHServiceException, IOException {
		StatisticsReportDTO report = find(name);
		boolean excel = "excel".equals(format);
		if (!excel && !"pdf".equals(format)) {
			throw new OHAPIException(new OHExceptionMessage("The format must be pdf or excel."));
		}
		if (StatisticsReportDTO.PERIOD.equals(report.kind())) {
			if (dateFrom == null || dateTo == null) {
				throw new OHAPIException(new OHExceptionMessage("The report needs dateFrom and dateTo."));
			}
			if (dateTo.isBefore(dateFrom)) {
				throw new OHAPIException(new OHExceptionMessage("angal.opd.datefrommustbebefordateto.msg"));
			}
		} else if (month == null || year == null || month < 1 || month > 12) {
			throw new OHAPIException(new OHExceptionMessage("The report needs a month (1-12) and a year."));
		}
		boolean period = StatisticsReportDTO.PERIOD.equals(report.kind());
		String filename = report.name() + (period ? "_" + dateFrom + "_" + dateTo : "_" + year + "-" + month);
		if (excel) {
			// the core writes the spreadsheet to a file
			File file = Files.createTempFile("oh-report-", ".xlsx").toFile();
			try {
				if (period) {
					reportsManager.getGenericReportFromDateToDateExcel(dateFrom, dateTo, report.folder(), report.name(), file.getAbsolutePath());
				} else {
					reportsManager.getGenericReportMYExcel(month, year, report.folder(), report.name(), file.getAbsolutePath());
				}
				return ResponseEntity.ok()
					.contentType(EXCEL)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".xlsx\"")
					.body(Files.readAllBytes(file.toPath()));
			} finally {
				Files.deleteIfExists(file.toPath());
			}
		}
		JasperReportResultDto result = period
			? reportsManager.getGenericReportFromDateToDatePdf(dateFrom, dateTo, report.folder(), report.name())
			: reportsManager.getGenericReportMYPdf(month, year, report.folder(), report.name());
		try {
			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + ".pdf\"")
				.body(JasperExportManager.exportReportToPdf(result.getJasperPrint()));
		} catch (JRException e) {
			throw new OHAPIException(new OHExceptionMessage("angal.stat.reporterror.msg"));
		}
	}

	/** A report found in the folders; never a path given by the client. */
	private StatisticsReportDTO find(String name) throws OHAPIException {
		if (!NAME.matcher(name).matches()) {
			throw new OHAPIException(new OHExceptionMessage("Report not found."), HttpStatus.NOT_FOUND);
		}
		for (String folder : FOLDERS) {
			Path file = baseFolder.resolve(folder).resolve(name + ".jasper");
			if (Files.isRegularFile(file)) {
				Optional<StatisticsReportDTO> report = describe(folder, file, GeneralData.LANGUAGE);
				if (report.isPresent()) {
					return report.get();
				}
			}
		}
		throw new OHAPIException(new OHExceptionMessage("Report not found."), HttpStatus.NOT_FOUND);
	}

	/**
	 * The report, if it has a title (sub-reports have none): the title of the language's properties
	 * ({@code <folder>/<language>/<name>.properties}), or else of the default ones; the kind from its parameters.
	 */
	private Optional<StatisticsReportDTO> describe(String folder, Path file, String language) {
		String name = file.getFileName().toString().replace(".jasper", "");
		Path dir = file.getParent();
		String title = title(dir.resolve(language).resolve(name + ".properties"));
		if (title == null) {
			title = title(dir.resolve(name + ".properties"));
		}
		if (title == null) {
			return Optional.empty();
		}
		try {
			JasperReport report = (JasperReport) JRLoader.loadObject(file.toFile());
			List<String> parameters = Stream.of(report.getParameters())
				.filter(parameter -> !parameter.isSystemDefined() && parameter.isForPrompting())
				.map(JRParameter::getName)
				.toList();
			// as the desktop client: from / to dates, or else month and year
			String kind = parameters.contains("fromdate") || parameters.contains("todate") ? StatisticsReportDTO.PERIOD : StatisticsReportDTO.MONTH;
			return Optional.of(new StatisticsReportDTO(name, folder, title, kind, parameters));
		} catch (JRException e) {
			LOGGER.error("Cannot read the report {}.", file, e);
			return Optional.empty();
		}
	}

	private static String title(Path properties) {
		if (!Files.isRegularFile(properties)) {
			return null;
		}
		Properties props = MessageBundle.loadPropertiesFileUtf8(properties, LOGGER);
		String title = props == null ? null : props.getProperty("jTitle");
		return title == null || title.isBlank() ? null : title.trim();
	}
}
