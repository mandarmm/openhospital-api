/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2026 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.version.rest;

import org.isf.generaldata.Version;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Open Hospital's version ({@code version.properties}), as the desktop client shows it and uses it to open the user
 * manual of the installed version.
 */
@RestController
@Tag(name = "Version")
@SecurityRequirement(name = "bearerAuth")
public class VersionController {

	public record VersionDTO(String major, String minor, String release, String version) {
	}

	@GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
	public VersionDTO getVersion() {
		// reads version.properties the first time
		String version = Version.getVersion().toString();
		return new VersionDTO(Version.VER_MAJOR, Version.VER_MINOR, Version.VER_RELEASE, version);
	}
}
