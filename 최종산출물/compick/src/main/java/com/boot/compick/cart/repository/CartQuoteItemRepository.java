package com.boot.compick.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.cart.entity.CartQuoteItemEntity;

public interface CartQuoteItemRepository extends JpaRepository<CartQuoteItemEntity, Long> {

	Optional<CartQuoteItemEntity> findByCartCartIdAndQuoteId(Long cartId, Long quoteId);

	List<CartQuoteItemEntity> findByCartCartIdOrderByCartQuoteItemIdDesc(Long cartId);

	List<CartQuoteItemEntity> findByCartCartIdAndSelectedOrderByCartQuoteItemIdDesc(
		Long cartId,
		String selected
	);
}
