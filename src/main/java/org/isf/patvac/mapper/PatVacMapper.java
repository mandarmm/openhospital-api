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
package org.isf.patvac.mapper;

import java.util.List;

import org.isf.patient.mapper.PatientMapper;
import org.isf.patvac.dto.PatientVaccineDTO;
import org.isf.patvac.model.PatientVaccine;
import org.isf.shared.GenericMapper;
import org.isf.vaccine.mapper.VaccineMapper;
import org.springframework.stereotype.Component;

@Component
public class PatVacMapper extends GenericMapper<PatientVaccine, PatientVaccineDTO> {

	private final PatientMapper patientMapper;

	private final VaccineMapper vaccineMapper;

	public PatVacMapper(PatientMapper patientMapper, VaccineMapper vaccineMapper) {
		super(PatientVaccine.class, PatientVaccineDTO.class);
		this.patientMapper = patientMapper;
		this.vaccineMapper = vaccineMapper;
	}

	/*
	 * Mapped by hand: the implicit mapping read "vaccineDate" as the vaccine's date and failed (error 500), so no
	 * vaccination could be created or updated.
	 */

	@Override
	public PatientVaccineDTO map2DTO(PatientVaccine patVac) {
		PatientVaccineDTO dto = new PatientVaccineDTO();
		dto.setCode(patVac.getCode());
		dto.setProgr(patVac.getProgr());
		dto.setVaccineDate(patVac.getVaccineDate());
		dto.setPatient(patVac.getPatient() == null ? null : patientMapper.map2DTO(patVac.getPatient()));
		dto.setVaccine(patVac.getVaccine() == null ? null : vaccineMapper.map2DTO(patVac.getVaccine()));
		dto.setLock(patVac.getLock());
		return dto;
	}

	@Override
	public PatientVaccine map2Model(PatientVaccineDTO dto) {
		PatientVaccine patVac = new PatientVaccine();
		patVac.setCode(dto.getCode());
		patVac.setProgr(dto.getProgr());
		patVac.setVaccineDate(dto.getVaccineDate());
		patVac.setPatient(dto.getPatient() == null ? null : patientMapper.map2Model(dto.getPatient()));
		patVac.setVaccine(dto.getVaccine() == null ? null : vaccineMapper.map2Model(dto.getVaccine()));
		patVac.setLock(dto.getLock());
		return patVac;
	}

	@Override
	public List<PatientVaccineDTO> map2DTOList(List<PatientVaccine> list) {
		return list.stream().map(this::map2DTO).toList();
	}

	@Override
	public List<PatientVaccine> map2ModelList(List<PatientVaccineDTO> list) {
		return list.stream().map(this::map2Model).toList();
	}
}
