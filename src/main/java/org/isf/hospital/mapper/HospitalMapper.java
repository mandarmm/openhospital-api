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
package org.isf.hospital.mapper;

import java.sql.Time;
import java.util.List;

import org.isf.hospital.dto.HospitalDTO;
import org.isf.hospital.model.Hospital;
import org.isf.shared.GenericMapper;
import org.springframework.stereotype.Component;

@Component
public class HospitalMapper extends GenericMapper<Hospital, HospitalDTO> {

    public HospitalMapper() {
        super(Hospital.class, HospitalDTO.class);
    }

	/*
	 * Mapped by hand: the model keeps the visiting hours as java.sql.Time, the DTO as LocalTime.
	 */

	@Override
	public HospitalDTO map2DTO(Hospital hospital) {
		HospitalDTO dto = new HospitalDTO();
		dto.setCode(hospital.getCode());
		dto.setDescription(hospital.getDescription());
		dto.setAddress(hospital.getAddress());
		dto.setCity(hospital.getCity());
		dto.setTelephone(hospital.getTelephone());
		dto.setFax(hospital.getFax());
		dto.setEmail(hospital.getEmail());
		dto.setCurrencyCod(hospital.getCurrencyCod());
		dto.setVisitStartTime(hospital.getVisitStartTime() != null ? hospital.getVisitStartTime().toLocalTime() : null);
		dto.setVisitEndTime(hospital.getVisitEndTime() != null ? hospital.getVisitEndTime().toLocalTime() : null);
		dto.setVisitIncrement(hospital.getVisitIncrement());
		dto.setVisitDuration(hospital.getVisitDuration());
		dto.setLock(hospital.getLock() != null ? hospital.getLock() : 0);
		return dto;
	}

	/**
	 * Visiting hours missing from the DTO get the model defaults; callers updating the hospital keep the stored
	 * values instead.
	 */
	@Override
	public Hospital map2Model(HospitalDTO dto) {
		Hospital hospital = new Hospital(dto.getCode(), dto.getDescription(), dto.getAddress(), dto.getCity(),
			dto.getTelephone(), dto.getFax(), dto.getEmail(), dto.getCurrencyCod());
		if (dto.getVisitStartTime() != null) {
			hospital.setVisitStartTime(Time.valueOf(dto.getVisitStartTime()));
		}
		if (dto.getVisitEndTime() != null) {
			hospital.setVisitEndTime(Time.valueOf(dto.getVisitEndTime()));
		}
		if (dto.getVisitIncrement() != null) {
			hospital.setVisitIncrement(dto.getVisitIncrement());
		}
		if (dto.getVisitDuration() != null) {
			hospital.setVisitDuration(dto.getVisitDuration());
		}
		hospital.setLock(dto.getLock());
		return hospital;
	}

	@Override
	public List<HospitalDTO> map2DTOList(List<Hospital> list) {
		return list.stream().map(this::map2DTO).toList();
	}

	@Override
	public List<Hospital> map2ModelList(List<HospitalDTO> list) {
		return list.stream().map(this::map2Model).toList();
	}
}