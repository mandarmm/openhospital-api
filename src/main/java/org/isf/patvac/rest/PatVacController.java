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
package org.isf.patvac.rest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.patvac.dto.PatientVaccineDTO;
import org.isf.patvac.manager.PatVacManager;
import org.isf.patvac.mapper.PatVacMapper;
import org.isf.patvac.model.PatientVaccine;
import org.isf.patvac.service.PatVacIoOperations;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.vaccine.manager.VaccineBrowserManager;
import org.isf.vaccine.model.Vaccine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Patient Vaccines")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class PatVacController {

	private static final Logger LOGGER = LoggerFactory.getLogger(PatVacController.class);

	private final PatVacManager patVacManager;

	private final PatVacMapper mapper;

	private final PatVacIoOperations patVacIoOperations;

	private final PatientBrowserManager patientManager;

	private final VaccineBrowserManager vaccineManager;

	public PatVacController(PatVacManager patVacManager, PatVacMapper patientVaccineMapper, PatVacIoOperations patVacIoOperations,
		PatientBrowserManager patientManager, VaccineBrowserManager vaccineManager) {
		this.patVacManager = patVacManager;
		this.mapper = patientVaccineMapper;
		this.patVacIoOperations = patVacIoOperations;
		this.patientManager = patientManager;
		this.vaccineManager = vaccineManager;
	}

	/**
	 * Create a new {@link PatientVaccine}.
	 * @param patientVaccineDTO Patient Vaccine DTO
	 * @return {@code true} if the operation type has been stored, {@code false} otherwise.
	 * @throws OHServiceException When failed to create patient vaccine
	 */
	@PostMapping("/patientvaccines")
	@ResponseStatus(HttpStatus.CREATED)
	public PatientVaccineDTO newPatientVaccine(@RequestBody PatientVaccineDTO patientVaccineDTO) throws OHServiceException {
		LOGGER.info("Create patient vaccine {}", patientVaccineDTO.getCode());

		// the core's messages (date, progressive, vaccine, patient) reach the client
		PatientVaccine patVac = withStored(mapper.map2Model(patientVaccineDTO));
		patVac.setCode(0);
		return mapper.map2DTO(patVacManager.newPatientVaccine(patVac));
	}

	/**
	 * Updates the specified {@link PatientVaccine}.
	 * @param patientVaccineDTO Patient Vaccine payload
	 * @return {@code true} if the operation type has been updated, {@code false} otherwise.
	 * @throws OHServiceException When failed to update patient vaccine
	 */
	@PutMapping("/patientvaccines/{code}")
	public PatientVaccineDTO updatePatientVaccinet(
		@PathVariable Integer code, @RequestBody PatientVaccineDTO patientVaccineDTO
	) throws OHServiceException {
		LOGGER.info("Update patientvaccines code: {}", patientVaccineDTO.getCode());
		if (patVacManager.getPatientVaccine(code).isEmpty()) {
			throw new OHAPIException(new OHExceptionMessage("Patient vaccine not found."), HttpStatus.NOT_FOUND);
		}
		PatientVaccine patVac = withStored(mapper.map2Model(patientVaccineDTO));
		patVac.setCode(code);
		return mapper.map2DTO(patVacManager.updatePatientVaccine(patVac));
	}

	/**
	 * Get all the {@link PatientVaccine}s for today or in the last week.
	 * @return the list of {@link PatientVaccine}s
	 * @throws OHServiceException When failed to get patient vaccines
	 */
	@GetMapping("/patientvaccines/week")
	public List<PatientVaccineDTO> getPatientVaccines(
		@RequestParam(required=false) Boolean oneWeek
	) throws OHServiceException {
		LOGGER.info("Get the all patient vaccine of to day or one week");
		if (oneWeek == null) {
			oneWeek = false;
		}

		return mapper.map2DTOList(patVacManager.getPatientVaccine(oneWeek));
	}

	/**
	 * Get all {@link PatientVaccine}s within {@code dateFrom} and {@code dateTo}.
	 * @return the list of {@link PatientVaccine}s
	 * @throws OHServiceException When failed to get patient vaccines
	 */
	@GetMapping("/patientvaccines/filter")
	public List<PatientVaccineDTO> getPatientVaccinesByDatesRanges(
		@RequestParam String vaccineTypeCode,
		@RequestParam String vaccineCode,
		@RequestParam LocalDate dateFrom,
		@RequestParam LocalDate dateTo,
		@RequestParam char sex,
		@RequestParam int ageFrom,
		@RequestParam int ageTo
	) throws OHServiceException {
		LOGGER.info("filter patient vaccine by dates ranges");

		// the whole "to" day: its vaccinations were left out
		return mapper.map2DTOList(patVacManager.getPatientVaccine(
			vaccineTypeCode, vaccineCode, dateFrom.atStartOfDay(), dateTo.atTime(LocalTime.MAX), sex, ageFrom, ageTo
		));
	}

	/**
	 * Get the maximum progressive number within specified year or within current year if {@code 0}.
	 * @return {@code int} - the progressive number in the year
	 * @throws OHServiceException When failed to get the progressive number
	 */
	@GetMapping("/patientvaccines/progyear/{year}")
	public Integer getProgYear(@PathVariable int year) throws OHServiceException {
		LOGGER.info("Get progressive number within specified year");

		return patVacManager.getProgYear(year);
	}

	/**
	 * Delete {@link PatientVaccine} for specified code.
	 * @param code Patient vaccine code
	 * @return {@code true} if the {@link PatientVaccine} has been deleted, {@code false} otherwise.
	 * @throws OHServiceException When failed to delete patient vaccine
	 */
	@DeleteMapping("/patientvaccines/{code}")
	public boolean deletePatientVaccine(@PathVariable int code) throws OHServiceException {
		LOGGER.info("Delete patient vaccine code: {}", code);
		// the stored vaccination, with its lock: a bare one with only the code failed once it had been updated
		PatientVaccine patVac = patVacManager.getPatientVaccine(code)
			.orElseThrow(() -> new OHAPIException(new OHExceptionMessage("Patient vaccine not found."), HttpStatus.NOT_FOUND));
		patVacManager.deletePatientVaccine(patVac);
		return true;
	}

	/**
	 * The vaccinations of a patient.
	 *
	 * @param patientCode the patient's code
	 * @return the patient's vaccinations
	 * @throws OHServiceException When failed to get them
	 */
	@GetMapping("/patientvaccines/patient/{patientCode}")
	public List<PatientVaccineDTO> getPatientVaccinesOfPatient(@PathVariable int patientCode) throws OHServiceException {
		return mapper.map2DTOList(patVacIoOperations.findForPatient(patientCode));
	}

	/**
	 * The stored patient and vaccine: the payload may carry only their codes.
	 */
	private PatientVaccine withStored(PatientVaccine patVac) throws OHServiceException {
		if (patVac.getPatient() != null) {
			Patient patient = patientManager.getPatientById(patVac.getPatient().getCode());
			if (patient == null) {
				throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
			}
			patVac.setPatient(patient);
		}
		if (patVac.getVaccine() != null) {
			Vaccine vaccine = vaccineManager.findVaccine(patVac.getVaccine().getCode());
			if (vaccine == null) {
				throw new OHAPIException(new OHExceptionMessage("Vaccine not found."), HttpStatus.NOT_FOUND);
			}
			patVac.setVaccine(vaccine);
		}
		return patVac;
	}
}
