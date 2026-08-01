package com.boot.compick.quote.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "QUOTE")
public class QuoteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "quote_id")
	private Long quoteId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "quote_name", nullable = false, length = 150)
	private String quoteName;

	@Enumerated(EnumType.STRING)
	@Column(name = "quote_type", nullable = false, length = 20)
	private QuoteType quoteType;

	@Enumerated(EnumType.STRING)
	@Column(name = "assembly_type", nullable = false, length = 20)
	private AssemblyType assemblyType = AssemblyType.SELF;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose_tag", length = 20)
	private PurposeTag purposeTag;

	@Column(name = "summary_description", length = 200)
	private String summaryDescription;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<QuoteItemEntity> items = new ArrayList<>();

	protected QuoteEntity() {
	}

	private QuoteEntity(
		Long memberId,
		String quoteName,
		QuoteType quoteType,
		AssemblyType assemblyType,
		PurposeTag purposeTag,
		String summaryDescription
	) {
		this.memberId = memberId;
		this.quoteName = quoteName;
		this.quoteType = quoteType;
		this.assemblyType = assemblyType;
		this.purposeTag = purposeTag;
		this.summaryDescription = summaryDescription;
	}

	public static QuoteEntity createUserQuote(
		Long memberId,
		String quoteName,
		AssemblyType assemblyType
	) {
		return new QuoteEntity(memberId, quoteName, QuoteType.USER, assemblyType, null, null);
	}

	/**
	 * 관리자(향후 관리자 페이지)가 추천 견적을 등록할 때 사용하는 팩토리.
	 * memberId는 시스템 계정 소유로 고정되며, 실제 소유자는 quoteType=PRESET으로 구분한다.
	 */
	public static QuoteEntity createPreset(
		Long systemMemberId,
		String quoteName,
		PurposeTag purposeTag,
		String summaryDescription
	) {
		return new QuoteEntity(
			systemMemberId,
			quoteName,
			QuoteType.PRESET,
			AssemblyType.SELF,
			purposeTag,
			summaryDescription
		);
	}

	public void updateDetails(String quoteName, PurposeTag purposeTag, String summaryDescription) {
		this.quoteName = quoteName;
		this.purposeTag = purposeTag;
		this.summaryDescription = summaryDescription;
	}

	public void addItem(Long productId, int quantity) {
		items.add(QuoteItemEntity.create(this, productId, quantity));
	}

	/**
	 * 기존 구성 부품을 모두 비우고 새 목록으로 교체한다.
	 * orphanRemoval=true라 clear() 시점에 기존 QUOTE_ITEM 행이 삭제된다.
	 */
	public void replaceItems(Map<Long, Integer> quantityByProductId) {
		items.clear();
		quantityByProductId.forEach(this::addItem);
	}

	public Long getQuoteId() {
		return quoteId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getQuoteName() {
		return quoteName;
	}

	public QuoteType getQuoteType() {
		return quoteType;
	}

	public AssemblyType getAssemblyType() {
		return assemblyType;
	}

	public PurposeTag getPurposeTag() {
		return purposeTag;
	}

	public String getSummaryDescription() {
		return summaryDescription;
	}

	public List<QuoteItemEntity> getItems() {
		return items;
	}
}
