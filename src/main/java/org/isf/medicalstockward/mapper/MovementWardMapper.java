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
import org.isf.medicalstockward.dto.MovementWardDTO;
import org.isf.medicalstockward.model.MovementWard;
import org.isf.shared.GenericMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MovementWardMapper extends GenericMapper<MovementWard, MovementWardDTO> {

	@Autowired
	private LotMapper lotMapper;

	public MovementWardMapper() {
		super(MovementWard.class, MovementWardDTO.class);
	}

	/*
	 * The lot is mapped by hand: the model's setter is "setlot", which the implicit mapping does not find, and without
	 * a lot the ward stock cannot be updated.
	 */

	@Override
	public MovementWardDTO map2DTO(MovementWard movement) {
		MovementWardDTO dto = super.map2DTO(movement);
		dto.setLot(movement.getLot() == null ? null : lotMapper.map2DTO(movement.getLot()));
		return dto;
	}

	@Override
	public MovementWard map2Model(MovementWardDTO dto) {
		MovementWard movement = super.map2Model(dto);
		movement.setlot(dto.getLot() == null ? null : lotMapper.map2Model(dto.getLot()));
		return movement;
	}

	@Override
	public List<MovementWardDTO> map2DTOList(List<MovementWard> list) {
		return list.stream().map(this::map2DTO).toList();
	}

	@Override
	public List<MovementWard> map2ModelList(List<MovementWardDTO> list) {
		return list.stream().map(this::map2Model).toList();
	}
}
