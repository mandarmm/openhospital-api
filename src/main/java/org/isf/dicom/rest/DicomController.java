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
package org.isf.dicom.rest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.isf.dicom.dto.DicomFileDTO;
import org.isf.dicom.dto.DicomSeriesDTO;
import org.isf.dicom.manager.DicomManagerFactory;
import org.isf.dicom.manager.DicomManagerInterface;
import org.isf.dicom.manager.SourceFiles;
import org.isf.dicom.model.FileDicom;
import org.isf.dicomtype.dto.DicomTypeDTO;
import org.isf.dicomtype.manager.DicomTypeBrowserManager;
import org.isf.dicomtype.model.DicomType;
import org.isf.generaldata.MessageBundle;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * A patient's images (the desktop client's DICOM viewer, {@code DicomGui}): DICOM or JPEG files in series, stored as
 * {@code dicom.properties} says (file system or database), with the core's loader and its size limit.
 */
@RestController
@Tag(name = "DICOM")
@SecurityRequirement(name = "bearerAuth")
public class DicomController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DicomController.class);

	private final PatientBrowserManager patientManager;
	private final DicomTypeBrowserManager dicomTypeManager;

	public DicomController(PatientBrowserManager patientManager, DicomTypeBrowserManager dicomTypeManager) {
		this.patientManager = patientManager;
		this.dicomTypeManager = dicomTypeManager;
	}

	/** The patient's series of images, newest first: each with its first image and the ids of all its images. */
	@GetMapping(value = "/dicom/patients/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<DicomSeriesDTO> getPatientSeries(@PathVariable int code) throws OHServiceException {
		LOGGER.info("Get the images of patient {}.", code);
		checkPatient(code);
		// the core gives the first image of each series
		FileDicom[] firsts = manager().loadPatientFiles(code);
		List<DicomSeriesDTO> series = new ArrayList<>();
		for (FileDicom first : Arrays.stream(firsts == null ? new FileDicom[0] : firsts)
			.sorted(Comparator.comparing((FileDicom file) -> date(file), Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(FileDicom::getDicomSeriesNumber, Comparator.nullsLast(Comparator.naturalOrder())))
			.toList()) {
			Long[] ids = manager().getSeriesDetail(code, first.getDicomSeriesNumber());
			series.add(new DicomSeriesDTO(toDTO(first), ids == null ? List.of(first.getIdFile()) : List.of(ids)));
		}
		return series;
	}

	/** The image's thumbnail (JPEG, 100 pixels wide). */
	@GetMapping(value = "/dicom/patients/{code}/series/{series}/files/{id}/thumbnail", produces = MediaType.IMAGE_JPEG_VALUE)
	public byte[] getThumbnail(@PathVariable int code, @PathVariable String series, @PathVariable long id) throws OHServiceException {
		FileDicom file = load(code, series, id);
		return bytes(file.getDicomThumbnail());
	}

	/** The image: a JPEG as it was loaded, a DICOM file's first frame as PNG. */
	@GetMapping(value = "/dicom/patients/{code}/series/{series}/files/{id}/image", produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE })
	public ResponseEntity<byte[]> getImage(@PathVariable int code, @PathVariable String series, @PathVariable long id) throws OHServiceException {
		FileDicom file = load(code, series, id);
		byte[] data = bytes(file.getDicomData() == null ? null : file.getDicomData().getData());
		if (isJpeg(file.getFileName())) {
			return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(data);
		}
		return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(dicomToPng(data, file.getFileName()));
	}

	/**
	 * Loads a DICOM ({@code .dcm}) or JPEG file for the patient, as the desktop client does: its type, date and
	 * description; into an existing series of the patient ({@code series}) or a new one.
	 *
	 * @return the image loaded (its series, for the next files of the same series)
	 */
	@PostMapping(value = "/dicom/patients/{code}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public DicomFileDTO upload(@PathVariable int code, @RequestPart("file") MultipartFile upload,
		@Nullable @RequestParam(required = false) String type,
		@Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
		@Nullable @RequestParam(required = false) String description,
		@Nullable @RequestParam(required = false) String series) throws Exception {
		LOGGER.info("Load an image for patient {}.", code);
		checkPatient(code);
		// only the name, never a path
		String fileName = Path.of(upload.getOriginalFilename() == null ? "" : upload.getOriginalFilename()).getFileName().toString();
		if (!isJpeg(fileName) && !fileName.toLowerCase(Locale.ROOT).endsWith(".dcm")) {
			throw new OHAPIException(new OHExceptionMessage(MessageBundle.formatMessage("angal.dicom.dicomformatnotsupported.fmt.msg", fileName)));
		}
		// the manager reads dicom.properties, and with it the size limit
		manager();
		if (upload.getSize() > DicomManagerFactory.getMaxDicomSizeLong()) {
			throw new OHAPIException(new OHExceptionMessage(
				MessageBundle.formatMessage("angal.dicom.thefileistoobigpleasesetdicommaxsizeproperty.fmt.msg", DicomManagerFactory.getMaxDicomSize())));
		}
		Path folder = Files.createTempDirectory("oh-dicom");
		File file = folder.resolve(fileName).toFile();
		try {
			upload.transferTo(file);
			FileDicom detail = SourceFiles.preLoadDicom(file, 1);
			if (description != null) {
				detail.setDicomSeriesDescription(description);
			}
			if (date != null) {
				detail.setDicomSeriesDate(date);
				detail.setDicomStudyDate(date);
			}
			if (type != null && !type.isBlank()) {
				detail.setDicomType(dicomTypeManager.getDicomType().stream()
					.filter(dicomType -> dicomType.getDicomTypeID().equals(type))
					.findFirst()
					.orElseThrow(() -> new OHAPIException(new OHExceptionMessage("DICOM type not found."), HttpStatus.NOT_FOUND)));
			}
			if (series != null && !series.isBlank()) {
				intoSeries(detail, code, series);
			}
			SourceFiles.loadDicom(detail, file, code);
			return toDTO(detail);
		} finally {
			Files.deleteIfExists(file.toPath());
			Files.deleteIfExists(folder);
		}
	}

	/** Deletes a series of the patient's images. It cannot be undone. */
	@DeleteMapping("/dicom/patients/{code}/series/{series}")
	public boolean deleteSeries(@PathVariable int code, @PathVariable String series) throws OHServiceException {
		LOGGER.info("Delete the image series {} of patient {}.", series, code);
		checkPatient(code);
		if (!manager().exist(code, series)) {
			throw new OHAPIException(new OHExceptionMessage("Series not found."), HttpStatus.NOT_FOUND);
		}
		manager().deleteSeries(code, series);
		return true;
	}

	/** As the desktop client: the identifiers the viewer groups and looks up by, from the chosen series. */
	private void intoSeries(FileDicom detail, int code, String series) throws OHServiceException {
		// the core gives the first image of each series
		FileDicom[] files = manager().loadPatientFiles(code);
		FileDicom target = Arrays.stream(files == null ? new FileDicom[0] : files)
			.filter(file -> series.equals(file.getDicomSeriesNumber()))
			.findFirst()
			.orElseThrow(() -> new OHAPIException(new OHExceptionMessage("Series not found."), HttpStatus.NOT_FOUND));
		detail.setDicomSeriesNumber(target.getDicomSeriesNumber());
		detail.setDicomSeriesInstanceUID(target.getDicomSeriesInstanceUID());
		detail.setDicomSeriesUID(target.getDicomSeriesUID());
		detail.setDicomSeriesDescription(target.getDicomSeriesDescription());
	}

	private FileDicom load(int code, String series, long id) throws OHServiceException {
		checkPatient(code);
		FileDicom file = manager().loadDetails(id, code, series);
		if (file == null) {
			throw new OHAPIException(new OHExceptionMessage("Image not found."), HttpStatus.NOT_FOUND);
		}
		return file;
	}

	private void checkPatient(int code) throws OHServiceException {
		if (patientManager.getPatientById(code) == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}
	}

	private static DicomManagerInterface manager() throws OHServiceException {
		return DicomManagerFactory.getManager();
	}

	private static LocalDateTime date(FileDicom file) {
		return file.getDicomSeriesDate() != null ? file.getDicomSeriesDate() : file.getDicomStudyDate();
	}

	private static boolean isJpeg(String fileName) {
		String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
		return name.endsWith(".jpg") || name.endsWith(".jpeg");
	}

	private static byte[] bytes(@Nullable Blob blob) throws OHServiceException {
		if (blob == null) {
			throw new OHAPIException(new OHExceptionMessage("Image not found."), HttpStatus.NOT_FOUND);
		}
		try (InputStream in = blob.getBinaryStream()) {
			return in.readAllBytes();
		} catch (IOException | SQLException e) {
			LOGGER.error("Cannot read the image.", e);
			throw new OHAPIException(new OHExceptionMessage("Cannot read the image."));
		}
	}

	/** The first frame, as the desktop client's viewer reads it. */
	private static byte[] dicomToPng(byte[] data, String fileName) throws OHServiceException {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("DICOM");
		if (!readers.hasNext()) {
			throw new OHAPIException(new OHExceptionMessage("No DICOM reader."));
		}
		ImageReader reader = readers.next();
		try (ImageInputStream in = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(data))) {
			reader.setInput(in, false);
			BufferedImage image = reader.read(0, reader.getDefaultReadParam());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Cannot read the DICOM image.", e);
			throw new OHAPIException(new OHExceptionMessage(MessageBundle.formatMessage("angal.dicom.thefileisnotindicomformat.fmt.msg", fileName)));
		} finally {
			reader.dispose();
		}
	}

	private static DicomFileDTO toDTO(FileDicom file) {
		DicomType type = file.getDicomType();
		return new DicomFileDTO(file.getIdFile(), file.getPatId(), file.getFileName(), file.getDicomSeriesNumber(), file.getDicomSeriesInstanceUID(),
			file.getDicomSeriesDescription(), file.getDicomSeriesDate(), file.getDicomStudyDate(), file.getDicomStudyDescription(), file.getModality(),
			file.getDicomInstitutionName(), type == null ? null : new DicomTypeDTO(type.getDicomTypeID(), type.getDicomTypeDescription()));
	}
}
