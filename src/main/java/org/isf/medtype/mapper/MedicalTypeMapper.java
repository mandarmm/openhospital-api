/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2023 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.medtype.mapper;

import java.util.List;

import org.isf.medtype.dto.MedicalTypeDTO;
import org.isf.medtype.model.MedicalType;
import org.isf.shared.GenericMapper;
import org.springframework.stereotype.Component;

@Component
public class MedicalTypeMapper extends GenericMapper<MedicalType, MedicalTypeDTO> {
	public MedicalTypeMapper() {
		super(MedicalType.class, MedicalTypeDTO.class);
	}

	/*
	 * Mapped by hand: the model keeps the deleted flag as a char ('Y' / 'N'), the DTO as a Boolean.
	 */

	@Override
	public MedicalTypeDTO map2DTO(MedicalType medicalType) {
		MedicalTypeDTO dto = new MedicalTypeDTO(medicalType.getCode(), medicalType.getDescription());
		dto.setDeleted(medicalType.getDeleted() == 'Y');
		return dto;
	}

	/**
	 * A missing deleted flag maps to 'N'; callers updating an existing type keep its stored flag instead.
	 */
	@Override
	public MedicalType map2Model(MedicalTypeDTO dto) {
		MedicalType medicalType = new MedicalType(dto.getCode(), dto.getDescription());
		medicalType.setDeleted(Boolean.TRUE.equals(dto.getDeleted()) ? 'Y' : 'N');
		return medicalType;
	}

	@Override
	public List<MedicalTypeDTO> map2DTOList(List<MedicalType> list) {
		return list.stream().map(this::map2DTO).toList();
	}

	@Override
	public List<MedicalType> map2ModelList(List<MedicalTypeDTO> list) {
		return list.stream().map(this::map2Model).toList();
	}
}
