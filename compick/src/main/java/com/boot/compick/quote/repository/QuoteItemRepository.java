package com.boot.compick.quote.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.quote.entity.QuoteItemEntity;

public interface QuoteItemRepository extends JpaRepository<QuoteItemEntity, Long> {
    @EntityGraph(attributePaths = "product")
    List<QuoteItemEntity> findAllByQuoteIdInOrderById(Collection<Long> quoteIds);
}
