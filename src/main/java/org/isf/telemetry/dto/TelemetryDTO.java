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

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Telemetry settings: whether this installation sends anonymous usage data, and which kinds of data.
 *
 * @param active        whether telemetry is enabled
 * @param optinDate     when it was last enabled
 * @param optoutDate    when it was last disabled
 * @param sentTimestamp when data was last sent
 * @param categories    the kinds of data that can be sent, with the current consent
 */
public record TelemetryDTO(
	@Schema(description = "Whether telemetry is enabled", example = "false") boolean active,
	@Schema(description = "When telemetry was last enabled") LocalDateTime optinDate,
	@Schema(description = "When telemetry was last disabled") LocalDateTime optoutDate,
	@Schema(description = "When data was last sent") LocalDateTime sentTimestamp,
	@Schema(description = "The kinds of data that can be sent, with the current consent") List<Category> categories) {

	/**
	 * @param id          the category id, used as key in the consent
	 * @param description what the category contains
	 * @param selected    whether the category is consented
	 * @param mandatory   whether the category is always sent while telemetry is enabled
	 */
	public record Category(
		@Schema(description = "Category id, used as key in the consent", example = "TEL_ID") String id,
		@Schema(description = "What the category contains", example = "Telemetry Unique ID (this instance)") String description,
		@Schema(description = "Whether the category is consented", example = "true") boolean selected,
		@Schema(description = "Whether the category is always sent while telemetry is enabled", example = "true") boolean mandatory) {
	}
}
