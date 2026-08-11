package com.boot.compick.order.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.boot.compick.order.entity.OrderStatus;
import com.boot.compick.order.service.OrderService;
import com.boot.compick.payment.service.PaymentService;

@Controller
public class OrderPageController {

	private final OrderService orderService;
	private final PaymentService paymentService;

	public OrderPageController(OrderService orderService, PaymentService paymentService) {
		this.orderService = orderService;
		this.paymentService = paymentService;
	}

	@GetMapping("/orders/new")
	public String checkout(Principal principal, Model model) {
		model.addAttribute("tossConfigured", paymentService.isTossConfigured());
		model.addAttribute("tossClientKey", paymentService.tossClientKey());
		model.addAttribute("customerKey", customerKeyFor(principal.getName()));
		return "order/new";
	}

	private String customerKeyFor(String loginId) {
		return "CUSTOMER-" + java.util.UUID.nameUUIDFromBytes(
			loginId.getBytes(java.nio.charset.StandardCharsets.UTF_8)
		).toString().replace("-", "");
	}

	@GetMapping("/orders")
	public String list(
		@RequestParam(required = false) String status,
		Principal principal,
		Model model
	) {
		OrderStatus statusFilter = parseStatus(status);
		model.addAttribute("orders", orderService.findOrders(principal.getName(), statusFilter));
		model.addAttribute("activeStatus", statusFilter == null ? "" : statusFilter.name());
		return "order/list";
	}

	@GetMapping("/orders/{orderNumber}")
	public String detail(
		@PathVariable String orderNumber,
		Principal principal,
		Model model
	) {
		model.addAttribute("order", orderService.findOrderDetail(principal.getName(), orderNumber));
		return "order/detail";
	}

	private OrderStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return OrderStatus.valueOf(status);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
