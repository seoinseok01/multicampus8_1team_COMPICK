package com.boot.compick.cart.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.cart.entity.CartQuoteItemEntity;

public interface CartQuoteItemRepository extends JpaRepository<CartQuoteItemEntity, Long> {
    boolean existsByCartCartIdAndQuoteId(Long cartId, Long quoteId);
    Optional<CartQuoteItemEntity> findByCartCartIdAndQuoteId(Long cartId, Long quoteId);

    @EntityGraph(attributePaths = "quote")
    List<CartQuoteItemEntity> findAllByCartCartIdAndSelectedOrderById(Long cartId, String selected);

    @EntityGraph(attributePaths = "quote")
    List<CartQuoteItemEntity> findAllByCartCartIdOrderById(Long cartId);

    void deleteAllByCartCartIdAndSelected(Long cartId, String selected);
    long deleteByCartCartIdAndId(Long cartId, Long id);
    void deleteAllByCartCartIdAndIdIn(Long cartId, List<Long> ids);
}
