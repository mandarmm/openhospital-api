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
package org.isf.telemetry.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.isf.shared.exceptions.OHAPIException;
import org.isf.telemetry.dto.TelemetryDTO;
import org.isf.telemetry.dto.TelemetryUpdateDTO;
import org.isf.telemetry.envdatacollector.AbstractDataCollector;
import org.isf.telemetry.manager.TelemetryManager;
import org.isf.telemetry.model.Telemetry;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Telemetry settings, as in the Swing telemetry dialog. Only the choice is stored here: the data is sent by the
 * core telemetry daemon.
 */
@RestController
@Tag(name = "Telemetry")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TelemetryController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryController.class);

	/** Always sent while telemetry is enabled (as in the Swing dialog). */
	static final String MANDATORY_CATEGORY = "TEL_ID";

	private final TelemetryManager telemetryManager;

	private final List<AbstractDataCollector> collectors;

	public TelemetryController(TelemetryManager telemetryManager, List<AbstractDataCollector> collectors) {
		this.telemetryManager = telemetryManager;
		this.collectors = collectors;
	}

	@GetMapping("/telemetry")
	public TelemetryDTO getTelemetry() {
		return toDTO(telemetryManager.retrieveSettings());
	}

	@PutMapping("/telemetry")
	public TelemetryDTO updateTelemetry(@RequestBody TelemetryUpdateDTO update) throws OHAPIException {
		Telemetry telemetry;
		if (update.active()) {
			Map<String, Boolean> consent = new HashMap<>(update.consent() != null ? update.consent() : Map.of());
			List<String> unknown = consent.keySet().stream()
				.filter(id -> collectors.stream().noneMatch(collector -> collector.getId().equals(id)))
				.toList();
			if (!unknown.isEmpty()) {
				throw new OHAPIException(new OHExceptionMessage("Unknown telemetry categories: " + String.join(", ", unknown)));
			}
			consent.put(MANDATORY_CATEGORY, true);
			LOGGER.info("Enabling telemetry.");
			telemetry = telemetryManager.enable(consent);
		} else {
			LOGGER.info("Disabling telemetry.");
			telemetry = telemetryManager.disable(new HashMap<>());
		}
		return toDTO(telemetryManager.save(telemetry));
	}

	private TelemetryDTO toDTO(Telemetry telemetry) {
		Map<String, Boolean> consent = telemetry != null && telemetry.getConsentMap() != null ? telemetry.getConsentMap() : Map.of();
		List<TelemetryDTO.Category> categories = collectors.stream()
			.map(collector -> new TelemetryDTO.Category(collector.getId(), collector.getDescription(),
				MANDATORY_CATEGORY.equals(collector.getId()) || collector.isSelected(consent),
				MANDATORY_CATEGORY.equals(collector.getId())))
			.toList();
		if (telemetry == null) {
			return new TelemetryDTO(false, null, null, null, categories);
		}
		return new TelemetryDTO(Boolean.TRUE.equals(telemetry.getActive()), telemetry.getOptinDate(), telemetry.getOptoutDate(),
			telemetry.getSentTimestamp(), categories);
	}
}
