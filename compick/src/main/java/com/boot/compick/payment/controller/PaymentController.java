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

	@GetMapping("/payments/kakao/approve")
	public String kakaoApprove(
		@RequestParam String orderNumber,
		@RequestParam("pg_token") String pgToken,
		Principal principal,
		Model model
	) {
		try {
			Long memberId = memberLookupRepository.findActiveMemberIdByLoginId(principal.getName())
				.orElseThrow();
			paymentService.approveKakao(principal.getName(), orderNumber, pgToken, memberId);
			model.addAttribute("orderNumber", orderNumber);
			model.addAttribute("method", "카카오페이");
			return "payment/success";
		} catch (ResponseStatusException | java.util.NoSuchElementException exception) {
			model.addAttribute("code", "PAYMENT_CONFIRM_FAILED");
			model.addAttribute("message", messageOf(exception));
			model.addAttribute("orderNumber", orderNumber);
			return "payment/fail";
		}
	}

	@GetMapping("/payments/kakao/cancel")
	public String kakaoCancel(
		@RequestParam String orderNumber,
		Principal principal,
		Model model
	) {
		markKakaoNotApproved(orderNumber, principal, true);
		model.addAttribute("code", "PAYMENT_CANCELLED");
		model.addAttribute("message", "결제를 취소했습니다.");
		model.addAttribute("orderNumber", orderNumber);
		return "payment/fail";
	}

	@GetMapping("/payments/kakao/fail")
	public String kakaoFail(
		@RequestParam String orderNumber,
		Principal principal,
		Model model
	) {
		markKakaoNotApproved(orderNumber, principal, false);
		model.addAttribute("code", "PAYMENT_FAILED");
		model.addAttribute("message", "결제가 완료되지 않았습니다.");
		model.addAttribute("orderNumber", orderNumber);
		return "payment/fail";
	}

	@GetMapping("/payments/toss/success")
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

	@GetMapping("/payments/toss/fail")
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

	private void markKakaoNotApproved(String orderNumber, Principal principal, boolean cancelled) {
		try {
			Long memberId = memberLookupRepository.findActiveMemberIdByLoginId(principal.getName())
				.orElseThrow();
			paymentService.markKakaoNotApproved(orderNumber, memberId, cancelled);
		} catch (RuntimeException ignored) {
			// 이미 처리된 주문이거나 존재하지 않으면 실패 화면만 보여주고 넘어간다.
		}
	}

	private String messageOf(Exception exception) {
		if (exception instanceof ResponseStatusException statusException) {
			return statusException.getReason();
		}
		return "결제 처리 중 오류가 발생했습니다.";
	}
}
