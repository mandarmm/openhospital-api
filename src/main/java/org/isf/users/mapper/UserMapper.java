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
package org.isf.users.mapper;

import java.util.List;

import org.isf.menu.model.User;
import org.isf.shared.GenericMapper;
import org.isf.users.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserMapper extends GenericMapper<User, UserDTO> {
	@Autowired
	private PasswordEncoder passwordEncoder;

	public UserMapper() {
		super(User.class, UserDTO.class);
	}

	@Override
	public UserDTO map2DTO(User user) {
		UserDTO dto = super.map2DTO(user);
		dto.setAccountLocked(user.isAccountLocked());
		dto.setFailedAttempts(user.getFailedAttempts());
		dto.setLockedTime(user.getLockedTime());
		dto.setLastLogin(user.getLastLogin());
		return dto;
	}

	@Override
	public List<UserDTO> map2DTOList(List<User> list) {
		return list.stream().map(this::map2DTO).toList();
	}

	/**
	 * The lock status is never taken from a request: it is managed by the login and {@code POST /users/{username}/unlock}.
	 */
	@Override
	public User map2Model(UserDTO dto) {
		var user = super.map2Model(dto);
		user.setAccountLocked(false);
		user.setFailedAttempts(0);
		user.setLockedTime(null);
		user.setLastLogin(null);
		if (dto.getPasswd() != null && !dto.getPasswd().isEmpty()) {
			user.setPasswd(passwordEncoder.encode(user.getPasswd()));
		}
		return user;
	}
}
