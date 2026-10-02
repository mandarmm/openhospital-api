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
package org.isf.medicalstock.mapper;

import java.util.List;

import org.isf.medical.mapper.MedicalMapper;
import org.isf.medicalstock.dto.MovementDTO;
import org.isf.medicalstock.model.Movement;
import org.isf.shared.GenericMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper extends GenericMapper<Movement, MovementDTO> {

	@Autowired
	private MedicalMapper medicalMapper;

	public MovementMapper() {
		super(Movement.class, MovementDTO.class);
	}

	/**
	 * The medical goes through its own mapper: implicitly mapped, it lost its product code.
	 */
	@Override
	public MovementDTO map2DTO(Movement movement) {
		MovementDTO dto = super.map2DTO(movement);
		if (movement.getMedical() != null) {
			dto.setMedical(medicalMapper.map2DTO(movement.getMedical()));
		}
		return dto;
	}

	@Override
	public List<MovementDTO> map2DTOList(List<Movement> list) {
		return list.stream().map(this::map2DTO).toList();
	}
}
