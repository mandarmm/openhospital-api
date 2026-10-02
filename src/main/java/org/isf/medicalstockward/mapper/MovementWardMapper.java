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

import org.isf.medical.dto.MedicalDTO;
import org.isf.medicals.model.Medical;
import org.isf.medicalstock.mapper.LotMapper;
import org.isf.medicalstockward.dto.MovementWardDTO;
import org.isf.medicalstockward.model.MovementWard;
import org.isf.patient.mapper.PatientMapper;
import org.isf.shared.GenericMapper;
import org.isf.ward.dto.WardDTO;
import org.isf.ward.model.Ward;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MovementWardMapper extends GenericMapper<MovementWard, MovementWardDTO> {

	@Autowired
	private LotMapper lotMapper;

	@Autowired
	private PatientMapper patientMapper;

	public MovementWardMapper() {
		super(MovementWard.class, MovementWardDTO.class);
	}

	/*
	 * Mapped by hand: the patient flag and the patient both have a "setPatient" (in the model and in the DTO), which
	 * the implicit mapping confuses, and the model's lot setter is "setlot", which it does not find; without a lot the
	 * ward stock cannot be updated.
	 */

	@Override
	public MovementWardDTO map2DTO(MovementWard movement) {
		MovementWardDTO dto = new MovementWardDTO(movement.getCode(), map(movement.getWard(), WardDTO.class), movement.getDate(),
			movement.isPatient(), movement.getPatient() == null ? null : patientMapper.map2DTO(movement.getPatient()), movement.getAge(),
			movement.getWeight(), movement.getDescription(), map(movement.getMedical(), MedicalDTO.class), movement.getQuantity(),
			movement.getUnits(), map(movement.getWardTo(), WardDTO.class), map(movement.getWardFrom(), WardDTO.class));
		dto.setLot(movement.getLot() == null ? null : lotMapper.map2DTO(movement.getLot()));
		return dto;
	}

	@Override
	public MovementWard map2Model(MovementWardDTO dto) {
		MovementWard movement = new MovementWard();
		movement.setCode(dto.getCode());
		movement.setWard(map(dto.getWard(), Ward.class));
		movement.setDate(dto.getDate());
		movement.setPatient(dto.isPatient());
		movement.setPatient(dto.getPatient() == null ? null : patientMapper.map2Model(dto.getPatient()));
		movement.setAge(dto.getAge());
		movement.setWeight(dto.getWeight());
		movement.setDescription(dto.getDescription());
		movement.setMedical(map(dto.getMedical(), Medical.class));
		movement.setQuantity(dto.getQuantity());
		movement.setUnits(dto.getUnits());
		movement.setWardTo(map(dto.getWardTo(), Ward.class));
		movement.setWardFrom(map(dto.getWardFrom(), Ward.class));
		movement.setlot(dto.getLot() == null ? null : lotMapper.map2Model(dto.getLot()));
		return movement;
	}

	private <T> T map(Object source, Class<T> type) {
		return source == null ? null : modelMapper.map(source, type);
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
