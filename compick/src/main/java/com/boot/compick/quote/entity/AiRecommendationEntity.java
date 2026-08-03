package com.boot.compick.quote.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AI_RECOMMENDATION")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AiRecommendationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_recommendation_id") private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false, unique = true) private QuoteEntity quote;
    @Lob @Column(name = "user_requirements", nullable = false) private String requirements;
    @Lob @Column(name = "ai_answer_json", nullable = false) private String answerJson;

    public static AiRecommendationEntity create(QuoteEntity quote, String requirements, String answerJson) {
        AiRecommendationEntity recommendation = new AiRecommendationEntity();
        recommendation.quote = quote;
        recommendation.requirements = requirements;
        recommendation.answerJson = answerJson;
        return recommendation;
    }
}
