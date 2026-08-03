package com.boot.compick.order.dto;
public record CheckoutItem(Long productId, String name, String brand, String imageUrl,
	long price, int quantity, long lineAmount) {}
