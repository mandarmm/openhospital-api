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
package org.isf.medicalstockward.dto;

import jakarta.validation.constraints.NotNull;

import org.isf.medical.dto.MedicalDTO;
import org.isf.medicalstock.dto.LotDTO;
import org.isf.ward.dto.WardDTO;

import io.swagger.v3.oas.annotations.media.Schema;

public class MedicalWardIdDTO {

	@NotNull
	@Schema(description = "The ward")
	private WardDTO ward;

	@NotNull
	@Schema(description = "The medical")
	private MedicalDTO medical;

	@Schema(description = "The lot: the stock of a ward is kept per lot")
	private LotDTO lot;

	public MedicalWardIdDTO() {
	}

	public MedicalWardIdDTO(WardDTO ward, MedicalDTO medical) {
		this.ward = ward;
		this.medical = medical;
	}

	public WardDTO getWard() {
		return this.ward;
	}

	public MedicalDTO getMedical() {
		return this.medical;
	}

	public void setWard(WardDTO ward) {
		this.ward = ward;
	}

	public LotDTO getLot() {
		return this.lot;
	}

	public void setLot(LotDTO lot) {
		this.lot = lot;
	}

	public void setMedical(MedicalDTO medical) {
		this.medical = medical;
	}
}
