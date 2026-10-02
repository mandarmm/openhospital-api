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
package org.isf.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A Swing menu item the current user sees (see {@code GET /users/me/menu}).
 *
 * @param code     menu item id (OH_MENUITEM.MNI_ID_A)
 * @param label    OH message key of the label
 * @param submenu  id of the parent menu
 * @param aSubmenu whether the item opens a submenu
 * @param position position in the parent menu
 */
public record UserMenuItemDTO(
	@Schema(description = "Menu item id", example = "ward") String code,
	@Schema(description = "OH message key of the label", example = "angal.menu.ward") String label,
	@Schema(description = "Id of the parent menu", example = "generaldata") String submenu,
	@Schema(description = "Whether the item opens a submenu", example = "false") boolean aSubmenu,
	@Schema(description = "Position in the parent menu", example = "2") int position) {
}
