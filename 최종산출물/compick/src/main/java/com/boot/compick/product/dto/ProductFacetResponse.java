package com.boot.compick.product.dto;

import java.util.List;
import java.util.Map;

public record ProductFacetResponse(
	List<String> brands,
	Map<String, List<String>> specOptions,
	long minPrice,
	long maxPrice
) {
}
