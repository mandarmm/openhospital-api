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
package org.isf.users.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import org.isf.usergroups.dto.UserGroupDTO;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

public class UserDTO {

	@NotNull
	@Schema(description = "The username (must be unique)", example = "John Doe", maxLength = 50)
	private String userName;

	@NotNull
	@Schema(description = "The user's group")
	private UserGroupDTO userGroupName;

	@NotNull
	@Schema(description = "The user's password", example = "21@U2g423", maxLength = 50)
	private String passwd;

	@Schema(description = "The user's description", example = "Lab chief technician", maxLength = 128)
	private String desc;
	@Schema(description = "Whether the user has been soft deleted or not", example = "false")
	private boolean deleted;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@Schema(description = "Whether the account is locked after too many failed logins (read-only; see POST /users/{username}/unlock)",
		example = "false", accessMode = Schema.AccessMode.READ_ONLY)
	private Boolean accountLocked;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@Schema(description = "Failed login attempts since the last successful login (read-only)", example = "0",
		accessMode = Schema.AccessMode.READ_ONLY)
	private Integer failedAttempts;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@Schema(description = "When the account was locked (read-only)", accessMode = Schema.AccessMode.READ_ONLY)
	private LocalDateTime lockedTime;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@Schema(description = "Last successful login (read-only)", accessMode = Schema.AccessMode.READ_ONLY)
	private LocalDateTime lastLogin;

	public UserDTO() {
	}

	public UserDTO(String userName, UserGroupDTO userGroupName, String passwd, String desc) {
		this(userName, userGroupName, passwd, desc, false);
	}

	public UserDTO(String userName, UserGroupDTO userGroupName, String passwd, String desc, boolean deleted) {
		this.userName = userName;
		this.userGroupName = userGroupName;
		this.passwd = passwd;
		this.desc = desc;
		this.deleted = deleted;
	}

	public String getUserName() {
		return this.userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public UserGroupDTO getUserGroupName() {
		return this.userGroupName;
	}
	public void setUserGroupName(UserGroupDTO userGroupName) {
		this.userGroupName = userGroupName;
	}
	public String getPasswd() {
		return this.passwd;
	}
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	public String getDesc() {
		return this.desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}

	public boolean isDeleted() {
		return deleted;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public Boolean getAccountLocked() {
		return accountLocked;
	}

	public void setAccountLocked(Boolean accountLocked) {
		this.accountLocked = accountLocked;
	}

	public Integer getFailedAttempts() {
		return failedAttempts;
	}

	public void setFailedAttempts(Integer failedAttempts) {
		this.failedAttempts = failedAttempts;
	}

	public LocalDateTime getLockedTime() {
		return lockedTime;
	}

	public void setLockedTime(LocalDateTime lockedTime) {
		this.lockedTime = lockedTime;
	}

	public LocalDateTime getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(LocalDateTime lastLogin) {
		this.lastLogin = lastLogin;
	}
}
