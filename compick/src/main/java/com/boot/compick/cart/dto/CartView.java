package com.boot.compick.cart.dto;

import java.util.List;

public record CartView(List<CartProductView> products, List<CartQuoteView> quotes, long selectedAmount) {
    public boolean isEmpty() { return products.isEmpty() && quotes.isEmpty(); }
    public boolean hasSelection() { return products.stream().anyMatch(CartProductView::selected)
            || quotes.stream().anyMatch(CartQuoteView::selected); }
}
