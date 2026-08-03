package com.boot.compick.cart.dto;

import java.util.List;

public record CartView(List<CartQuoteView> quotes, long selectedAmount) {
    public boolean isEmpty() { return quotes.isEmpty(); }
    public boolean hasSelection() { return quotes.stream().anyMatch(CartQuoteView::selected); }
}
