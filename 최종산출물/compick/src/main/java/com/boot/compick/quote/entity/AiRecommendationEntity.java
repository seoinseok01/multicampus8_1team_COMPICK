package com.boot.compick.quote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "AI_RECOMMENDATION")
public class AiRecommendationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ai_recommendation_id")
	private Long aiRecommendationId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quote_id", nullable = false, unique = true)
	private QuoteEntity quote;

	@Lob
	@Column(name = "user_requirements", nullable = false)
	private String userRequirements;

	@Lob
	@Column(name = "ai_answer_json", nullable = false)
	private String aiAnswerJson;

	protected AiRecommendationEntity() {
	}

	private AiRecommendationEntity(QuoteEntity quote, String userRequirements, String aiAnswerJson) {
		this.quote = quote;
		this.userRequirements = userRequirements;
		this.aiAnswerJson = aiAnswerJson;
	}

	public static AiRecommendationEntity create(
		QuoteEntity quote,
		String userRequirements,
		String aiAnswerJson
	) {
		return new AiRecommendationEntity(quote, userRequirements, aiAnswerJson);
	}

	public Long getAiRecommendationId() {
		return aiRecommendationId;
	}

	public QuoteEntity getQuote() {
		return quote;
	}

	public String getUserRequirements() {
		return userRequirements;
	}

	public String getAiAnswerJson() {
		return aiAnswerJson;
	}

	public void updateAiAnswerJson(String aiAnswerJson) {
		this.aiAnswerJson = aiAnswerJson;
	}
}
