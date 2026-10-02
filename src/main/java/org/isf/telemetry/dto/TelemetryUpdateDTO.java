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
package org.isf.telemetry.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Change of the telemetry settings, as sent to {@code PUT /telemetry}.
 *
 * @param active  whether telemetry is enabled
 * @param consent consent per category id (see {@link TelemetryDTO.Category#id()}); ignored when disabling
 */
public record TelemetryUpdateDTO(
	@Schema(description = "Whether telemetry is enabled", example = "true") boolean active,
	@Schema(description = "Consent per category id; ignored when disabling") Map<String, Boolean> consent) {
}
