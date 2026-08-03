package com.boot.compick.order.dto;

import java.util.List;

public record CheckoutQuote(
        Long quoteId,
        String name,
        int quantity,
        List<CheckoutItem> items,
        long unitAmount,
        long lineAmount) {
}
