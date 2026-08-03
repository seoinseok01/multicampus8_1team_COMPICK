package com.boot.compick.payment.controller;

import java.security.Principal;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.boot.compick.payment.service.*;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PaymentController {
	private final PaymentApplicationService payments;

	@GetMapping("/payments/success")
	public String success(Principal principal, @RequestParam String paymentKey, @RequestParam String orderId,
		@RequestParam long amount, Model model) {
		try {
			Map<String,Object> result = payments.confirm(principal.getName(), paymentKey, orderId, amount);
			model.addAttribute("orderId", orderId); model.addAttribute("amount", amount);
			model.addAttribute("method", result.get("method")); model.addAttribute("status", result.get("status"));
			return "payment/success";
		} catch (TossPaymentException | IllegalArgumentException e) {
			model.addAttribute("code", "PAYMENT_CONFIRM_FAILED"); model.addAttribute("message", e.getMessage());
			model.addAttribute("orderId", orderId); return "payment/fail";
		}
	}
	@GetMapping("/payments/fail")
	public String fail(@RequestParam(defaultValue="PAYMENT_FAILED") String code,
		@RequestParam(defaultValue="결제가 완료되지 않았습니다.") String message,
		@RequestParam(required=false) String orderId, Model model) {
		model.addAttribute("code", code); model.addAttribute("message", message); model.addAttribute("orderId", orderId);
		return "payment/fail";
	}
}
