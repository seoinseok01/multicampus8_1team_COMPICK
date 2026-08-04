package com.boot.compick.cart.dto;

public record CartProductView(Long cartItemId, Long productId, String name, String brand,
        String imageUrl, long price, int quantity, boolean selected, long lineAmount) {}
