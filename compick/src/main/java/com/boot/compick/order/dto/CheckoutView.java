package com.boot.compick.order.dto;
import java.util.List;
public record CheckoutView(
        List<CheckoutItem> items,
        List<CheckoutQuote> quotes,
        long productAmount,
        long shippingFee,
        long finalAmount) {

    public boolean isEmpty() {
        return items.isEmpty() && quotes.isEmpty();
    }
}
