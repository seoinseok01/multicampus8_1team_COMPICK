package com.boot.compick.payment.controller;

import java.security.Principal;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
	@PostMapping("/orders/{orderNumber}/cancel")
	public String cancel(Principal principal, @PathVariable String orderNumber,
		@RequestParam(defaultValue = "고객 요청") String reason, RedirectAttributes redirect) {
		try {
			var result = payments.cancel(principal.getName(), orderNumber, reason);
			if (result.pendingOrderDeleted()) {
				redirect.addFlashAttribute("message", "결제 대기 주문을 취소하고 견적서를 장바구니로 되돌렸습니다.");
				return "redirect:/cart";
			}
			redirect.addFlashAttribute("message",
				String.format("주문을 취소하고 %,d원을 환불했습니다.", result.refundedAmount()));
		} catch (TossPaymentException | IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/orders/" + orderNumber;
	}
}
