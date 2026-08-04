package com.boot.compick.product.dto;

import java.util.Map;

import com.boot.compick.product.SpecJsonSupport;
import com.boot.compick.product.entity.ProductEntity;

public record ProductListItemResponse(
	Long productId,
	String category,
	String brand,
	String name,
	long price,
	int stockQuantity,
	int ratingCount,
	String imageUrl,
	Integer powerConsumption,
	Integer recommendedPower,
	String socketType,
	String memoryType,
	String formFactor,
	Integer gpuLengthMm,
	Integer maxGpuLengthMm,
	Integer powerCapacityWatt,
	Integer memorySlots,
	Map<String, String> specs
) {

	public String specSummary() {
		return String.join(" / ", specs.values());
	}

	public static ProductListItemResponse from(ProductEntity product) {
		String category = product.getCategory().getCategoryName();
		Integer memorySlots = "MAINBOARD".equals(category)
			? SpecJsonSupport.readInt(product.getSpecJson(), "Memory Slots")
			: null;

		return new ProductListItemResponse(
			product.getProductId(),
			category,
			product.getBrand(),
			product.getProductName(),
			product.getPrice(),
			product.getStockQuantity(),
			product.getRatingCount(),
			product.getImageUrl(),
			product.getPowerConsumption(),
			product.getRecommendedPower(),
			product.getSocketType(),
			product.getMemoryType(),
			product.getFormFactor(),
			product.getGpuLengthMm(),
			product.getMaxGpuLengthMm(),
			product.getPowerCapacityWatt(),
			memorySlots,
			SpecJsonSupport.readAll(product.getSpecJson())
		);
	}
}
