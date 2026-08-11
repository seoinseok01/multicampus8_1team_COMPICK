package com.boot.compick.order.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boot.compick.order.dto.CreateOrderRequest;
import com.boot.compick.order.dto.OrderDetailResponse;
import com.boot.compick.order.service.OrderService;
import com.boot.compick.payment.service.TossPaymentException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

	private final OrderService orderService;

	public OrderApiController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderDetailResponse create(
		@Valid @RequestBody CreateOrderRequest request,
		Principal principal
	) {
		return orderService.createPendingOrder(principal.getName(), request);
	}

	@PostMapping("/{orderNumber}/cancel-request")
	public void requestCancel(@PathVariable String orderNumber, Principal principal) {
		orderService.requestCancel(principal.getName(), orderNumber);
	}

	@PostMapping("/{orderNumber}/return-request")
	public void requestReturn(@PathVariable String orderNumber, Principal principal) {
		orderService.requestReturn(principal.getName(), orderNumber);
	}

	@ExceptionHandler(TossPaymentException.class)
	public ResponseEntity<Map<String, String>> tossPaymentError(TossPaymentException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
	}
}
