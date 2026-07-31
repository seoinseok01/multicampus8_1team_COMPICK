package com.boot.compick.payment.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void manipulatedAmountIsRejectedBeforeConfirmation() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("pendingPayment:ORDER-1", 10_000);

		mockMvc.perform(get("/payments/success")
				.param("paymentKey", "test-payment-key")
				.param("orderId", "ORDER-1")
				.param("amount", "100")
				.session(session)
				.with(user("customer")))
			.andExpect(status().isOk())
			.andExpect(view().name("payment/fail"))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("주문 금액이 일치하지 않아")
			));
	}

	@Test
	void paymentFailurePageIsRendered() throws Exception {
		mockMvc.perform(get("/payments/fail")
				.param("code", "PAY_PROCESS_CANCELED")
				.param("message", "결제가 취소되었습니다.")
				.with(user("customer")))
			.andExpect(status().isOk())
			.andExpect(view().name("payment/fail"))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("PAY_PROCESS_CANCELED")
			));
	}
}
