package com.boot.compick.order.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderController {

	@GetMapping("/orders/new")
	public String orderForm(Model model) {
		List<OrderItemView> items = List.of(
			new OrderItemView(
				"게이밍 PC 컴픽 에디션",
				"AMD Ryzen 7 7800X3D · RTX 4070 SUPER",
				1,
				1_589_000,
				"PC"
			),
			new OrderItemView(
				"전문 조립 및 안정성 테스트",
				"전문 기사 조립 · 2년 무상 방문 지원",
				1,
				329_000,
				"TOOL"
			)
		);

		int productAmount = items.stream()
			.mapToInt(item -> item.price() * item.quantity())
			.sum();
		int shippingFee = 0;

		model.addAttribute("customerName", "서인석");
		model.addAttribute("phoneNumber", "010-1234-0108");
		model.addAttribute("address", "서울특별시 강남구 테헤란로 123, COMPICK 8층");
		model.addAttribute("items", items);
		model.addAttribute("productAmount", productAmount);
		model.addAttribute("shippingFee", shippingFee);
		model.addAttribute("totalAmount", productAmount + shippingFee);

		return "order/form";
	}

	record OrderItemView(
		String name,
		String description,
		int quantity,
		int price,
		String icon
	) {
	}
}
