package com.boot.compick.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import com.boot.compick.product.entity.ProductEntity;

public interface ProductRepository
	extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {
	Optional<ProductEntity> findFirstByProductName(String productName);

	@EntityGraph(attributePaths = "category")
	List<ProductEntity>
		findTop4BySalesStatusAndStockQuantityGreaterThanOrderByRatingCountDescCreatedAtDesc(
			String salesStatus,
			int stockQuantity
		);

	@Query(
		"select distinct p.brand from ProductEntity p "
			+ "where p.category.categoryName = :categoryName and p.salesStatus = 'ON_SALE' "
			+ "order by p.brand"
	)
	List<String> findDistinctBrandsByCategory(@Param("categoryName") String categoryName);

	@Query(
		"select min(p.price) from ProductEntity p "
			+ "where p.category.categoryName = :categoryName and p.salesStatus = 'ON_SALE'"
	)
	Long findMinPriceByCategory(@Param("categoryName") String categoryName);

	@Query(
		"select max(p.price) from ProductEntity p "
			+ "where p.category.categoryName = :categoryName and p.salesStatus = 'ON_SALE'"
	)
	Long findMaxPriceByCategory(@Param("categoryName") String categoryName);
}
