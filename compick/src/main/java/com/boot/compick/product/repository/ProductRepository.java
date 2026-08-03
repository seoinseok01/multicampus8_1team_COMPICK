package com.boot.compick.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.product.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	Optional<ProductEntity> findFirstByProductName(String productName);

	@EntityGraph(attributePaths = "category")
	List<ProductEntity>
		findTop4BySalesStatusAndStockQuantityGreaterThanOrderByRatingCountDescCreatedAtDesc(
			String salesStatus,
			int stockQuantity
		);
}
