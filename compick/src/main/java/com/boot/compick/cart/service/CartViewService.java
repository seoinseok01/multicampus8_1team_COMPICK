package com.boot.compick.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.compick.cart.dto.CartItemView;
import com.boot.compick.cart.dto.CartView;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional(readOnly = true)
public class CartViewService {

	@PersistenceContext
	private EntityManager entityManager;

	public CartView findByLoginId(String loginId) {
		@SuppressWarnings("unchecked")
		List<Object[]> rows = entityManager.createNativeQuery("""
			SELECT
				item.cart_product_item_id,
				product.product_id,
				product.product_name,
				product.brand,
				product.model_name,
				product.price,
				item.quantity,
				product.image_url,
				item.is_selected
			FROM MEMBER member
			JOIN CART cart ON cart.member_id = member.member_id
			JOIN CART_PRODUCT_ITEM item ON item.cart_id = cart.cart_id
			JOIN PRODUCT product ON product.product_id = item.product_id
			WHERE member.login_id = :loginId
			ORDER BY item.cart_product_item_id DESC
			""")
			.setParameter("loginId", loginId)
			.getResultList();

		if (rows.isEmpty()) {
			return CartView.empty();
		}

		List<CartItemView> items = rows.stream()
			.map(this::toView)
			.toList();
		long productAmount = items.stream()
			.filter(CartItemView::selected)
			.mapToLong(CartItemView::lineAmount)
			.sum();
		long shippingFee = productAmount > 0 ? 0 : 0;

		return new CartView(
			items,
			productAmount,
			shippingFee,
			productAmount + shippingFee
		);
	}

	private CartItemView toView(Object[] row) {
		return new CartItemView(
			((Number) row[0]).longValue(),
			((Number) row[1]).longValue(),
			(String) row[2],
			(String) row[3],
			(String) row[4],
			((Number) row[5]).longValue(),
			((Number) row[6]).intValue(),
			(String) row[7],
			"Y".equalsIgnoreCase(String.valueOf(row[8]).trim())
		);
	}
}
