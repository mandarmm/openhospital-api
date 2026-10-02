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
package org.isf.medical.mapper;

import java.util.List;

import org.isf.medical.dto.MedicalDTO;
import org.isf.medicals.model.Medical;
import org.isf.medtype.mapper.MedicalTypeMapper;
import org.isf.shared.GenericMapper;
import org.springframework.stereotype.Component;

@Component
public class MedicalMapper extends GenericMapper<Medical, MedicalDTO> {

	private final MedicalTypeMapper medicalTypeMapper;

	public MedicalMapper(MedicalTypeMapper medicalTypeMapper) {
		super(Medical.class, MedicalDTO.class);
		this.medicalTypeMapper = medicalTypeMapper;
	}

	/*
	 * Mapped by hand: the product code is "prod_code" in the DTO and "prodCode" in the model, so the implicit mapping
	 * lost it (every medical was returned without its code, and creating or updating one failed with an error 500 in the
	 * validation). The type goes through its own mapper.
	 */

	@Override
	public MedicalDTO map2DTO(Medical medical) {
		MedicalDTO dto = new MedicalDTO(medical.getCode(), medical.getType() == null ? null : medicalTypeMapper.map2DTO(medical.getType()),
			medical.getProdCode(), medical.getDescription(), medical.getInitialqty(), medical.getPcsperpck(), medical.getMinqty(),
			medical.getInqty(), medical.getOutqty());
		dto.setLock(medical.getLock() == null ? 0 : medical.getLock());
		return dto;
	}

	/**
	 * A medical without product code gets an empty one, as in Swing. The initial quantity is not set: the model keeps
	 * it at 0.
	 */
	@Override
	public Medical map2Model(MedicalDTO dto) {
		Medical medical = new Medical(dto.getCode(), dto.getType() == null ? null : medicalTypeMapper.map2Model(dto.getType()),
			dto.getProd_code() == null ? "" : dto.getProd_code(), dto.getDescription(), dto.getPcsperpck(), dto.getMinqty(), dto.getInqty(),
			dto.getOutqty());
		medical.setLock(dto.getLock());
		return medical;
	}

	@Override
	public List<MedicalDTO> map2DTOList(List<Medical> list) {
		return list.stream().map(this::map2DTO).toList();
	}

	@Override
	public List<Medical> map2ModelList(List<MedicalDTO> list) {
		return list.stream().map(this::map2Model).toList();
	}
}
