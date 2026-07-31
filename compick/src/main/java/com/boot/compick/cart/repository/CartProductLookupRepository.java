package com.boot.compick.cart.repository;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class CartProductLookupRepository {

	@PersistenceContext
	private EntityManager entityManager;

	public boolean isPurchasable(Long productId, int quantity) {
		Number count = (Number) entityManager.createNativeQuery("""
			SELECT COUNT(*)
			FROM PRODUCT
			WHERE product_id = :productId
			  AND sales_status = 'ON_SALE'
			  AND stock_quantity >= :quantity
			""")
			.setParameter("productId", productId)
			.setParameter("quantity", quantity)
			.getSingleResult();

		return count.longValue() > 0;
	}
}
