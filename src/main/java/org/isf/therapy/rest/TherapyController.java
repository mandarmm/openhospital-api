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
package org.isf.therapy.rest;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import org.isf.medical.dto.MedicalDTO;
import org.isf.medical.mapper.MedicalMapper;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.sms.service.SmsOperations;
import org.isf.therapy.dto.TherapyDTO;
import org.isf.therapy.dto.TherapyRowDTO;
import org.isf.therapy.manager.TherapyManager;
import org.isf.therapy.mapper.TherapyMapper;
import org.isf.therapy.mapper.TherapyRowMapper;
import org.isf.therapy.model.TherapyRow;
import org.isf.therapy.service.TherapyIoOperationRepository;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Therapies")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TherapyController {

	private final TherapyManager manager;

	private final TherapyMapper therapyMapper;

	private final TherapyRowMapper therapyRowMapper;

	private final MedicalMapper medicalMapper;

	private final TherapyIoOperationRepository therapyRepository;

	private final SmsOperations smsOperations;

	public TherapyController(
		TherapyManager manager,
		TherapyMapper therapyMapper,
		TherapyRowMapper therapyRowMapper,
		MedicalMapper medicalMapper,
		TherapyIoOperationRepository therapyRepository,
		SmsOperations smsOperations
	) {
		this.manager = manager;
		this.therapyMapper = therapyMapper;
		this.therapyRowMapper = therapyRowMapper;
		this.medicalMapper = medicalMapper;
		this.therapyRepository = therapyRepository;
		this.smsOperations = smsOperations;
	}

	/**
	 * Creates a new therapy for related Patient.
	 * @param thRowDTO - the therapy
	 * @return the created therapy
	 * @throws OHServiceException When failed to create therapy
	 */
	@PostMapping("/therapies")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(rollbackFor = OHServiceException.class)
	public TherapyRowDTO newTherapy(@RequestBody TherapyRowDTO thRowDTO) throws OHServiceException {
		if (thRowDTO.getPatID() == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}

		TherapyRow created = manager.newTherapy(therapyRowMapper.map2Model(thRowDTO));
		rescheduleSms(created.getPatient().getCode());
		return therapyRowMapper.map2DTO(created);
	}

	/**
	 * Replaces all therapies for related Patient, as the Swing therapy form saves them.
	 * @param thRowDTOs - the list of therapies, all of the same patient
	 * @return the first of the patient's therapies once replaced
	 * @throws OHServiceException When failed to replace patient therapies
	 */
	@PostMapping("/therapies/replace")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(rollbackFor = OHServiceException.class)
	public TherapyRowDTO replaceTherapies(
		@RequestBody @Valid List<TherapyRowDTO> thRowDTOs
	) throws OHServiceException {
		if (thRowDTOs.isEmpty()) {
			throw new OHAPIException(new OHExceptionMessage("No therapies to replace with."));
		}
		List<TherapyRow> therapies = new ArrayList<>(therapyRowMapper.map2ModelList(thRowDTOs));
		Integer patientCode = therapies.get(0).getPatient().getCode();
		if (therapies.stream().anyMatch(therapy -> !patientCode.equals(therapy.getPatient().getCode()))) {
			throw new OHAPIException(new OHExceptionMessage("The therapies must be of one patient."));
		}
		manager.deleteAllTherapies(patientCode);
		// inserted again, with new ids
		therapies.forEach(therapy -> therapy.setTherapyID(0));
		manager.newTherapies(therapies);
		return therapyRowMapper.map2DTO(manager.getTherapyRows(patientCode).get(0));
	}

	/**
	 * Updates one therapy.
	 * @param therapyID - the therapy's id
	 * @param thRowDTO - the therapy
	 * @return the updated therapy
	 * @throws OHServiceException When failed to update the therapy
	 */
	@PutMapping("/therapies/rows/{therapyID}")
	@Transactional(rollbackFor = OHServiceException.class)
	public TherapyRowDTO updateTherapy(
		@PathVariable("therapyID") int therapyID, @RequestBody @Valid TherapyRowDTO thRowDTO
	) throws OHServiceException {
		TherapyRow stored = therapyRepository.findById(therapyID)
			.orElseThrow(() -> new OHAPIException(new OHExceptionMessage("Therapy not found."), HttpStatus.NOT_FOUND));
		if (thRowDTO.getTherapyID() != 0 && thRowDTO.getTherapyID() != therapyID) {
			throw new OHAPIException(new OHExceptionMessage("Therapy id mismatch."));
		}
		TherapyRow therapy = therapyRowMapper.map2Model(thRowDTO);
		therapy.setTherapyID(therapyID);
		// a therapy stays with its patient
		therapy.setPatient(stored.getPatient());
		TherapyRow updated = manager.newTherapy(therapy);
		rescheduleSms(stored.getPatient().getCode());
		return therapyRowMapper.map2DTO(updated);
	}

	/**
	 * Deletes one therapy.
	 * @param therapyID - the therapy's id
	 * @return {@code true} if the therapy has been deleted
	 * @throws OHServiceException When failed to delete the therapy
	 */
	@DeleteMapping("/therapies/rows/{therapyID}")
	@Transactional(rollbackFor = OHServiceException.class)
	public boolean deleteTherapy(@PathVariable("therapyID") int therapyID) throws OHServiceException {
		TherapyRow stored = therapyRepository.findById(therapyID)
			.orElseThrow(() -> new OHAPIException(new OHExceptionMessage("Therapy not found."), HttpStatus.NOT_FOUND));
		Integer patientCode = stored.getPatient().getCode();
		therapyRepository.delete(stored);
		rescheduleSms(patientCode);
		return true;
	}

	/**
	 * Schedules again the SMS reminders of the patient's therapies, as the Swing therapy form does when it saves them:
	 * {@link TherapyManager#newTherapies} saves the therapies unchanged and schedules the reminders of those with SMS.
	 */
	private void rescheduleSms(Integer patientCode) throws OHServiceException {
		List<TherapyRow> therapies = manager.getTherapyRows(patientCode);
		if (therapies.isEmpty()) {
			smsOperations.deleteByModuleModuleID("therapy", String.valueOf(patientCode));
		} else {
			manager.newTherapies(therapies);
		}
	}

	/**
	 * Deletes all therapies for specified Patient Code.
	 * @param code - the Patient Code
	 * @return {@code true} if the therapies have been deleted, throws an exception otherwise
	 * @throws OHServiceException When failed to delete patient therapies
	 */
	@DeleteMapping("/therapies/{code_patient}")
	public boolean deleteAllTherapies(@PathVariable("code_patient") Integer code) throws OHServiceException {
		try {
			manager.deleteAllTherapies(code);
			return true;
		} catch (OHServiceException serviceException) {
			throw new OHAPIException(new OHExceptionMessage("Therapies not deleted."));
		}
	}

	/**
	 * Gets the medicals that are not available for the specified list of therapies.
	 * @param therapyDTOs - the list of therapies
	 * @return the list of medicals out of stock
	 * @throws OHServiceException When failed to get not available medicals
	 */
	@PostMapping("/therapies/meds-out-of-stock")
	public List<MedicalDTO> getMedicalsOutOfStock(
		@RequestBody List<TherapyDTO> therapyDTOs
	) throws OHServiceException {
		return medicalMapper.map2DTOList(manager.getMedicalsOutOfStock(therapyMapper.map2ModelList(therapyDTOs)));
	}

	/**
	 * Gets the list of therapies for specified Patient ID.
	 * @param patientID - the Patient ID
	 * @return the list of therapies of the patient or all the therapies if {@code 0} is passed
	 * @throws OHServiceException When failed to get patient therapies
	 */
	@GetMapping("/therapies/{code_patient}")
	public List<TherapyRowDTO> getTherapyRows
	(@PathVariable("code_patient") Integer patientID
	) throws OHServiceException {
		return therapyRowMapper.map2DTOList(manager.getTherapyRows(patientID));
	}

	/**
	 * Gets a list of therapies from a list of therapyRows (DB records).
	 * @param thRowDTOs - the list of therapyRows
	 * @return the list of therapies
	 * @throws OHServiceException When failed to get therapies
	 */
	@PostMapping("/therapies/from-rows")
	public List<TherapyDTO> getTherapies(
		@RequestBody @Valid List<TherapyRowDTO> thRowDTOs
	) throws OHServiceException {
		return therapyMapper.map2DTOList(manager.getTherapies(therapyRowMapper.map2ModelList(thRowDTOs)));
	}

	/**
	 * Gets therapy from a therapyRow (DB record).
	 * @param thRowDTO - the therapyRow
	 * @return the therapy
	 * @throws OHServiceException When failed to get therapy
	 */
	@PostMapping("/therapies/from-row")
	public TherapyDTO getTherapy(@RequestBody @Valid TherapyRowDTO thRowDTO) throws OHServiceException {
		return therapyMapper.map2DTO(manager.createTherapy(therapyRowMapper.map2Model(thRowDTO)));
	}
}
