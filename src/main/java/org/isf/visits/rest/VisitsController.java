/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2024 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.visits.rest;

import java.time.LocalDate;
import java.util.List;

import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.visits.dto.VisitDTO;
import org.isf.visits.manager.VisitManager;
import org.isf.visits.mapper.VisitMapper;
import org.isf.visits.model.Visit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
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
@Tag(name = "Visit")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class VisitsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VisitsController.class);

	private final VisitManager visitManager;

	private final VisitMapper mapper;

	private final PatientBrowserManager patientManager;

	public VisitsController(VisitManager visitManager, VisitMapper visitMapper, PatientBrowserManager patientManager) {
		this.visitManager = visitManager;
		this.mapper = visitMapper;
		this.patientManager = patientManager;
	}

	/**
	 * Get all the visitors related to a patient.
	 *
	 * @param patID the id of the patient
	 * @return NO_CONTENT if there aren't visitors, {@code List<VaccineDTO>} otherwise
	 * @throws OHServiceException When failed to get patient visits
	 */
	/**
	 * The visits of a ward (the desktop client's worksheet), optionally of the days from {@code dateFrom} to
	 * {@code dateTo} (inclusive).
	 */
	@GetMapping("/visits/ward/{wardCode}")
	public List<VisitDTO> getVisitsOfWard(
		@PathVariable String wardCode,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
	) throws OHServiceException {
		LOGGER.info("Get the visits of ward: {}", wardCode);
		List<Visit> visits = visitManager.getVisitsWard(wardCode);
		return mapper.map2DTOList(visits.stream()
			.filter(visit -> dateFrom == null || visit.getDate() != null && !visit.getDate().toLocalDate().isBefore(dateFrom))
			.filter(visit -> dateTo == null || visit.getDate() != null && !visit.getDate().toLocalDate().isAfter(dateTo))
			.toList());
	}

	@GetMapping("/visits/patient/{patID}")
	public List<VisitDTO> getVisit(@PathVariable("patID") int patID) throws OHServiceException {
		LOGGER.info("Get visit related to patId: {}", patID);

		return mapper.map2DTOList(visitManager.getVisits(patID));
	}

	/**
	 * Create a new visitor.
	 *
	 * @param newVisit Visit payload
	 * @return an error if there are some problem, the visitor id (Integer) otherwise
	 * @throws OHServiceException When failed to create visit
	 */
	@PostMapping("/visits")
	@ResponseStatus(HttpStatus.CREATED)
	public VisitDTO newVisit(@RequestBody VisitDTO newVisit) throws OHServiceException {
		LOGGER.info("Create Visit: {}", newVisit);
		return mapper.map2DTO(visitManager.newVisit(withStoredPatient(mapper.map2Model(newVisit))));
	}

	/**
	 * Create new visits.
	 *
	 * @param newVisits a list with all the visitors
	 * @return an error message if there are some problem, ok otherwise
	 * @throws OHServiceException When failed to create visits
	 */
	@PostMapping("/visits/insertList")
	@ResponseStatus(HttpStatus.CREATED)
	public boolean newVisits(@RequestBody List<VisitDTO> newVisits) throws OHServiceException {
		LOGGER.info("Create Visits");
		return visitManager.newVisits(mapper.map2ModelList(newVisits));
	}

	/**
	 * Delete all the visits related to a patient.
	 *
	 * @param patID the id of the patient
	 * @return an error message if there are some problem, ok otherwise
	 * @throws OHServiceException When failed to delete patient visit
	 */
	@DeleteMapping("/visits/delete/{patID}")
	public boolean deleteVisitsRelatedToPatient(
		@PathVariable("patID") int patID
	) throws OHServiceException {
		LOGGER.info("Delete Visit related to patId: {}", patID);

		return visitManager.deleteAllVisits(patID);
	}

	/**
	 * Update visit
	 *
	 * @param visitID the id of the visit
	 * @param updateVisit Visit payload
	 * @return an error message if there are some problem, ok otherwise
	 * @throws OHServiceException When failed to update the visit
	 */
	@PutMapping("/visits/{visitID}")
	public VisitDTO updateVisit(
		@PathVariable("visitID") int visitID, @RequestBody VisitDTO updateVisit
	) throws OHServiceException {
		LOGGER.info("Create Visits");

		Visit visit = visitManager.findVisit(visitID);
		if (visit == null || visit.getVisitID() != updateVisit.getVisitID()) {
			throw new OHAPIException(new OHExceptionMessage("Visit not found."), HttpStatus.NOT_FOUND);
		}

		Visit visitUp = withStoredPatient(mapper.map2Model(updateVisit));
		Visit visitUpdate = visitManager.newVisit(visitUp);
		if (visitUpdate == null) {
			throw new OHAPIException(new OHExceptionMessage("Visit not updated."));
		}

		return mapper.map2DTO(visitUpdate);
	}

	/**
	 * Delete a visit.
	 *
	 * @param visitID the id of the visit
	 * @return {@code true} if the visit has been deleted
	 * @throws OHServiceException When failed to delete the visit
	 */
	@DeleteMapping("/visits/{visitID}")
	public boolean deleteVisit(@PathVariable("visitID") int visitID) throws OHServiceException {
		LOGGER.info("Delete Visit: {}", visitID);
		Visit visit = visitManager.findVisit(visitID);
		if (visit == null) {
			throw new OHAPIException(new OHExceptionMessage("Visit not found."), HttpStatus.NOT_FOUND);
		}
		visitManager.deleteVisit(visit);
		return true;
	}

	/*
	 * The payload's patient may carry only its code: the stored patient is needed for the checks of the visit
	 * (e.g. whether the patient's sex suits the ward).
	 */
	private Visit withStoredPatient(Visit visit) throws OHServiceException {
		Patient patient = visit.getPatient() == null ? null : patientManager.getPatientById(visit.getPatient().getCode());
		if (patient == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}
		visit.setPatient(patient);
		return visit;
	}
}
