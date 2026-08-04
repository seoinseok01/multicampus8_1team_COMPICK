package com.boot.compick.order.controller;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.boot.compick.member.entity.Address;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.service.AddressService;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.order.dto.CreateOrderRequest;
import com.boot.compick.order.entity.OrderEntity;
import com.boot.compick.order.service.CheckoutService;
import com.boot.compick.order.service.OrderService;

@Controller
public class OrderController {
	private final CheckoutService checkoutService; private final AddressService addressService;
	private final MemberService memberService; private final OrderService orderService; private final String tossClientKey;
	public OrderController(CheckoutService checkoutService, AddressService addressService, MemberService memberService,
		OrderService orderService, @Value("${toss.payments.client-key:}") String tossClientKey) {
		this.checkoutService=checkoutService; this.addressService=addressService; this.memberService=memberService;
		this.orderService=orderService; this.tossClientKey=tossClientKey;
	}
	@GetMapping({"/order", "/orders/new"})
	public String form(Principal principal, Model model) {
		var checkout = checkoutService.getCheckout(principal.getName());
		if (checkout.isEmpty()) {
			return "redirect:/cart?empty";
		}

		List<Address> addresses = addressService.findAll(principal.getName());
		CreateOrderRequest request = new CreateOrderRequest();
		addresses.stream()
			.filter(Address::isDefault)
			.findFirst()
			.or(() -> addresses.stream().findFirst())
			.map(Address::getId)
			.ifPresent(request::setAddressId);

		model.addAttribute("checkout", checkout);
		model.addAttribute("addresses", addresses);
		model.addAttribute("orderRequest", request);
		return "order/form";
	}
	@PostMapping("/orders")
	public String create(Principal principal, @ModelAttribute("orderRequest") CreateOrderRequest request,
		RedirectAttributes redirect) {
		try {
			String orderNumber = orderService.create(principal.getName(), request).getOrderNumber();
			redirect.addFlashAttribute("message", "주문이 생성되었습니다. 결제를 진행해 주세요.");
			return "redirect:/orders/" + orderNumber + "/payment";
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			return "redirect:/orders/new";
		}
	}
	@GetMapping("/orders")
	public String history(Principal principal, Model model) {
		model.addAttribute("orders", orderService.findAllOwned(principal.getName()));
		return "order/history";
	}
	@GetMapping("/orders/{orderNumber}")
	public String detail(Principal principal, @PathVariable String orderNumber, Model model) {
		OrderEntity order = orderService.findOwned(principal.getName(), orderNumber);
		model.addAttribute("order", order);
		model.addAttribute("groups", orderService.getGroups(order.getId()));
		return "order/detail";
	}
	@GetMapping("/orders/{orderNumber}/payment")
	public String payment(Principal principal, @PathVariable String orderNumber, Model model) {
		OrderEntity order = orderService.findOwned(principal.getName(), orderNumber);
		Member member = memberService.findActiveByLoginId(principal.getName());
		model.addAttribute("order", order);
		model.addAttribute("items", orderService.getItems(order.getId()));
		model.addAttribute("tossClientKey", tossClientKey);
		model.addAttribute("tossConfigured", !tossClientKey.isBlank());
		model.addAttribute("customerName", member.getName());
		model.addAttribute("customerKey", customerKey(principal.getName()));
		return "order/payment";
	}

	private String customerKey(String loginId) {
		return "CUSTOMER-" + UUID.nameUUIDFromBytes(loginId.getBytes(StandardCharsets.UTF_8))
			.toString()
			.replace("-", "");
	}
}
