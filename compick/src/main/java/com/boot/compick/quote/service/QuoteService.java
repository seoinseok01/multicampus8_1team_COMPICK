package com.boot.compick.quote.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.cart.entity.CartEntity;
import com.boot.compick.cart.entity.CartQuoteItemEntity;
import com.boot.compick.cart.repository.CartQuoteItemRepository;
import com.boot.compick.cart.repository.CartRepository;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.product.SpecJsonSupport;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.dto.CartQuoteItemResponse;
import com.boot.compick.quote.dto.PresetDetailResponse;
import com.boot.compick.quote.dto.PresetSummaryResponse;
import com.boot.compick.quote.dto.QuoteBuildRequest;
import com.boot.compick.quote.dto.QuoteItemView;
import com.boot.compick.quote.entity.PurposeTag;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.entity.QuoteItemEntity;
import com.boot.compick.quote.entity.QuoteType;
import com.boot.compick.quote.repository.QuoteRepository;

@Service
@Transactional(readOnly = true)
public class QuoteService {

	private static final String MAINBOARD_CATEGORY = "MAINBOARD";

	private final QuoteRepository quoteRepository;
	private final ProductRepository productRepository;
	private final CartRepository cartRepository;
	private final CartQuoteItemRepository cartQuoteItemRepository;
	private final MemberService memberService;

	public QuoteService(
		QuoteRepository quoteRepository,
		ProductRepository productRepository,
		CartRepository cartRepository,
		CartQuoteItemRepository cartQuoteItemRepository,
		MemberService memberService
	) {
		this.quoteRepository = quoteRepository;
		this.productRepository = productRepository;
		this.cartRepository = cartRepository;
		this.cartQuoteItemRepository = cartQuoteItemRepository;
		this.memberService = memberService;
	}

