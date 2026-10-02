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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.isf.menu.manager.UserBrowsingManager;
import org.isf.menu.model.User;
import org.isf.menu.model.UserGroup;
import org.isf.permissions.manager.PermissionManager;
import org.isf.permissions.mapper.PermissionMapper;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.usergroups.mapper.UserGroupMapper;
import org.isf.users.dto.UserDTO;
import org.isf.users.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Account lock status and {@code POST /users/{username}/unlock}.
 */
class UserUnlockTest {

	@Mock
	private UserBrowsingManager userManagerMock;
	@Mock
	private PermissionManager permissionManagerMock;

	private final UserMapper userMapper = new UserMapper();

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		ReflectionTestUtils.setField(userMapper, "modelMapper", modelMapper);
		ReflectionTestUtils.setField(userMapper, "passwordEncoder", new BCryptPasswordEncoder());
		UserGroupMapper userGroupMapper = new UserGroupMapper();
		ReflectionTestUtils.setField(userGroupMapper, "modelMapper", modelMapper);
		PermissionMapper permissionMapper = new PermissionMapper();
		ReflectionTestUtils.setField(permissionMapper, "modelMapper", modelMapper);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(permissionManagerMock, permissionMapper, userMapper, userGroupMapper, userManagerMock))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	private static User lockedUser() {
		User user = new User("nurse", new UserGroup("guest", "Guests"), "secret", "Nurse");
		user.setAccountLocked(true);
		user.setFailedAttempts(5);
		return user;
	}

	@Test
	void listShowsLockStatusButNotPassword() throws Exception {
		when(userManagerMock.getUser()).thenReturn(List.of(lockedUser()));

		mockMvc.perform(get("/users"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].accountLocked").value(true))
			.andExpect(jsonPath("$[0].failedAttempts").value(5));
	}

	@Test
	void unlockUnlocksTheAccount() throws Exception {
		User locked = lockedUser();
		User unlocked = new User("nurse", new UserGroup("guest", "Guests"), "secret", "Nurse");
		when(userManagerMock.getUserByName("nurse", true)).thenReturn(locked, unlocked);

		mockMvc.perform(post("/users/nurse/unlock"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userName").value("nurse"))
			.andExpect(jsonPath("$.accountLocked").value(false))
			.andExpect(jsonPath("$.failedAttempts").value(0))
			.andExpect(jsonPath("$.passwd").doesNotExist());
		verify(userManagerMock).unlockUser(locked);
	}

	@Test
	void unlockUnknownUserIsNotFound() throws Exception {
		mockMvc.perform(post("/users/nobody/unlock"))
			.andExpect(status().isNotFound());
	}

	@Test
	void lockStatusIsNotTakenFromRequests() throws Exception {
		UserDTO dto = new ObjectMapper().readValue(
			"{\"userName\":\"nurse\",\"passwd\":\"Secret#1\",\"accountLocked\":true,\"failedAttempts\":9}", UserDTO.class);
		assertThat(dto.getAccountLocked()).isNull();

		User model = userMapper.map2Model(dto);
		assertThat(model.isAccountLocked()).isFalse();
		assertThat(model.getFailedAttempts()).isZero();
	}
}
