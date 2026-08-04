package com.boot.compick.cart.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.compick.cart.entity.CartProductItemEntity;

public interface CartProductItemRepository
	extends JpaRepository<CartProductItemEntity, Long> {

	Optional<CartProductItemEntity> findByCartCartIdAndProductId(
		Long cartId,
		Long productId
	);

	@Query("""
		select coalesce(sum(item.quantity), 0)
		from CartProductItemEntity item
		where item.cart.cartId = :cartId
		""")
	long sumQuantityByCartId(@Param("cartId") Long cartId);

	List<CartProductItemEntity> findAllByCartCartIdAndSelectedOrderByCartProductItemId(
		Long cartId,
		String selected
	);
	List<CartProductItemEntity> findAllByCartCartIdOrderByCartProductItemId(Long cartId);
	void deleteAllByCartCartIdAndSelected(Long cartId, String selected);
	long deleteByCartCartIdAndCartProductItemId(Long cartId, Long cartProductItemId);
	void deleteAllByCartCartIdAndCartProductItemIdIn(Long cartId, List<Long> cartProductItemIds);

	void deleteAllByCartCartId(Long cartId);
}
