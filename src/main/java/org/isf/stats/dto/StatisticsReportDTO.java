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
package org.isf.stats.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A statistics report of the installation (a Jasper report in {@code rpt_stat} or {@code rpt_extra}), as the desktop
 * client's Reports menu lists it.
 *
 * @param name the report's file name without extension, the id to run it with
 * @param folder {@code rpt_stat} (OH's statistics) or {@code rpt_extra} (the country's forms)
 * @param title the report's title ({@code jTitle} of its properties), in the requested language when available
 * @param kind {@code period} (from / to dates) or {@code month} (month and year)
 * @param parameters the report's own parameters
 */
@Schema(description = "A statistics report")
public record StatisticsReportDTO(String name, String folder, String title, String kind, List<String> parameters) {

	public static final String PERIOD = "period";
	public static final String MONTH = "month";
}
