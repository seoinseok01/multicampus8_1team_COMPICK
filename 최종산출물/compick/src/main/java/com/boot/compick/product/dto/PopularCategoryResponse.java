package com.boot.compick.product.dto;

import java.util.List;

public record PopularCategoryResponse(
	String category,
	String label,
	List<PopularProductResponse> products
) {
}
