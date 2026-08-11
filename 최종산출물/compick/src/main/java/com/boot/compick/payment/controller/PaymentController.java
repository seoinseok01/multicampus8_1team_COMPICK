package com.boot.compick.payment.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.cart.repository.CartMemberLookupRepository;
import com.boot.compick.payment.service.PaymentService;

@Controller
public class PaymentController {

	private final PaymentService paymentService;
	private final CartMemberLookupRepository memberLookupRepository;

	public PaymentController(
		PaymentService paymentService,
		CartMemberLookupRepository memberLookupRepository
	) {
		this.paymentService = paymentService;
		this.memberLookupRepository = memberLookupRepository;
	}

	@GetMapping("/payments/success")
	public String tossSuccess(
		@RequestParam String paymentKey,
		@RequestParam String orderId,
		@RequestParam long amount,
		Principal principal,
		Model model
	) {
		try {
			Long memberId = memberLookupRepository.findActiveMemberIdByLoginId(principal.getName())
				.orElseThrow();
			paymentService.confirmToss(principal.getName(), orderId, paymentKey, amount, memberId);
			model.addAttribute("orderNumber", orderId);
			model.addAttribute("method", "토스페이먼츠");
			return "payment/success";
		} catch (ResponseStatusException | java.util.NoSuchElementException exception) {
			model.addAttribute("code", "PAYMENT_CONFIRM_FAILED");
			model.addAttribute("message", messageOf(exception));
			model.addAttribute("orderNumber", orderId);
			return "payment/fail";
		}
	}

	@GetMapping("/payments/fail")
	public String tossFail(
		@RequestParam(defaultValue = "PAYMENT_FAILED") String code,
		@RequestParam(defaultValue = "결제가 완료되지 않았습니다.") String message,
		@RequestParam(required = false) String orderId,
		Model model
	) {
		model.addAttribute("code", code);
		model.addAttribute("message", message);
		model.addAttribute("orderNumber", orderId);
		return "payment/fail";
	}

	private String messageOf(Exception exception) {
		if (exception instanceof ResponseStatusException statusException) {
			return statusException.getReason();
		}
		return "결제 처리 중 오류가 발생했습니다.";
	}
}
