package com.boot.compick.quote.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.quote.entity.PurposeTag;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.entity.QuoteType;

public interface QuoteRepository extends JpaRepository<QuoteEntity, Long> {

	@EntityGraph(attributePaths = "items")
	Optional<QuoteEntity> findByQuoteIdAndQuoteType(Long quoteId, QuoteType quoteType);

	List<QuoteEntity> findByQuoteTypeOrderByQuoteIdAsc(QuoteType quoteType);

	List<QuoteEntity> findByQuoteTypeAndPurposeTagOrderByQuoteIdAsc(
		QuoteType quoteType,
		PurposeTag purposeTag
	);
}
