package com.boot.compick.quote.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.quote.entity.AiRecommendationEntity;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendationEntity, Long> {
	List<AiRecommendationEntity> findAllByOrderByAiRecommendationIdDesc();
}
