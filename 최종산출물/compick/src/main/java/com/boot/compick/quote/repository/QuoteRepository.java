package com.boot.compick.quote.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.quote.entity.PurposeTag;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.entity.QuoteType;

public interface QuoteRepository extends JpaRepository<QuoteEntity, Long> {

	@EntityGraph(attributePaths = "items")
	Optional<QuoteEntity> findByQuoteIdAndQuoteType(Long quoteId, QuoteType quoteType);

	@EntityGraph(attributePaths = "items")
	Optional<QuoteEntity> findById(Long quoteId);

	@EntityGraph(attributePaths = "items")
	List<QuoteEntity> findByQuoteTypeOrderByQuoteIdAsc(QuoteType quoteType);

	@EntityGraph(attributePaths = "items")
	List<QuoteEntity> findByQuoteTypeAndPurposeTagOrderByQuoteIdAsc(
		QuoteType quoteType,
		PurposeTag purposeTag
	);

	@EntityGraph(attributePaths = "items")
	List<QuoteEntity> findByQuoteIdIn(List<Long> quoteIds);

	@EntityGraph(attributePaths = "items")
	List<QuoteEntity> findByMemberIdAndQuoteTypeOrderByCreatedAtDesc(
		Long memberId,
		QuoteType quoteType,
		Pageable pageable
	);
}
