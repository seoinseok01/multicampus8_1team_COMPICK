package com.boot.compick.payment.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.boot.compick.payment.service.TossPaymentException;
import com.boot.compick.payment.service.TossPaymentService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PaymentController {

	private final TossPaymentService tossPaymentService;

	public PaymentController(TossPaymentService tossPaymentService) {
		this.tossPaymentService = tossPaymentService;
	}

	@GetMapping("/payments/success")
	public String success(
		@RequestParam String paymentKey,
		@RequestParam String orderId,
		@RequestParam long amount,
		HttpSession session,
		Model model
	) {
		Object expectedValue = session.getAttribute("pendingPayment:" + orderId);
		if (!(expectedValue instanceof Number expected)
			|| expected.longValue() != amount) {
			model.addAttribute("code", "INVALID_PAYMENT_AMOUNT");
			model.addAttribute("message", "주문 금액이 일치하지 않아 결제를 중단했습니다.");
			return "payment/fail";
		}

		try {
			Map<String, Object> payment = tossPaymentService.confirm(
				paymentKey,
				orderId,
				expected.longValue()
			);
			session.removeAttribute("pendingPayment:" + orderId);
			model.addAttribute("orderId", orderId);
			model.addAttribute("amount", expected.longValue());
			model.addAttribute("method", payment.get("method"));
			model.addAttribute("status", payment.get("status"));
			return "payment/success";
		} catch (TossPaymentException exception) {
			model.addAttribute("code", "PAYMENT_CONFIRM_FAILED");
			model.addAttribute("message", exception.getMessage());
			return "payment/fail";
		}
	}

	@GetMapping("/payments/fail")
	public String fail(
		@RequestParam(defaultValue = "PAYMENT_FAILED") String code,
		@RequestParam(defaultValue = "결제가 완료되지 않았습니다.") String message,
		@RequestParam(required = false) String orderId,
		Model model
	) {
		model.addAttribute("code", code);
		model.addAttribute("message", message);
		model.addAttribute("orderId", orderId);
		return "payment/fail";
	}
}
