package com.boot.compick.product.dto;

public record PopularProductResponse(
	Long productId,
	String category,
	String brand,
	String name,
	long price,
	int stockQuantity,
	String description,
	String imageUrl,
	String specLabel1,
	String specValue1,
	String specLabel2,
	String specValue2,
	String specLabel3,
	String specValue3,
	String specLabel4,
	String specValue4
) {
}
