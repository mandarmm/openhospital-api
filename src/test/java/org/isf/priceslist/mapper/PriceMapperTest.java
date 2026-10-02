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
package org.isf.priceslist.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.isf.priceslist.dto.PriceDTO;
import org.isf.priceslist.model.Price;
import org.isf.priceslist.model.PriceList;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Price has both getPrice() (the amount) and isPrice() (whether it has an item): the amount must always be mapped.
 */
class PriceMapperTest {

	@Test
	void mapsTheAmountNotTheItemFlag() {
		PriceListMapper listMapper = new PriceListMapper();
		ReflectionTestUtils.setField(listMapper, "modelMapper", new ModelMapper());
		PriceMapper mapper = new PriceMapper(listMapper);
		PriceList list = new PriceList(4, "LIST004", "Private", "Private list", "EUR");
		Price price = new Price(7, list, "EXA", "01.01", "HB", new BigDecimal("12.50"));
		assertThat(price.isPrice()).isTrue();

		PriceDTO dto = mapper.map2DTO(price);
		assertThat(dto.getPrice()).isEqualByComparingTo("12.50");
		assertThat(dto.getDescription()).isEqualTo("HB");
		assertThat(dto.getList().getId()).isEqualTo(4);

		Price back = mapper.map2Model(dto);
		assertThat(back.getPrice()).isEqualByComparingTo("12.50");
		assertThat(back.getDesc()).isEqualTo("HB");
		assertThat(mapper.map2DTOList(List.of(price))).extracting(PriceDTO::getPrice).containsExactly(new BigDecimal("12.50"));
	}
}
