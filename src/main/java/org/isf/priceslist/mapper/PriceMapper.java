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
package org.isf.priceslist.mapper;

import java.util.List;

import org.isf.priceslist.dto.PriceDTO;
import org.isf.priceslist.model.Price;
import org.isf.shared.GenericMapper;
import org.springframework.stereotype.Component;

@Component
public class PriceMapper extends GenericMapper<Price, PriceDTO> {

	private final PriceListMapper priceListMapper;

	public PriceMapper(PriceListMapper priceListMapper) {
		super(Price.class, PriceDTO.class);
		this.priceListMapper = priceListMapper;
	}

	/*
	 * Mapped by hand: the model has both getPrice() (the amount) and isPrice() (whether the price has an item), so
	 * implicit mapping could take the boolean for "price" depending on reflection order and return 1 for every price.
	 */

	@Override
	public PriceDTO map2DTO(Price price) {
		PriceDTO dto = new PriceDTO();
		dto.setId(price.getId());
		dto.setList(price.getList() != null ? priceListMapper.map2DTO(price.getList()) : null);
		dto.setGroup(price.getGroup());
		dto.setItem(price.getItem());
		dto.setDescription(price.getDesc());
		dto.setPrice(price.getPrice());
		dto.setLock(price.getLock());
		dto.setEditable(price.isEditable());
		return dto;
	}

	@Override
	public Price map2Model(PriceDTO dto) {
		Price price = new Price(dto.getId(), dto.getList() != null ? priceListMapper.map2Model(dto.getList()) : null, dto.getGroup(),
			dto.getItem(), dto.getDescription(), dto.getPrice());
		price.setEditable(dto.isEditable());
		price.setLock(dto.getLock());
		return price;
	}

	@Override
	public List<PriceDTO> map2DTOList(List<Price> list) {
		return list.stream().map(this::map2DTO).toList();
	}

	@Override
	public List<Price> map2ModelList(List<PriceDTO> list) {
		return list.stream().map(this::map2Model).toList();
	}
}