	@Transactional
	public CartQuoteItemResponse buildAndAddToCart(String loginId, QuoteBuildRequest request) {
		Map<Long, Integer> quantityByProductId = request.items().stream()
			.collect(Collectors.toMap(
				QuoteBuildRequest.QuoteLineItem::productId,
				QuoteBuildRequest.QuoteLineItem::quantity
			));

		List<ProductEntity> products = productRepository.findAllById(quantityByProductId.keySet());
		if (products.size() != quantityByProductId.size()) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"존재하지 않는 상품이 포함되어 있습니다."
			);
		}

		QuoteSelectionValidator.validate(products, quantityByProductId);

		Long memberId = memberService.findActiveByLoginId(loginId).getId();

		QuoteEntity quote = QuoteEntity.createUserQuote(
			memberId,
			buildQuoteName(products),
			request.assemblyType()
		);
		products.forEach(product ->
			quote.addItem(product.getProductId(), quantityByProductId.get(product.getProductId()))
		);
		QuoteEntity savedQuote = quoteRepository.save(quote);

		return addToCart(memberId, savedQuote.getQuoteId());
	}

	@Transactional
	public CartQuoteItemResponse addExistingQuoteToCart(String loginId, Long quoteId) {
		quoteRepository.findByQuoteIdAndQuoteType(quoteId, QuoteType.PRESET)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"추천 견적을 찾을 수 없습니다."
			));

		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		return addToCart(memberId, quoteId);
	}

	public List<PresetSummaryResponse> findPresets(PurposeTag purposeTag) {
		List<QuoteEntity> presets = purposeTag == null
			? quoteRepository.findByQuoteTypeOrderByQuoteIdAsc(QuoteType.PRESET)
			: quoteRepository.findByQuoteTypeAndPurposeTagOrderByQuoteIdAsc(
				QuoteType.PRESET,
				purposeTag
			);

		return presets.stream()
			.map(this::toSummary)
			.toList();
	}

	public PresetDetailResponse findPresetDetail(Long quoteId) {
		QuoteEntity quote = quoteRepository.findByQuoteIdAndQuoteType(quoteId, QuoteType.PRESET)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"추천 견적을 찾을 수 없습니다."
			));

		Map<Long, ProductEntity> productsById = productsById(quote.getItems());
		List<QuoteItemView> items = toItemViews(quote.getItems(), productsById);

		return new PresetDetailResponse(
			quote.getQuoteId(),
			quote.getQuoteName(),
			quote.getPurposeTag() == null ? null : quote.getPurposeTag().name(),
			quote.getSummaryDescription(),
			items,
			totalPrice(items),
			estimatedPowerWatt(quote.getItems(), productsById)
		);
	}

	public QuoteItemView findProductAsQuoteItem(Long productId) {
		ProductEntity product = productRepository.findById(productId)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"상품을 찾을 수 없습니다."
			));

		return toItemView(product, 1);
	}

	private CartQuoteItemResponse addToCart(Long memberId, Long quoteId) {
		CartEntity cart = cartRepository.findByMemberId(memberId)
			.orElseGet(() -> cartRepository.save(CartEntity.create(memberId)));

		CartQuoteItemEntity cartItem = cartQuoteItemRepository
			.findByCartCartIdAndQuoteId(cart.getCartId(), quoteId)
			.map(item -> {
				item.increaseQuantity(1);
				return item;
			})
			.orElseGet(() -> cartQuoteItemRepository.save(
				CartQuoteItemEntity.create(cart, quoteId, 1)
			));

		return new CartQuoteItemResponse(
			cart.getCartId(),
			quoteId,
			cartItem.getQuantity(),
			"장바구니에 견적을 담았습니다."
		);
	}

	private String buildQuoteName(List<ProductEntity> products) {
		return products.stream()
			.filter(product -> "CPU".equals(product.getCategory().getCategoryName()))
			.map(ProductEntity::getProductName)
			.findFirst()
			.map(cpuName -> cpuName + " 기반 견적")
			.orElse("나의 견적");
	}

	private PresetSummaryResponse toSummary(QuoteEntity quote) {
		Map<Long, ProductEntity> productsById = productsById(quote.getItems());
		List<QuoteItemView> items = toItemViews(quote.getItems(), productsById);

		List<String> highlights = List.of("CPU", "GPU").stream()
			.map(category -> items.stream()
				.filter(item -> item.category().equals(category))
				.findFirst()
				.map(QuoteItemView::name)
				.orElse(null))
			.filter(name -> name != null)
			.toList();

		return new PresetSummaryResponse(
			quote.getQuoteId(),
			quote.getQuoteName(),
			quote.getPurposeTag() == null ? null : quote.getPurposeTag().name(),
			quote.getSummaryDescription(),
			totalPrice(items),
			highlights
		);
	}

	private Map<Long, ProductEntity> productsById(List<QuoteItemEntity> items) {
		List<Long> productIds = items.stream().map(QuoteItemEntity::getProductId).toList();
		return productRepository.findAllById(productIds).stream()
			.collect(Collectors.toMap(ProductEntity::getProductId, product -> product));
	}

	private List<QuoteItemView> toItemViews(
		List<QuoteItemEntity> items,
		Map<Long, ProductEntity> productsById
	) {
		return items.stream()
			.map(item -> {
				ProductEntity product = productsById.get(item.getProductId());
				return product == null ? null : toItemView(product, item.getQuantity());
			})
			.filter(item -> item != null)
			.toList();
	}

	private QuoteItemView toItemView(ProductEntity product, int quantity) {
		String category = product.getCategory().getCategoryName();
		Integer memorySlots = MAINBOARD_CATEGORY.equals(category)
			? SpecJsonSupport.readInt(product.getSpecJson(), "Memory Slots")
			: null;

		return new QuoteItemView(
			category,
			product.getProductId(),
			product.getProductName(),
			product.getBrand(),
			product.getPrice(),
			quantity,
			product.getImageUrl(),
			product.getPowerConsumption(),
			product.getRecommendedPower(),
			product.getSocketType(),
			product.getMemoryType(),
			product.getFormFactor(),
			product.getGpuLengthMm(),
			product.getMaxGpuLengthMm(),
			product.getPowerCapacityWatt(),
			memorySlots
		);
	}

	private long totalPrice(List<QuoteItemView> items) {
		return items.stream().mapToLong(item -> item.price() * item.quantity()).sum();
	}

	private Integer estimatedPowerWatt(
		List<QuoteItemEntity> items,
		Map<Long, ProductEntity> productsById
	) {
		int total = 0;
		boolean hasAnyPowerInfo = false;

		for (QuoteItemEntity item : items) {
			ProductEntity product = productsById.get(item.getProductId());
			if (product == null) {
				continue;
			}
			String category = product.getCategory().getCategoryName();
			if ("CPU".equals(category) && product.getPowerConsumption() != null) {
				total += product.getPowerConsumption() * item.getQuantity();
				hasAnyPowerInfo = true;
			}
			if ("GPU".equals(category) && product.getRecommendedPower() != null) {
				total += product.getRecommendedPower() * item.getQuantity();
				hasAnyPowerInfo = true;
			}
		}

		return hasAnyPowerInfo ? total : null;
	}
}
