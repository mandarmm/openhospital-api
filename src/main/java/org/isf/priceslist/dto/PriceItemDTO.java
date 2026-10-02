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
package org.isf.priceslist.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The price of one item in a price list, as sent to {@code PUT /pricelists/{id}/prices}.
 *
 * @param group       the kind of item: EXA (exam code), OPE (operation code), MED (medical code) or OTH (other price id)
 * @param item        the item code or id
 * @param description the item description stored with the price
 * @param price       the price
 */
public record PriceItemDTO(
	@NotNull @Pattern(regexp = "EXA|OPE|MED|OTH")
	@Schema(description = "Kind of item: EXA (exam), OPE (operation), MED (medical) or OTH (other price)", example = "EXA")
	String group,

	@NotNull @Size(max = 10)
	@Schema(description = "Item code (exam, operation or medical code) or other price id", example = "01.01")
	String item,

	@Size(max = 100)
	@Schema(description = "Item description stored with the price", example = "1.1 HB")
	String description,

	@NotNull @DecimalMin("0")
	@Schema(description = "The price", example = "10.00")
	BigDecimal price) {
}
