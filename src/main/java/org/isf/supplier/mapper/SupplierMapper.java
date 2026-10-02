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
package org.isf.supplier.mapper;

import java.util.List;

import org.isf.shared.GenericMapper;
import org.isf.supplier.dto.SupplierDTO;
import org.isf.supplier.model.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper extends GenericMapper<Supplier, SupplierDTO> {
	public SupplierMapper() {
		super(Supplier.class, SupplierDTO.class);
	}

	/*
	 * Mapped by hand: the model keeps the deleted flag as a Character ('Y' / 'N'), the DTO as a Boolean.
	 */

	@Override
	public SupplierDTO map2DTO(Supplier supplier) {
		SupplierDTO dto = new SupplierDTO();
		dto.setSupId(supplier.getSupId());
		dto.setSupName(supplier.getSupName());
		dto.setSupAddress(supplier.getSupAddress());
		dto.setSupTaxcode(supplier.getSupTaxcode());
		dto.setSupPhone(supplier.getSupPhone());
		dto.setSupFax(supplier.getSupFax());
		dto.setSupEmail(supplier.getSupEmail());
		dto.setSupNote(supplier.getSupNote());
		dto.setSupDeleted(supplier.getSupDeleted() != null && supplier.getSupDeleted() == 'Y');
		dto.setLock(supplier.getLock());
		return dto;
	}

	/**
	 * A missing deleted flag maps to 'N'; callers updating a supplier keep its stored flag instead.
	 */
	@Override
	public Supplier map2Model(SupplierDTO dto) {
		Supplier supplier = new Supplier(dto.getSupId(), dto.getSupName(), dto.getSupAddress(), dto.getSupTaxcode(),
			dto.getSupPhone(), dto.getSupFax(), dto.getSupEmail(), dto.getSupNote());
		supplier.setSupDeleted(Boolean.TRUE.equals(dto.getSupDeleted()) ? 'Y' : 'N');
		supplier.setLock(dto.getLock());
		return supplier;
	}

	@Override
	public List<SupplierDTO> map2DTOList(List<Supplier> list) {
		return list.stream().map(this::map2DTO).toList();
	}

	@Override
	public List<Supplier> map2ModelList(List<SupplierDTO> list) {
		return list.stream().map(this::map2Model).toList();
	}
}
