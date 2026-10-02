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
package org.isf.usergroups.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.isf.menu.manager.UserBrowsingManager;
import org.isf.menu.model.UserGroup;
import org.isf.menu.model.UserMenuItem;
import org.isf.permissions.manager.GroupPermissionManager;
import org.isf.permissions.manager.PermissionManager;
import org.isf.permissions.mapper.PermissionMapper;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.usergroups.mapper.UserGroupMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@code GET} and {@code PUT /usergroups/{group_code}/menu}.
 */
class GroupMenuTest {

	@Mock
	private UserBrowsingManager userManagerMock;
	@Mock
	private PermissionManager permissionManagerMock;
	@Mock
	private GroupPermissionManager groupPermissionManagerMock;

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		UserGroupMapper userGroupMapper = new UserGroupMapper();
		ReflectionTestUtils.setField(userGroupMapper, "modelMapper", modelMapper);
		PermissionMapper permissionMapper = new PermissionMapper();
		ReflectionTestUtils.setField(permissionMapper, "modelMapper", modelMapper);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserGroupController(permissionManagerMock, groupPermissionManagerMock, userGroupMapper, permissionMapper,
				userManagerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
		when(userManagerMock.findUserGroupByCode("guest")).thenReturn(new UserGroup("guest", "Guests"));
		when(userManagerMock.findUserGroupByCode("admin")).thenReturn(new UserGroup("admin", "Admins"));
		// a fresh list on every call, as the database would return
		when(userManagerMock.getGroupMenu(argThat(group -> group != null && "admin".equals(group.getCode()))))
			.thenAnswer(invocation -> adminMenu());
		when(userManagerMock.getGroupMenu(argThat(group -> group != null && "guest".equals(group.getCode()))))
			.thenAnswer(invocation -> new ArrayList<>(List.of(item("ward", true))));
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private static UserMenuItem item(String code, boolean active) {
		return new UserMenuItem(code, "angal.menu.btn." + code, "angal.menu." + code, "x", 'X', "generaldata", "none", false, 1, active);
	}

	private static List<UserMenuItem> adminMenu() {
		return new ArrayList<>(List.of(item("ward", true), item("disease", true), item("users", true), item("exit", true)));
	}

	@Test
	void groupMenuListsAllItemsWithGroupFlags() throws Exception {
		mockMvc.perform(get("/usergroups/guest/menu"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(4))
			.andExpect(jsonPath("$[0].code").value("ward"))
			.andExpect(jsonPath("$[0].active").value(true))
			.andExpect(jsonPath("$[0].label").value("angal.menu.ward"))
			.andExpect(jsonPath("$[1].code").value("disease"))
			.andExpect(jsonPath("$[1].active").value(false));
	}

	@Test
	void updateStoresEveryItemWithItsFlag() throws Exception {
		mockMvc.perform(put("/usergroups/guest/menu")
				.contentType(MediaType.APPLICATION_JSON)
				.content("[{\"code\":\"disease\",\"active\":true}]"))
			.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<UserMenuItem>> saved = ArgumentCaptor.forClass(List.class);
		verify(userManagerMock).setGroupMenu(argThat(group -> "guest".equals(group.getCode())), saved.capture());
		Map<String, Boolean> flags = saved.getValue().stream().collect(Collectors.toMap(UserMenuItem::getCode, UserMenuItem::isActive));
		assertThat(flags).containsEntry("disease", true).containsEntry("ward", false).containsEntry("users", false).hasSize(4);
	}

	@Test
	void adminKeepsUserManagementAndExit() throws Exception {
		mockMvc.perform(put("/usergroups/admin/menu")
				.contentType(MediaType.APPLICATION_JSON)
				.content("[{\"code\":\"users\",\"active\":false},{\"code\":\"exit\",\"active\":false}]"))
			.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<UserMenuItem>> saved = ArgumentCaptor.forClass(List.class);
		verify(userManagerMock).setGroupMenu(argThat(group -> "admin".equals(group.getCode())), saved.capture());
		Map<String, Boolean> flags = saved.getValue().stream().collect(Collectors.toMap(UserMenuItem::getCode, UserMenuItem::isActive));
		assertThat(flags).containsEntry("users", true).containsEntry("exit", true).containsEntry("ward", false);
	}

	@Test
	void unknownMenuItemIsRejected() throws Exception {
		mockMvc.perform(put("/usergroups/guest/menu")
				.contentType(MediaType.APPLICATION_JSON)
				.content("[{\"code\":\"nosuchitem\",\"active\":true}]"))
			.andExpect(status().isBadRequest());
		verify(userManagerMock, never()).setGroupMenu(any(), anyList());
	}

	@Test
	void unknownGroupIsNotFound() throws Exception {
		mockMvc.perform(get("/usergroups/nobody/menu"))
			.andExpect(status().isNotFound());
	}
}
