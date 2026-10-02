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
package org.isf.medicalstockward.mapper;

import java.util.List;

import org.isf.medicalstock.mapper.LotMapper;
import org.isf.medicalstockward.dto.MedicalWardDTO;
import org.isf.medicalstockward.model.MedicalWard;
import org.isf.shared.GenericMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MedicalWardMapper extends GenericMapper<MedicalWard, MedicalWardDTO> {

	@Autowired
	private LotMapper lotMapper;

	public MedicalWardMapper() {
		super(MedicalWard.class, MedicalWardDTO.class);
	}

	/**
	 * With the lot: the stock of a ward is kept per lot (and the lot gives the expiry date).
	 */
	@Override
	public MedicalWardDTO map2DTO(MedicalWard medicalWard) {
		MedicalWardDTO dto = super.map2DTO(medicalWard);
		if (dto.getId() != null) {
			dto.getId().setLot(medicalWard.getLot() == null ? null : lotMapper.map2DTO(medicalWard.getLot()));
		}
		return dto;
	}

	@Override
	public List<MedicalWardDTO> map2DTOList(List<MedicalWard> list) {
		return list.stream().map(this::map2DTO).toList();
	}
}
