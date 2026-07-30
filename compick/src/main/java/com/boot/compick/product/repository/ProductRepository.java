package com.boot.compick.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.product.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	@EntityGraph(attributePaths = "category")
	List<ProductEntity>
<<<<<<< HEAD
		findTop4BySalesStatusAndStockQuantityGreaterThanOrderByRatingCountDesc(
=======
		findTop4BySalesStatusAndStockQuantityGreaterThanOrderByRatingCountDescCreatedAtDesc(
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
			String salesStatus,
			int stockQuantity
		);
}
