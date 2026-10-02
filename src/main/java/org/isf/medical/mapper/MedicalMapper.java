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
import org.isf.shared.GenericMapper;
import org.springframework.stereotype.Component;

@Component
public class MedicalMapper extends GenericMapper<Medical, MedicalDTO> {

	public MedicalMapper() {
		super(Medical.class, MedicalDTO.class);
	}

	/*
	 * The product code is "prod_code" in the DTO and "prodCode" in the model, so the implicit mapping lost it: every
	 * medical was returned without its code, and creating or updating one failed with an error 500 in the validation.
	 */

	@Override
	public MedicalDTO map2DTO(Medical medical) {
		MedicalDTO dto = super.map2DTO(medical);
		dto.setProd_code(medical.getProdCode());
		return dto;
	}

	@Override
	public Medical map2Model(MedicalDTO dto) {
		Medical medical = super.map2Model(dto);
		medical.setProdCode(dto.getProd_code() == null ? "" : dto.getProd_code());
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
