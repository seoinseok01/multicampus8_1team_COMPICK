package com.boot.compick.quote.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.member.service.MemberService;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.dto.PresetUpsertRequest;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.entity.QuoteType;
import com.boot.compick.quote.repository.QuoteRepository;

/**
 * 추천 견적(PRESET) 등록/수정/삭제 유스케이스.
 * 현재 이 서비스를 호출하는 컨트롤러는 없다 — 향후 관리자 페이지가 생기면
 * 여기 메서드를 그대로 호출하는 얇은 @RestController만 추가하면 된다.
 * 자세한 사용법은 docs/recommended-build.md 참고.
 */
@Service
@Transactional(readOnly = true)
public class PresetAdminService {

	private static final String SYSTEM_PRESET_LOGIN_ID = "system_preset";

	private final QuoteRepository quoteRepository;
	private final ProductRepository productRepository;
	private final MemberService memberService;

	public PresetAdminService(
		QuoteRepository quoteRepository,
		ProductRepository productRepository,
		MemberService memberService
	) {
		this.quoteRepository = quoteRepository;
		this.productRepository = productRepository;
		this.memberService = memberService;
	}

	@Transactional
	public Long createPreset(PresetUpsertRequest request) {
		Map<Long, Integer> quantityByProductId = quantityByProductId(request.items());
		List<ProductEntity> products = resolveProducts(quantityByProductId);
		QuoteSelectionValidator.validate(products, quantityByProductId);

		Long systemMemberId = memberService.findActiveByLoginId(SYSTEM_PRESET_LOGIN_ID).getId();
		QuoteEntity preset = QuoteEntity.createPreset(
			systemMemberId,
			request.quoteName(),
			request.purposeTag(),
			request.summaryDescription()
		);
		quantityByProductId.forEach(preset::addItem);

		return quoteRepository.save(preset).getQuoteId();
	}

	@Transactional
	public void updatePreset(Long quoteId, PresetUpsertRequest request) {
		QuoteEntity preset = findPresetOrThrow(quoteId);

		Map<Long, Integer> quantityByProductId = quantityByProductId(request.items());
		List<ProductEntity> products = resolveProducts(quantityByProductId);
		QuoteSelectionValidator.validate(products, quantityByProductId);

		preset.updateDetails(request.quoteName(), request.purposeTag(), request.summaryDescription());
		preset.replaceItems(quantityByProductId);
	}

	@Transactional
	public void deletePreset(Long quoteId) {
		QuoteEntity preset = findPresetOrThrow(quoteId);
		quoteRepository.delete(preset);
	}

	private QuoteEntity findPresetOrThrow(Long quoteId) {
		return quoteRepository.findByQuoteIdAndQuoteType(quoteId, QuoteType.PRESET)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"추천 견적을 찾을 수 없습니다."
			));
	}

	private Map<Long, Integer> quantityByProductId(List<PresetUpsertRequest.PresetItem> items) {
		return items.stream()
			.collect(Collectors.toMap(
				PresetUpsertRequest.PresetItem::productId,
				PresetUpsertRequest.PresetItem::quantity
			));
	}

	private List<ProductEntity> resolveProducts(Map<Long, Integer> quantityByProductId) {
		List<ProductEntity> products = productRepository.findAllById(quantityByProductId.keySet());
		if (products.size() != quantityByProductId.size()) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"존재하지 않는 상품이 포함되어 있습니다."
			);
		}
		return products;
	}
}
