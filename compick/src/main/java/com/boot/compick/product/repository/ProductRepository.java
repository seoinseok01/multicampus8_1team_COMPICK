package com.boot.compick.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.product.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	@EntityGraph(attributePaths = "category")
	List<ProductEntity>
		findTop4BySalesStatusAndStockQuantityGreaterThanOrderByRatingCountDesc(
			String salesStatus,
			int stockQuantity
		);
}
