/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2025 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.isf.disease.dto.DiseaseDTO;
import org.isf.disease.mapper.DiseaseMapper;
import org.isf.exam.dto.ExamDTO;
import org.isf.exam.mapper.ExamMapper;
import org.isf.operation.dto.OperationDTO;
import org.isf.operation.mapper.OperationMapper;
import org.isf.vaccine.dto.VaccineDTO;
import org.isf.vaccine.mapper.VaccineMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A new item posted without a lock must reach the model with a {@code null} lock: with 0, Spring Data merges it
 * instead of inserting it, and the insert fails with an optimistic lock error.
 */
class NewItemLockMappingTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static Stream<Arguments> newItems() {
		return Stream.of(
			Arguments.of("Disease", mapper(new DiseaseMapper()), DiseaseDTO.class,
				"{\"code\":\"X1\",\"description\":\"d\",\"diseaseType\":{\"code\":\"T\",\"description\":\"t\"}}"),
			Arguments.of("Vaccine", mapper(new VaccineMapper()), VaccineDTO.class,
				"{\"code\":\"X1\",\"description\":\"d\",\"vaccineType\":{\"code\":\"T\",\"description\":\"t\"}}"),
			Arguments.of("Exam", mapper(new ExamMapper()), ExamDTO.class,
				"{\"code\":\"X1\",\"description\":\"d\",\"procedure\":3,\"examtype\":{\"code\":\"T\",\"description\":\"t\"}}"),
			Arguments.of("Operation", mapper(new OperationMapper()), OperationDTO.class,
				"{\"code\":\"X1\",\"description\":\"d\",\"major\":0,\"type\":{\"code\":\"T\",\"description\":\"t\"}}"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("newItems")
	<D> void newItemKeepsNullLock(String name, GenericMapper<?, D> mapper, Class<D> dtoClass, String json) throws Exception {
		D dto = OBJECT_MAPPER.readValue(json, dtoClass);
		Object model = mapper.map2Model(dto);
		assertThat(ReflectionTestUtils.getField(model, "lock")).as(name + " lock").isNull();
	}

	private static <M extends GenericMapper<?, ?>> M mapper(M mapper) {
		ReflectionTestUtils.setField(mapper, "modelMapper", new ModelMapper());
		return mapper;
	}
}
