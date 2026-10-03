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
package org.isf.dicom.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.isf.OpenHospitalApiApplication;
import org.isf.patient.data.PatientHelper;
import org.isf.patient.manager.PatientBrowserManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The images' permissions and the checks made before the core's loader (which works on real files).
 */
@SpringBootTest(classes = OpenHospitalApiApplication.class)
@AutoConfigureMockMvc
class DicomControllerTest {

	@Autowired
	private MockMvc mvc;
	@MockitoBean
	private PatientBrowserManager patientBrowserManager;

	private static MockMultipartFile file(String name) {
		return new MockMultipartFile("file", name, "application/octet-stream", new byte[] { 1, 2, 3 });
	}

	@Test
	@WithMockUser(username = "admin", authorities = { "patients.update" })
	void only_dicom_and_jpeg_files_are_loaded() throws Exception {
		when(patientBrowserManager.getPatientById(7)).thenReturn(PatientHelper.setup());
		mvc.perform(multipart("/dicom/patients/{code}", 7).file(file("report.pdf")))
			.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "admin", authorities = { "patients.update", "patients.read" })
	void an_unknown_patient_is_404() throws Exception {
		mvc.perform(multipart("/dicom/patients/{code}", 8).file(file("image.jpg"))).andExpect(status().isNotFound());
		mvc.perform(get("/dicom/patients/{code}", 8)).andExpect(status().isNotFound());
		mvc.perform(get("/dicom/patients/{code}/series/{series}/files/{id}", 8, "1", 1)).andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "reader", authorities = { "patients.read" })
	void loading_needs_patients_update() throws Exception {
		mvc.perform(multipart("/dicom/patients/{code}", 7).file(file("image.jpg"))).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "editor", authorities = { "patients.read", "patients.update" })
	void deleting_a_series_needs_patients_delete() throws Exception {
		mvc.perform(delete("/dicom/patients/{code}/series/{series}", 7, "1")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "nobody", authorities = { "exams.read" })
	void viewing_needs_patients_read() throws Exception {
		mvc.perform(get("/dicom/patients/{code}", 7)).andExpect(status().isForbidden());
	}
}
