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
package org.isf.shared.exceptions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.isf.utils.exception.OHDataValidationException;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Error responses carry the messages, but not the server's stack trace.
 */
class OHResponseEntityExceptionHandlerTest {

	@RestController
	static class FailingController {

		@GetMapping("/fail/validation")
		public String validation() throws OHServiceException {
			throw new OHDataValidationException(new OHExceptionMessage("angal.common.pleaseinsertacode.msg"));
		}

		@GetMapping("/fail/service")
		public String service() throws OHServiceException {
			throw new OHServiceException(new OHExceptionMessage("Something failed."));
		}
	}

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.build();
	}

	@Test
	void validationErrorHasMessageButNoStackTrace() throws Exception {
		mockMvc.perform(get("/fail/validation").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("angal.common.pleaseinsertacode.msg"))
			.andExpect(jsonPath("$.stackTrace").doesNotExist());
	}

	@Test
	void serverErrorHasMessageButNoStackTrace() throws Exception {
		mockMvc.perform(get("/fail/service").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.message").value("Something failed."))
			.andExpect(jsonPath("$.stackTrace").doesNotExist());
	}
}
