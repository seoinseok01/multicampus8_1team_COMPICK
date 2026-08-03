package com.boot.compick.quote.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.quote.entity.QuoteEntity;

public interface QuoteRepository extends JpaRepository<QuoteEntity, Long> {
    Optional<QuoteEntity> findByIdAndMemberId(Long id, Long memberId);
}
