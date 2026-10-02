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

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;

/**
 * An inventory of the main store ({@code main}) or of a ward ({@code ward}).
 */
@Schema(description = "Class representing an inventory")
public class MedicalInventoryDTO {

	@Schema(description = "The inventory's id", accessMode = AccessMode.READ_ONLY)
	private Integer id;

	@Schema(description = "draft, validated, canceled or done", accessMode = AccessMode.READ_ONLY)
	private String status;

	@Schema(description = "The date of the count")
	private LocalDateTime inventoryDate;

	@Schema(description = "The user who made it", accessMode = AccessMode.READ_ONLY)
	private String user;

	@Schema(description = "The reference, unique (max 40 characters)", example = "INV-2026-10")
	private String inventoryReference;

	@Schema(description = "main or ward", example = "main")
	private String inventoryType;

	@Schema(description = "The ward of a ward inventory")
	private String wardCode;

	@Schema(description = "The movement type that books what was counted in excess", example = "charge")
	private String chargeType;

	@Schema(description = "The movement type that books what was missing", example = "discharge")
	private String dischargeType;

	@Schema(description = "The supplier of the charges")
	private Integer supplier;

	@Schema(description = "The ward the discharges go to")
	private String destination;

	@Schema(description = "Lock", example = "0")
	private int lock;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getInventoryDate() {
		return inventoryDate;
	}

	public void setInventoryDate(LocalDateTime inventoryDate) {
		this.inventoryDate = inventoryDate;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getInventoryReference() {
		return inventoryReference;
	}

	public void setInventoryReference(String inventoryReference) {
		this.inventoryReference = inventoryReference;
	}

	public String getInventoryType() {
		return inventoryType;
	}

	public void setInventoryType(String inventoryType) {
		this.inventoryType = inventoryType;
	}

	public String getWardCode() {
		return wardCode;
	}

	public void setWardCode(String wardCode) {
		this.wardCode = wardCode;
	}

	public String getChargeType() {
		return chargeType;
	}

	public void setChargeType(String chargeType) {
		this.chargeType = chargeType;
	}

	public String getDischargeType() {
		return dischargeType;
	}

	public void setDischargeType(String dischargeType) {
		this.dischargeType = dischargeType;
	}

	public Integer getSupplier() {
		return supplier;
	}

	public void setSupplier(Integer supplier) {
		this.supplier = supplier;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public int getLock() {
		return lock;
	}

	public void setLock(int lock) {
		this.lock = lock;
	}
}
