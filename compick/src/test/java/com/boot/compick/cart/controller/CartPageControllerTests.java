package com.boot.compick.cart.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CartPageControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authenticatedUserCanViewEmptyCart() throws Exception {
		mockMvc.perform(get("/cart").with(user("cart-user")))
			.andExpect(status().isOk())
			.andExpect(view().name("cart/index"))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("장바구니가 비어 있습니다")
			));
	}
}
