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

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An inventory with its rows.
 */
@Schema(description = "Class representing an inventory with its rows")
public class InventoryWithRowsDTO {

	@Schema(description = "The inventory")
	private MedicalInventoryDTO inventory;

	@Schema(description = "The rows")
	private List<MedicalInventoryRowDTO> rows = new ArrayList<>();

	public InventoryWithRowsDTO() {
	}

	public InventoryWithRowsDTO(MedicalInventoryDTO inventory, List<MedicalInventoryRowDTO> rows) {
		this.inventory = inventory;
		this.rows = rows;
	}

	public MedicalInventoryDTO getInventory() {
		return inventory;
	}

	public void setInventory(MedicalInventoryDTO inventory) {
		this.inventory = inventory;
	}

	public List<MedicalInventoryRowDTO> getRows() {
		return rows;
	}

	public void setRows(List<MedicalInventoryRowDTO> rows) {
		this.rows = rows;
	}
}
