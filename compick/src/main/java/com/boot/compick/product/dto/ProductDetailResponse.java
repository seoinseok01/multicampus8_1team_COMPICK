package com.boot.compick.product.dto;

import java.util.Map;

import com.boot.compick.product.CategoryDisplay;
import com.boot.compick.product.SpecJsonSupport;
import com.boot.compick.product.entity.ProductEntity;

public record ProductDetailResponse(
	Long productId,
	String category,
	String categoryLabel,
	String brand,
	String name,
	String description,
	long price,
	boolean inStock,
	String imageUrl,
	Map<String, String> specs
) {

	public static ProductDetailResponse from(ProductEntity product) {
		String category = product.getCategory().getCategoryName();
		return new ProductDetailResponse(
			product.getProductId(),
			category,
			CategoryDisplay.labelOf(category),
			product.getBrand(),
			product.getProductName(),
			product.getProductDescription(),
			product.getPrice(),
			product.getStockQuantity() > 0,
			product.getImageUrl(),
			SpecJsonSupport.readAll(product.getSpecJson())
		);
	}
}
