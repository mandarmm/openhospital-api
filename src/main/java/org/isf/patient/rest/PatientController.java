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
package org.isf.patient.rest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.isf.admission.manager.AdmissionBrowserManager;
import org.isf.admission.model.Admission;
import org.isf.patconsensus.manager.PatientConsensusBrowserManager;
import org.isf.patconsensus.model.PatientConsensus;
import org.isf.patient.dto.PatientDTO;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.mapper.PatientMapper;
import org.isf.patient.model.Patient;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.shared.pagination.Page;
import org.isf.shared.pagination.PageInfoDTO;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.utils.pagination.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
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
@Tag(name = "Patients")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class PatientController {

	private static final Logger LOGGER = LoggerFactory.getLogger(PatientController.class);

	// TODO: to centralize
	protected static final String DEFAULT_PAGE_SIZE = "80";

	private static final int MAX_FIND_PAGE_SIZE = 200;

	private final PatientBrowserManager patientManager;

	private final AdmissionBrowserManager admissionManager;

	private final PatientConsensusBrowserManager patientConsensusManager;

	private final PatientMapper patientMapper;

	public PatientController(
		PatientBrowserManager patientManager,
		AdmissionBrowserManager admissionManager,
		PatientMapper patientMapper,
		PatientConsensusBrowserManager patientConsensusManager
	) {
		this.patientManager = patientManager;
		this.admissionManager = admissionManager;
		this.patientMapper = patientMapper;
		this.patientConsensusManager = patientConsensusManager;
	}

	/**
	 * Create new {@link Patient}.
	 *
	 * @param newPatient Patient payload
	 * @return The created patient
	 * @throws OHServiceException When failed to create patient
	 */
	@PostMapping(value = "/patients")
	@ResponseStatus(HttpStatus.CREATED)
	public PatientDTO newPatient(@RequestBody PatientDTO newPatient) throws OHServiceException {
		String name = StringUtils.hasLength(newPatient.getName()) ? newPatient.getFirstName() + ' ' + newPatient.getSecondName() : newPatient.getName();
		LOGGER.info("Create patient '{}'.", name);

		// TODO: remove this line when UI will be ready to collect the patient consensus
		newPatient.setConsensusFlag(true);
		if (newPatient.getBlobPhoto() != null && newPatient.getBlobPhoto().length == 0) {
			throw new OHAPIException(new OHExceptionMessage("Malformed picture."));
		}
		Patient patientModel = patientMapper.map2Model(newPatient);
		// the database needs them: "unknown" when the client does not send them, as the desktop client's default
		if (!StringUtils.hasText(patientModel.getMaritalStatus())) {
			patientModel.setMaritalStatus("unknown");
		}
		if (!StringUtils.hasText(patientModel.getProfession())) {
			patientModel.setProfession("unknown");
		}
		Patient patient = patientManager.savePatient(patientModel);

		if (patient == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not created."));
		}

		return patientMapper.map2DTO(patient);
	}

	@PutMapping(value = "/patients/{code}")
	public PatientDTO updatePatient(@PathVariable int code, @RequestBody PatientDTO updatePatient) throws OHServiceException {
		LOGGER.info("Update patient code: '{}'.", code);
		if (!updatePatient.getCode().equals(code)) {
			throw new OHAPIException(new OHExceptionMessage("Patient code mismatch."));
		}
		Patient patientRead = patientManager.getPatientById(code);
		if (patientRead == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}

		Optional<PatientConsensus> patientConsensus = patientConsensusManager.getPatientConsensusByUserId(patientRead.getCode());
		if (patientConsensus.isEmpty()) {
			throw new OHAPIException(new OHExceptionMessage("PatientConsensus not found."));
		}
		if (updatePatient.getBlobPhoto() != null && updatePatient.getBlobPhoto().length == 0) {
			throw new OHAPIException(new OHExceptionMessage("Malformed picture."));
		}
		Patient updatePatientModel = patientMapper.map2Model(updatePatient);
		// clients that do not send them keep the stored values (they were lost on every update)
		if (updatePatient.getMaritalStatus() == null) {
			updatePatientModel.setMaritalStatus(patientRead.getMaritalStatus());
		}
		if (updatePatient.getProfession() == null) {
			updatePatientModel.setProfession(patientRead.getProfession());
		}
		updatePatientModel.getPatientConsensus().setPatient(updatePatientModel);
		updatePatientModel.getPatientConsensus().setId(patientConsensus.get().getId());
		updatePatientModel.setLock(patientRead.getLock());
		Patient patient = patientManager.savePatient(updatePatientModel);
		if (patient == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not updated."));
		}

		return patientMapper.map2DTO(patient);
	}

	@GetMapping(value = "/patients")
	public Page<PatientDTO> getPatients(
		@RequestParam(value = "page", required = false, defaultValue = "0") int page,
		@RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int size
	) throws OHServiceException {
		LOGGER.info("Get patients page: {}  size: {}.", page, size);
		PagedResponse<Patient> patients = patientManager.getPatientsPageable(page, size);

		Page<PatientDTO> patientPageableDTO = new Page<>();
		List<PatientDTO> patientsDTO = patientMapper.map2DTOList(patients.getData());
		patientPageableDTO.setData(patientsDTO);
		patientPageableDTO.setPageInfo(patientMapper.setParameterPageInfo(patients.getPageInfo()));

		return patientPageableDTO;
	}

	@GetMapping(value = "/patients/{code}")
	public PatientDTO getPatient(@PathVariable("code") int code) throws OHServiceException {
		LOGGER.info("Get patient code: '{}'.", code);
		Patient patient = patientManager.getPatientById(code);
		LOGGER.info("Patient retrieved: {}.", patient);
		if (patient == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}

		LOGGER.debug("AdmissionBrowserManager injected: {}.", admissionManager);
		Admission admission = admissionManager.getCurrentAdmission(patient);
		LOGGER.debug("Admission retrieved: {}.", admission);
		Boolean status = admission != null;

		return patientMapper.map2DTOWS(patient, status);
	}

	/**
	 * Finds patients for the reception desk: every word of {@code q} must be found in the code, first or second name,
	 * city, address, telephone, note or tax code (the Swing "search key"); newest patients first. Without {@code q},
	 * all patients are paged. The photo is left out (see {@code GET /patients/{code}}); {@code status} tells whether
	 * the patient is currently admitted.
	 * @param q the words to search for
	 * @param page the page, from 0
	 * @param size the page size, 1 to 200
	 * @return the page of patients
	 * @throws OHServiceException When failed to search patients
	 */
	@GetMapping(value = "/patients/find")
	public Page<PatientDTO> findPatients(
		@RequestParam(value = "q", required = false, defaultValue = "") String q,
		@RequestParam(value = "page", required = false, defaultValue = "0") int page,
		@RequestParam(value = "size", required = false, defaultValue = "20") int size
	) throws OHServiceException {
		if (page < 0 || size < 1 || size > MAX_FIND_PAGE_SIZE) {
			throw new OHAPIException(new OHExceptionMessage("Invalid page or size."));
		}
		List<Patient> patients;
		PageInfoDTO pageInfo;
		if (q.isBlank()) {
			PagedResponse<Patient> paged = patientManager.getPatientsPageable(page, size);
			patients = paged.getData();
			pageInfo = patientMapper.setParameterPageInfo(paged.getPageInfo());
		} else {
			// the search is not paged in the core: page the matches here (a search returns few patients)
			List<Patient> matches = patientManager.getPatientsByOneOfFieldsLike(q.trim());
			int from = Math.min(page * size, matches.size());
			int to = Math.min(from + size, matches.size());
			patients = matches.subList(from, to);
			pageInfo = pageInfo(page, size, patients.size(), matches.size());
		}
		Page<PatientDTO> result = new Page<>();
		result.setData(patients.stream().map(patient -> {
			PatientDTO dto = patientMapper.map2DTOWS(patient, admissionManager.getCurrentAdmission(patient) != null);
			dto.setBlobPhoto(null);
			return dto;
		}).toList());
		result.setPageInfo(pageInfo);
		return result;
	}

	private static PageInfoDTO pageInfo(int page, int size, int elements, long total) {
		PageInfoDTO pageInfo = new PageInfoDTO();
		pageInfo.setPage(page);
		pageInfo.setSize(size);
		pageInfo.setNbOfElements(elements);
		pageInfo.setTotalNbOfElements(total);
		long totalPages = (total + size - 1) / size;
		pageInfo.setTotalPages(totalPages);
		pageInfo.setHasPreviousPage(page > 0);
		pageInfo.setHasNextPage(page + 1 < totalPages);
		return pageInfo;
	}

	@GetMapping(value = "/patients/search")
	public List<PatientDTO> searchPatient(
		@RequestParam(value = "firstName", defaultValue = "", required = false) String firstName,
		@RequestParam(value = "secondName", defaultValue = "", required = false) String secondName,
		@RequestParam(value = "birthDate", defaultValue = "", required = false) LocalDateTime birthDate,
		@RequestParam(value = "address", defaultValue = "", required = false) String address
	) throws OHServiceException {
		Map<String, Object> params = new HashMap<>();

		if (firstName != null && !firstName.isEmpty()) {
			params.put("firstName", firstName);
		}

		if (secondName != null && !secondName.isEmpty()) {
			params.put("secondName", secondName);
		}

		if (birthDate != null) {
			params.put("birthDate", birthDate);
		}

		if (address != null && !address.isEmpty()) {
			params.put("address", address);
		}

		List<Patient> patientList = new ArrayList<>();
		if (!params.entrySet().isEmpty()) {
			patientList = patientManager.getPatients(params);
		}

		return patientList.stream().map(patient -> {
			Admission admission = admissionManager.getCurrentAdmission(patient);
			Boolean status = admission != null;
			return patientMapper.map2DTOWS(patient, status);
		}).toList();
	}

	@GetMapping(value = "/patients/all")
	public PatientDTO getPatientAll(@RequestParam int code) throws OHServiceException {
		LOGGER.info("Get patient for provided code even if logically deleted: '{}'.", code);
		Patient patient = patientManager.getPatientAll(code);
		if (patient == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}

		return patientMapper.map2DTO(patient);
	}

	@GetMapping(value = "/patients/nextcode")
	public Integer getPatientNextCode() throws OHServiceException {
		LOGGER.info("Get patient next code.");
		return patientManager.getNextPatientCode();
	}

	@DeleteMapping(value = "/patients/{code}")
	public boolean deletePatient(@PathVariable int code) throws OHServiceException {
		LOGGER.info("Delete patient code: '{}'.", code);
		Patient patient = patientManager.getPatientById(code);

		if (patient == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}

		try {
			patientManager.deletePatient(patient);
			return true;
		} catch (OHServiceException serviceException) {
			throw new OHAPIException(new OHExceptionMessage("Patient not deleted."));
		}
	}

	/**
	 * Merges patient {@code code2} into patient {@code mergedcode}: {@code code2}'s history goes to {@code mergedcode},
	 * which takes its missing details, and {@code code2} is deleted. It cannot be undone.
	 *
	 * @param mergedcode the patient kept
	 * @param code2 the patient deleted
	 * @param nameOf the code of the patient whose name is kept (the kept patient's by default)
	 */
	@PostMapping(value = "/patients/merge")
	public boolean mergePatients(@RequestParam int mergedcode, @RequestParam int code2, @RequestParam(required = false) Integer nameOf)
		throws OHServiceException {
		LOGGER.info("Merge patient for code '{}' in patient for code '{}'.", code2, mergedcode);
		if (mergedcode == code2) {
			throw new OHAPIException(new OHExceptionMessage("A patient cannot be merged with itself."));
		}
		Patient mergedPatient = patientManager.getPatientById(mergedcode);
		Patient patient2 = patientManager.getPatientById(code2);
		if (mergedPatient == null || patient2 == null) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}
		if (nameOf != null && nameOf == code2) {
			mergedPatient.setFirstName(patient2.getFirstName());
			mergedPatient.setSecondName(patient2.getSecondName());
		}
		// the core's reasons (admitted, pending bills, different sex) reach the client
		patientManager.mergePatient(mergedPatient, patient2);
		return true;
	}

	@GetMapping(value = "/patients/cities")
	public List<String> getPatientCities() throws OHServiceException {
		LOGGER.info("Get all cities of the patients.");

		return patientManager.getCities();
	}
	
	@PostMapping("/patients/by-codes")
    public List<PatientDTO> getPatientByCodes(@RequestBody List<Integer> codes) throws OHServiceException {
		if (codes == null || codes.isEmpty()) {
			throw new OHAPIException(new OHExceptionMessage("The list of patients' codes cannot be empty."));
		}
		List<Patient> patients = patientManager.getPatientByCodes(codes);
		if (patients == null || patients.isEmpty()) {
			throw new OHAPIException(new OHExceptionMessage("Patient not found."), HttpStatus.NOT_FOUND);
		}
		List<PatientDTO> patientsDTO = patientMapper.map2DTOList(patients);
        return patientsDTO;
    }
}
