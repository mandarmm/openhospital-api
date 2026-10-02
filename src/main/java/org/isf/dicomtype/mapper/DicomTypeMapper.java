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
package org.isf.dicomtype.mapper;

import java.util.List;

import org.isf.dicomtype.dto.DicomTypeDTO;
import org.isf.dicomtype.model.DicomType;
import org.springframework.stereotype.Component;

/**
 * Maps DICOM types; the model names its fields dicomTypeID / dicomTypeDescription, the DTO code / description.
 */
@Component
public class DicomTypeMapper {

	public DicomTypeDTO map2DTO(DicomType dicomType) {
		return new DicomTypeDTO(dicomType.getDicomTypeID(), dicomType.getDicomTypeDescription());
	}

	public DicomType map2Model(DicomTypeDTO dto) {
		return new DicomType(dto.code(), dto.description());
	}

	public List<DicomTypeDTO> map2DTOList(List<DicomType> list) {
		return list.stream().map(this::map2DTO).toList();
	}
}
