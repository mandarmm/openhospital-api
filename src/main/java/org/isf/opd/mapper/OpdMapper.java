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
package org.isf.opd.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.isf.opd.dto.OpdDTO;
import org.isf.opd.model.Opd;
import org.isf.shared.GenericMapper;
import org.isf.visits.model.Visit;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OpdMapper extends GenericMapper<Opd, OpdDTO> {
	
	public OpdMapper() {
		super(Opd.class, OpdDTO.class);
	}
	
	/*
	 * The OPD number is "prog_year" in the DTO and "progYear" in the model, so implicit mapping lost it. The next
	 * visit is a Visit in the model and only its date in the DTO: it is read-only here (implicit mapping failed on
	 * it with an error 500); the visit itself is managed through /visits.
	 */
	private synchronized void configure(ModelMapper modelMapper) {
		if (modelMapper.getTypeMap(OpdDTO.class, Opd.class) == null) {
			modelMapper.emptyTypeMap(OpdDTO.class, Opd.class)
				.addMappings(mapping -> {
					mapping.skip(Opd::setNextVisit);
					mapping.map(OpdDTO::getProg_year, Opd::setProgYear);
				})
				.implicitMappings();
		}
		if (modelMapper.getTypeMap(Opd.class, OpdDTO.class) == null) {
			Converter<Visit, LocalDateTime> visitDate = context -> context.getSource() == null ? null : context.getSource().getDate();
			modelMapper.emptyTypeMap(Opd.class, OpdDTO.class)
				.addMappings(mapping -> {
					mapping.map(Opd::getProgYear, OpdDTO::setProg_year);
					mapping.using(visitDate).map(Opd::getNextVisit, OpdDTO::setNextVisitDate);
				})
				.implicitMappings();
		}
	}

	@Override
	public Opd map2Model(OpdDTO toObj) {
		configure(modelMapper);
		return super.map2Model(toObj);
	}

	@Override
	public OpdDTO map2DTO(Opd fromObj) {
		configure(modelMapper);
		OpdDTO opdDTO = super.map2DTO(fromObj);
		if (fromObj.getPatient() != null) {
			opdDTO.setPatientCode(fromObj.getPatient().getCode());
			opdDTO.setPatientName(fromObj.getFullName());
			if (fromObj.getAge() == 0 ) {
				opdDTO.setAgeType("d0");
			}
			if (fromObj.getAge() > 0 && fromObj.getAge() <= 5) {
				opdDTO.setAgeType("d1");
			}
			if (fromObj.getAge() > 5 && fromObj.getAge() <= 12) {
				opdDTO.setAgeType("d2");
			}
			if (fromObj.getAge() > 12 && fromObj.getAge() <= 24) {
				opdDTO.setAgeType("d3");
			}
			if (fromObj.getAge() > 24 && fromObj.getAge() <= 59) {
				opdDTO.setAgeType("d4");
			}
			if (fromObj.getAge() > 59 && fromObj.getAge() < 200) {
				opdDTO.setAgeType("d5");
			}
		}
		return opdDTO;

	}
	
	@Override
	public List<OpdDTO> map2DTOList(List<Opd> list) {
		return list.stream().map(it -> map2DTO(it)).collect(Collectors.toList());
	}

	@Override
	public List<Opd> map2ModelList(List<OpdDTO> list) {
		return list.stream().map(this::map2Model).collect(Collectors.toList());
	}
}