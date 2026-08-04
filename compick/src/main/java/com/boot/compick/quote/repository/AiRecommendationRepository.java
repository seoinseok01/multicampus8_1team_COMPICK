package com.boot.compick.quote.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.quote.entity.AiRecommendationEntity;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendationEntity, Long> {
}
