package com.boot.compick.cart.repository;

import java.util.List;
import java.util.Optional;

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

	List<CartProductItemEntity> findByCartCartIdOrderByCartProductItemIdDesc(Long cartId);

	List<CartProductItemEntity> findByCartCartIdAndSelectedOrderByCartProductItemIdDesc(
		Long cartId,
		String selected
	);

	@Query("""
		select coalesce(sum(item.quantity), 0)
		from CartProductItemEntity item
		where item.cart.cartId = :cartId
		""")
	long sumQuantityByCartId(@Param("cartId") Long cartId);
}
