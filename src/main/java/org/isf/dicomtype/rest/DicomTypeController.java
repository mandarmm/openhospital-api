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
package org.isf.dicomtype.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.isf.dicomtype.dto.DicomTypeDTO;
import org.isf.dicomtype.manager.DicomTypeBrowserManager;
import org.isf.dicomtype.mapper.DicomTypeMapper;
import org.isf.dicomtype.model.DicomType;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * DICOM (medical imaging) file types. Validation errors of the core (empty or duplicate code, ...) are returned
 * as they are.
 */
@RestController
@Tag(name = "DICOM Type")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class DicomTypeController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DicomTypeController.class);

	private final DicomTypeBrowserManager dicomTypeManager;

	private final DicomTypeMapper mapper;

	public DicomTypeController(DicomTypeBrowserManager dicomTypeManager, DicomTypeMapper mapper) {
		this.dicomTypeManager = dicomTypeManager;
		this.mapper = mapper;
	}

	@GetMapping("/dicomtypes")
	public List<DicomTypeDTO> getDicomTypes() throws OHServiceException {
		LOGGER.info("Get DICOM types.");
		return mapper.map2DTOList(dicomTypeManager.getDicomType());
	}

	@PostMapping("/dicomtypes")
	@ResponseStatus(HttpStatus.CREATED)
	public DicomTypeDTO newDicomType(@RequestBody @Valid DicomTypeDTO dicomType) throws OHServiceException {
		LOGGER.info("Create DICOM type: {}", dicomType.code());
		return mapper.map2DTO(dicomTypeManager.newDicomType(mapper.map2Model(dicomType)));
	}

	@PutMapping("/dicomtypes/{code}")
	public DicomTypeDTO updateDicomType(@PathVariable String code, @RequestBody @Valid DicomTypeDTO dicomType) throws OHServiceException {
		LOGGER.info("Update DICOM type: {}", code);
		if (!code.equals(dicomType.code())) {
			throw new OHAPIException(new OHExceptionMessage("DICOM type code mismatch."));
		}
		findDicomType(code);
		return mapper.map2DTO(dicomTypeManager.updateDicomType(mapper.map2Model(dicomType)));
	}

	@DeleteMapping("/dicomtypes/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDicomType(@PathVariable String code) throws OHServiceException {
		LOGGER.info("Delete DICOM type: {}", code);
		dicomTypeManager.deleteDicomType(findDicomType(code));
	}

	@GetMapping("/dicomtypes/check/{code}")
	public boolean isCodePresent(@PathVariable String code) throws OHServiceException {
		return dicomTypeManager.isCodePresent(code);
	}

	private DicomType findDicomType(String code) throws OHServiceException {
		return dicomTypeManager.getDicomType().stream()
			.filter(type -> type.getDicomTypeID().equals(code))
			.findFirst()
			.orElseThrow(() -> new OHAPIException(new OHExceptionMessage("DICOM type not found."), HttpStatus.NOT_FOUND));
	}
}
