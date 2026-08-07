package com.boot.compick;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminPageIntegrationTests {

	@Autowired
	MockMvc mockMvc;

	@ParameterizedTest
	@ValueSource(strings = { "/admin", "/admin/members", "/admin/products", "/admin/presets", "/admin/presets/new", "/admin/orders", "/admin/ai-logs" })
	void adminListPagesRender(String path) throws Exception {
		mockMvc.perform(get(path).with(user("admin").roles("ADMIN")))
			.andExpect(status().isOk());
	}

	@ParameterizedTest
	@ValueSource(strings = { "/admin", "/admin/members", "/admin/products" })
	void regularMemberCannotAccessAdmin(String path) throws Exception {
		mockMvc.perform(get(path).with(user("member").roles("USER")))
			.andExpect(status().isForbidden());
	}
}
