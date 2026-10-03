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
package org.isf.dicom.dto;

import java.time.LocalDateTime;

import org.isf.dicomtype.dto.DicomTypeDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A patient's image (DICOM or JPEG file), without its data: the image and its thumbnail are read separately. Images
 * are grouped in series ({@code seriesNumber}).
 */
@Schema(description = "A patient's image (DICOM or JPEG), without its data")
public record DicomFileDTO(
	long id,
	int patientCode,
	String fileName,
	String seriesNumber,
	String seriesInstanceUid,
	String seriesDescription,
	LocalDateTime seriesDate,
	LocalDateTime studyDate,
	String studyDescription,
	String modality,
	String institutionName,
	DicomTypeDTO type,
	String studyId,
	String dicomPatientName,
	String dicomPatientSex,
	String dicomPatientAge) {
}
