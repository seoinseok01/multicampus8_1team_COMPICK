package com.boot.compick.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.boot.compick.product.entity.ProductEntity;

public interface ProductRepository
	extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

	@EntityGraph(attributePaths = "category")
	List<ProductEntity> findByCategoryCategoryNameAndSalesStatusAndStockQuantityGreaterThan(
		String categoryName,
		String salesStatus,
		int stockQuantity,
		Pageable pageable
	);

	@Query(
		"select distinct p.brand from ProductEntity p "
			+ "where p.category.categoryName = :categoryName and p.salesStatus = 'ON_SALE' "
			+ "order by p.brand"
	)
	List<String> findDistinctBrandsByCategory(@Param("categoryName") String categoryName);

	Optional<ProductEntity> findFirstByProductName(String productName);

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
