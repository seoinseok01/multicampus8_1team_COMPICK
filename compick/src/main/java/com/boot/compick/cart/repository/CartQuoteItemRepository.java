package com.boot.compick.cart.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.cart.entity.CartQuoteItemEntity;

public interface CartQuoteItemRepository extends JpaRepository<CartQuoteItemEntity, Long> {
    boolean existsByCartCartIdAndQuoteId(Long cartId, Long quoteId);

    @EntityGraph(attributePaths = "quote")
    List<CartQuoteItemEntity> findAllByCartCartIdAndSelectedOrderById(Long cartId, String selected);

    @EntityGraph(attributePaths = "quote")
    List<CartQuoteItemEntity> findAllByCartCartIdOrderById(Long cartId);

    void deleteAllByCartCartIdAndSelected(Long cartId, String selected);
}
