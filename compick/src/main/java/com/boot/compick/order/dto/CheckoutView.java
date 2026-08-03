package com.boot.compick.order.dto;
import java.util.List;
public record CheckoutView(List<CheckoutItem> items, long productAmount, long shippingFee, long finalAmount) {}
