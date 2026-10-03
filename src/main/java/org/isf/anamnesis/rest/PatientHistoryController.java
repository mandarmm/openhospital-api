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
package org.isf.anamnesis.rest;

import java.lang.reflect.UndeclaredThrowableException;

import org.isf.anamnesis.dto.PatientHistoryDTO;
import org.isf.anamnesis.manager.PatientHistoryManager;
import org.isf.anamnesis.mapper.PatientHistoryMapper;
import org.isf.anamnesis.model.PatientHistory;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.utils.exception.OHDataLockFailureException;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * A patient's history (anamnesis, the desktop client's {@code PatientHistoryEdit}): one per patient, under the
 * patients' permissions.
 */
@RestController
@Tag(name = "Patient history")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class PatientHistoryController {

	private static final Logger LOGGER = LoggerFactory.getLogger(PatientHistoryController.class);

	private final PatientHistoryManager patientHistoryManager;
	private final PatientBrowserManager patientManager;
	private final PatientHistoryMapper mapper;

	public PatientHistoryController(PatientHistoryManager patientHistoryManager, PatientBrowserManager patientManager,
		PatientHistoryMapper mapper) {
		this.patientHistoryManager = patientHistoryManager;
		this.patientManager = patientManager;
		this.mapper = mapper;
	}

	/**
	 * The patient's history; a new one (id 0, the desktop client's defaults) when none is recorded yet.
	 */
	@GetMapping("/patients/{code}/history")
	public PatientHistoryDTO getPatientHistory(@PathVariable int code) throws OHServiceException {
		LOGGER.info("Get the history of patient {}.", code);
		checkPatient(code);
		PatientHistory history = patientHistoryManager.getByPatientId(code);
		if (history == null) {
			history = new PatientHistory();
			history.setPatientId(code);
		}
		return mapper.map2DTO(history);
	}

	/**
	 * Stores the patient's history, creating it the first time. The {@code lock} read with it guards against
	 * overwriting someone else's changes (409).
	 */
	@PutMapping(value = "/patients/{code}/history", consumes = MediaType.APPLICATION_JSON_VALUE)
	public PatientHistoryDTO savePatientHistory(@PathVariable int code, @RequestBody PatientHistoryDTO historyDTO) throws OHServiceException {
		LOGGER.info("Save the history of patient {}.", code);
		checkPatient(code);
		PatientHistory history = mapper.map2Model(historyDTO);
		history.setPatientId(code);
		PatientHistory stored = patientHistoryManager.getByPatientId(code);
		// one history per patient, whatever id the client sent
		history.setId(stored == null ? 0 : stored.getId());
		if (stored == null) {
			history.setLock(0);
		}
		try {
			return mapper.map2DTO(patientHistoryManager.saveOrUpdate(history));
		} catch (Exception e) {
			// the core's lock failure is a checked exception its manager does not declare: the proxy wraps it
			Throwable failure = e instanceof UndeclaredThrowableException ? e.getCause() : e;
			if (failure instanceof OHDataLockFailureException || failure instanceof OptimisticLockingFailureException) {
				throw new OHAPIException(new OHExceptionMessage("angal.sql.thedatahasbeenupdatedbysomeoneelse.msg"), HttpStatus.CONFLICT);
			}
			throw e;
		}
	}

	private void checkPatient(int code) throws OHServiceException {
		if (patientManager.getPatientById(code) == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}
	}
}
