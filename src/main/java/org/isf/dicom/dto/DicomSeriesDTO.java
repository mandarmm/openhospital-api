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

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A series of a patient's images, as the desktop client shows it: its first image (thumbnail and details) and the ids
 * of all its images, in order, to be read one by one.
 */
@Schema(description = "A series of a patient's images: its first image and the ids of all its images")
public record DicomSeriesDTO(DicomFileDTO first, List<Long> fileIds) {
}
