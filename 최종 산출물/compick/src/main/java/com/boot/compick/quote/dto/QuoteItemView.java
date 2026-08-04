package com.boot.compick.quote.dto;

public record QuoteItemView(
	String category,
	Long productId,
	String name,
	String brand,
	long price,
	int quantity,
	String imageUrl,
	Integer powerConsumption,
	Integer recommendedPower,
	String socketType,
	String memoryType,
	String formFactor,
	Integer gpuLengthMm,
	Integer maxGpuLengthMm,
	Integer powerCapacityWatt,
	Integer memorySlots
) {
}
