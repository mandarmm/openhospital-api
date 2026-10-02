/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2026 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.medicalinventory.dto;

import java.math.BigDecimal;

import org.isf.medical.dto.MedicalDTO;
import org.isf.medicalstock.dto.LotDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;

/**
 * A row of an inventory: a lot of a medical, its theoretical quantity (the stock) and the counted one.
 */
@Schema(description = "Class representing an inventory row")
public class MedicalInventoryRowDTO {

	@Schema(description = "The row's id; empty for a new row")
	private Integer id;

	@Schema(description = "The quantity in stock", example = "20")
	private BigDecimal theoreticQty;

	@Schema(description = "The counted quantity", example = "18")
	private BigDecimal realQty;

	@Schema(description = "The medical")
	private MedicalDTO medical;

	@Schema(description = "The lot (an existing one)")
	private LotDTO lot;

	@Schema(description = "Whether the lot was added by the inventory", accessMode = AccessMode.READ_ONLY)
	private boolean newLot;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public BigDecimal getTheoreticQty() {
		return theoreticQty;
	}

	public void setTheoreticQty(BigDecimal theoreticQty) {
		this.theoreticQty = theoreticQty;
	}

	public BigDecimal getRealQty() {
		return realQty;
	}

	public void setRealQty(BigDecimal realQty) {
		this.realQty = realQty;
	}

	public MedicalDTO getMedical() {
		return medical;
	}

	public void setMedical(MedicalDTO medical) {
		this.medical = medical;
	}

	public LotDTO getLot() {
		return lot;
	}

	public void setLot(LotDTO lot) {
		this.lot = lot;
	}

	public boolean isNewLot() {
		return newLot;
	}

	public void setNewLot(boolean newLot) {
		this.newLot = newLot;
	}
}
