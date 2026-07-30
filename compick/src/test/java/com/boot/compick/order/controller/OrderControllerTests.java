package com.boot.compick.order.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authenticatedUserCanViewOrderForm() throws Exception {
		mockMvc.perform(get("/orders/new").with(user("customer")))
			.andExpect(status().isOk())
			.andExpect(view().name("order/form"))
			.andExpect(model().attribute("totalAmount", 1_918_000))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("주문서 작성")));
	}

	@Test
	void anonymousUserIsRedirectedToLogin() throws Exception {
		mockMvc.perform(get("/orders/new"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("**/members/login"));
	}
}
