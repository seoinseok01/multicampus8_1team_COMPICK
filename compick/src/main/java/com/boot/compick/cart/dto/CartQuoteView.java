package com.boot.compick.cart.dto;

import java.util.List;
import com.boot.compick.order.dto.CheckoutItem;

public record CartQuoteView(
        Long cartItemId,
        Long quoteId,
        String name,
        String typeLabel,
        int quantity,
        boolean selected,
        List<CheckoutItem> items,
        long lineAmount) {
}
