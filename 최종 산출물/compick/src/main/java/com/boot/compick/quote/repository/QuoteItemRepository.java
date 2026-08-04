package com.boot.compick.quote.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.quote.entity.QuoteItemEntity;

public interface QuoteItemRepository extends JpaRepository<QuoteItemEntity, Long> {
}
