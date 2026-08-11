package com.boot.compick.quote.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	@Column(name = "image_url", length = 1000)
	private String imageUrl;

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

	public static QuoteEntity createAi(Long memberId, String quoteName) {
		return new QuoteEntity(memberId, quoteName, QuoteType.AI, AssemblyType.SELF, null, null);
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

	public void updateDetails(String quoteName, PurposeTag purposeTag, String summaryDescription, String imageUrl) {
		this.quoteName = quoteName;
		this.purposeTag = purposeTag;
		this.summaryDescription = summaryDescription;
		this.imageUrl = imageUrl;
	}

	public void addItem(Long productId, int quantity) {
		items.add(QuoteItemEntity.create(this, productId, quantity));
	}

	public void replaceItems(Map<Long, Integer> quantityByProductId) {
		Map<Long, QuoteItemEntity> currentItems = items.stream()
			.collect(Collectors.toMap(QuoteItemEntity::getProductId, Function.identity()));

		items.removeIf(item -> !quantityByProductId.containsKey(item.getProductId()));
		quantityByProductId.forEach((productId, quantity) -> {
			QuoteItemEntity item = currentItems.get(productId);
			if (item == null) addItem(productId, quantity);
			else item.updateQuantity(quantity);
		});
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

	public String getImageUrl() { return imageUrl; }

	public List<QuoteItemEntity> getItems() {
		return items;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
