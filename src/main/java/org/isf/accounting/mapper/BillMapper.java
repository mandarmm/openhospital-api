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
package org.isf.accounting.mapper;

import java.util.List;

import org.isf.accounting.dto.BillDTO;
import org.isf.accounting.model.Bill;
import org.isf.patient.dto.PatientDTO;
import org.isf.shared.GenericMapper;
import org.springframework.stereotype.Component;

/**
 * Bills, mapped by hand: implicit mapping mixed the model's {@code isPatient()} flag ("patient") with the DTO's patient,
 * so the flag was never set ({@code patientTrue} false, and bills stored as not of a patient), and the model's setters
 * {@code setIsPatient} / {@code setIsList} are not found by name. The patient and the price list of a new or changed
 * bill are looked up by the controller.
 */
@Component
public class BillMapper extends GenericMapper<Bill, BillDTO> {

	public BillMapper() {
		super(Bill.class, BillDTO.class);
	}

	@Override
	public BillDTO map2DTO(Bill bill) {
		if (bill == null) {
			return null;
		}
		BillDTO dto = new BillDTO();
		dto.setId(bill.getId());
		if (bill.getBillPatient() != null) {
			PatientDTO patient = modelMapper.map(bill.getBillPatient(), PatientDTO.class);
			// a bill shows who, not the photo
			patient.setBlobPhoto(null);
			dto.setPatient(patient);
		}
		dto.setListId(bill.getPriceList() == null ? null : bill.getPriceList().getId());
		dto.setDate(bill.getDate());
		dto.setUpdate(bill.getUpdate());
		dto.setList(bill.isList());
		dto.setListName(bill.getListName());
		dto.setPatientTrue(bill.isPatient());
		dto.setPatName(bill.getPatName());
		dto.setStatus(bill.getStatus());
		dto.setAmount(bill.getAmount());
		dto.setBalance(bill.getBalance());
		dto.setUser(bill.getUser());
		dto.setLock(bill.getLock());
		dto.setAdmissionId(bill.getAdmission() == null ? null : bill.getAdmission().getId());
		return dto;
	}

	@Override
	public Bill map2Model(BillDTO dto) {
		if (dto == null) {
			return null;
		}
		Bill bill = new Bill();
		bill.setId(dto.getId() == null ? 0 : dto.getId());
		bill.setDate(dto.getDate());
		bill.setUpdate(dto.getUpdate());
		bill.setIsList(dto.isList());
		bill.setListName(dto.getListName());
		bill.setIsPatient(dto.isPatientTrue());
		bill.setPatName(dto.getPatName());
		bill.setStatus(dto.getStatus());
		bill.setAmount(dto.getAmount());
		bill.setBalance(dto.getBalance());
		bill.setUser(dto.getUser());
		bill.setLock(dto.getLock());
		return bill;
	}

	@Override
	public List<BillDTO> map2DTOList(List<Bill> bills) {
		return bills.stream().map(this::map2DTO).toList();
	}

	@Override
	public List<Bill> map2ModelList(List<BillDTO> dtos) {
		return dtos.stream().map(this::map2Model).toList();
	}
}
