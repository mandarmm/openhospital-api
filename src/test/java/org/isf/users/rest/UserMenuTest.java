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
package org.isf.users.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.isf.menu.manager.UserBrowsingManager;
import org.isf.menu.model.User;
import org.isf.menu.model.UserGroup;
import org.isf.menu.model.UserMenuItem;
import org.isf.permissions.manager.PermissionManager;
import org.isf.permissions.mapper.PermissionMapper;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.usergroups.mapper.UserGroupMapper;
import org.isf.users.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@code GET /users/me/menu}.
 */
class UserMenuTest {

	@Mock
	private UserBrowsingManager userManagerMock;
	@Mock
	private PermissionManager permissionManagerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		UserMapper userMapper = new UserMapper();
		ReflectionTestUtils.setField(userMapper, "modelMapper", modelMapper);
		UserGroupMapper userGroupMapper = new UserGroupMapper();
		ReflectionTestUtils.setField(userGroupMapper, "modelMapper", modelMapper);
		PermissionMapper permissionMapper = new PermissionMapper();
		ReflectionTestUtils.setField(permissionMapper, "modelMapper", modelMapper);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(permissionManagerMock, permissionMapper, userMapper, userGroupMapper, userManagerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("nurse", null, List.of()));
	}

	@AfterEach
	void closeService() throws Exception {
		SecurityContextHolder.clearContext();
		closeable.close();
	}

	private static UserMenuItem item(String code, boolean active) {
		return new UserMenuItem(code, "angal.menu.btn." + code, "angal.menu." + code, "x", 'X', "main", "none", false, 1, active);
	}

	@Test
	void returnsOnlyTheItemsTheUserSees() throws Exception {
		User nurse = new User("nurse", new UserGroup("guest", "Guests"), "secret", "Nurse");
		when(userManagerMock.getUserByName("nurse")).thenReturn(nurse);
		when(userManagerMock.getMenu(nurse)).thenReturn(List.of(item("opd", true), item("admission", false), item("ward", true)));

		mockMvc.perform(get("/users/me/menu"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].code").value("opd"))
			.andExpect(jsonPath("$[0].label").value("angal.menu.opd"))
			.andExpect(jsonPath("$[1].code").value("ward"));
	}

	@Test
	void unknownUserIsNotFound() throws Exception {
		mockMvc.perform(get("/users/me/menu"))
			.andExpect(status().isNotFound());
	}
}
