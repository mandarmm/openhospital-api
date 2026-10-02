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
package org.isf.priceslist.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.isf.priceslist.manager.PriceListManager;
import org.isf.priceslist.mapper.PriceListMapper;
import org.isf.priceslist.mapper.PriceMapper;
import org.isf.priceslist.model.Price;
import org.isf.priceslist.model.PriceList;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * {@code GET} and {@code PUT /pricelists/{id}/prices}.
 */
class PriceUpdateTest {

	@Mock
	private PriceListManager managerMock;

	private final PriceListMapper listMapper = new PriceListMapper();
	private final PriceMapper priceMapper = new PriceMapper(listMapper);

	private final PriceList basic = new PriceList(1, "LIST001", "Basic", "Basic list", "EUR");
	private final PriceList other = new PriceList(2, "LIST002", "Other", "Other list", "EUR");

	private MockMvc mockMvc;

	private AutoCloseable closeable;

	@BeforeEach
	void setup() throws Exception {
		closeable = MockitoAnnotations.openMocks(this);
		ModelMapper modelMapper = new ModelMapper();
		ReflectionTestUtils.setField(listMapper, "modelMapper", modelMapper);
		ReflectionTestUtils.setField(priceMapper, "modelMapper", modelMapper);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
			.standaloneSetup(new PriceListController(managerMock, listMapper, priceMapper))
			.setControllerAdvice(new OHResponseEntityExceptionHandler())
			.setValidator(validator)
			.build();
		when(managerMock.getLists()).thenReturn(List.of(basic, other));
		when(managerMock.getPrices()).thenReturn(List.of(
			new Price(10, basic, "EXA", "01.01", "HB", new BigDecimal("10.00")),
			new Price(11, other, "EXA", "01.01", "HB", new BigDecimal("12.00"))));
	}

	@AfterEach
	void closeService() throws Exception {
		closeable.close();
	}

	@Test
	void getPricesOfOneList() throws Exception {
		mockMvc.perform(get("/pricelists/1/prices"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].price").value(10.0))
			.andExpect(jsonPath("$[0].description").value("HB"))
			.andExpect(jsonPath("$[0].list.id").value(1));
	}

	@Test
	void putReplacesThePricesOfTheList() throws Exception {
		mockMvc.perform(put("/pricelists/1/prices")
				.contentType(MediaType.APPLICATION_JSON)
				.content("[{\"group\":\"EXA\",\"item\":\"01.01\",\"description\":\"HB\",\"price\":15.5},"
					+ "{\"group\":\"OPE\",\"item\":\"45\",\"price\":200}]"))
			.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Price>> saved = ArgumentCaptor.forClass(List.class);
		verify(managerMock).updatePrices(eq(basic), saved.capture());
		assertThat(saved.getValue()).hasSize(2);
		assertThat(saved.getValue()).allMatch(price -> price.getId() == 0 && price.getList() == basic);
		assertThat(saved.getValue().get(0).getPrice()).isEqualByComparingTo("15.5");
		assertThat(saved.getValue().get(1).getDesc()).isEmpty();
	}

	@Test
	void unknownListIsNotFound() throws Exception {
		mockMvc.perform(put("/pricelists/99/prices")
				.contentType(MediaType.APPLICATION_JSON)
				.content("[]"))
			.andExpect(status().isNotFound());
		verify(managerMock, never()).updatePrices(any(), anyList());
	}

	@Test
	void invalidPricesAreRejected() throws Exception {
		mockMvc.perform(put("/pricelists/1/prices")
				.contentType(MediaType.APPLICATION_JSON)
				.content("[{\"group\":\"XXX\",\"item\":\"01.01\",\"price\":10}]"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(put("/pricelists/1/prices")
				.contentType(MediaType.APPLICATION_JSON)
				.content("[{\"group\":\"EXA\",\"item\":\"01.01\",\"price\":-1}]"))
			.andExpect(status().isBadRequest());
		verify(managerMock, never()).updatePrices(any(), anyList());
	}
}
