package com.boot.compick.cart.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.cart.dto.AddCartProductRequest;
import com.boot.compick.cart.dto.AddCartProductResponse;
import com.boot.compick.cart.dto.CartProductLineResponse;
import com.boot.compick.cart.dto.CartQuoteItemPreview;
import com.boot.compick.cart.dto.CartQuoteLineResponse;
import com.boot.compick.cart.dto.CartViewResponse;
import com.boot.compick.cart.entity.CartEntity;
import com.boot.compick.cart.entity.CartProductItemEntity;
import com.boot.compick.cart.entity.CartQuoteItemEntity;
import com.boot.compick.cart.repository.CartMemberLookupRepository;
import com.boot.compick.cart.repository.CartProductItemRepository;
import com.boot.compick.cart.repository.CartProductLookupRepository;
import com.boot.compick.cart.repository.CartQuoteItemRepository;
import com.boot.compick.cart.repository.CartRepository;
import com.boot.compick.product.CategoryDisplay;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.entity.QuoteItemEntity;
import com.boot.compick.quote.repository.QuoteRepository;

@Service
@Transactional(readOnly = true)
public class CartService {

	private static final Pattern EATX_PATTERN = Pattern.compile("E-?ATX");
	private static final Pattern MICRO_ATX_PATTERN = Pattern.compile("MICRO|M-?ATX");
	private static final Pattern DTX_PATTERN = Pattern.compile("DTX");
	private static final Pattern ITX_PATTERN = Pattern.compile("ITX");
	private static final Pattern ATX_PATTERN = Pattern.compile("ATX");

	private final CartRepository cartRepository;
	private final CartProductItemRepository cartProductItemRepository;
	private final CartQuoteItemRepository cartQuoteItemRepository;
	private final CartMemberLookupRepository memberLookupRepository;
	private final CartProductLookupRepository productLookupRepository;
	private final ProductRepository productRepository;
	private final QuoteRepository quoteRepository;

	public CartService(
		CartRepository cartRepository,
		CartProductItemRepository cartProductItemRepository,
		CartQuoteItemRepository cartQuoteItemRepository,
		CartMemberLookupRepository memberLookupRepository,
		CartProductLookupRepository productLookupRepository,
		ProductRepository productRepository,
		QuoteRepository quoteRepository
	) {
		this.cartRepository = cartRepository;
		this.cartProductItemRepository = cartProductItemRepository;
		this.cartQuoteItemRepository = cartQuoteItemRepository;
		this.memberLookupRepository = memberLookupRepository;
		this.productLookupRepository = productLookupRepository;
		this.productRepository = productRepository;
		this.quoteRepository = quoteRepository;
	}

	@Transactional
	public AddCartProductResponse addProduct(
		String loginId,
		AddCartProductRequest request
	) {
		Long memberId = activeMemberId(loginId);

		if (!productLookupRepository.isPurchasable(
			request.productId(),
			request.quantity()
		)) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"판매 중인 상품이 아니거나 재고가 부족합니다."
			);
		}

		CartEntity cart = findOrCreateCart(memberId);

		CartProductItemEntity cartItem = cartProductItemRepository
			.findByCartCartIdAndProductId(cart.getCartId(), request.productId())
			.map(item -> {
				item.increaseQuantity(request.quantity());
				return item;
			})
			.orElseGet(() -> cartProductItemRepository.save(
				CartProductItemEntity.create(
					cart,
					request.productId(),
					request.quantity()
				)
			));

		long cartItemCount = cartProductItemRepository
			.sumQuantityByCartId(cart.getCartId());

		return new AddCartProductResponse(
			cart.getCartId(),
			request.productId(),
			cartItem.getQuantity(),
			cartItemCount,
			"장바구니에 상품을 담았습니다."
		);
	}

	public CartViewResponse getCartView(String loginId) {
		Long memberId = activeMemberId(loginId);
		CartEntity cart = cartRepository.findByMemberId(memberId).orElse(null);
		if (cart == null) {
			return new CartViewResponse(List.of(), List.of());
		}

		return new CartViewResponse(
			buildProductLines(cart.getCartId()),
			buildQuoteLines(cart.getCartId())
		);
	}

	/** 주문서 작성(OrderService)이 선택된 항목만 뽑아 주문을 만들 때 쓴다. */
	public List<CartProductItemEntity> findSelectedProductItems(String loginId) {
		Long memberId = activeMemberId(loginId);
		return cartRepository.findByMemberId(memberId)
			.map(cart -> cartProductItemRepository
				.findByCartCartIdAndSelectedOrderByCartProductItemIdDesc(cart.getCartId(), "Y"))
			.orElseGet(List::of);
	}

	public List<CartQuoteItemEntity> findSelectedQuoteItems(String loginId) {
		Long memberId = activeMemberId(loginId);
		return cartRepository.findByMemberId(memberId)
			.map(cart -> cartQuoteItemRepository
				.findByCartCartIdAndSelectedOrderByCartQuoteItemIdDesc(cart.getCartId(), "Y"))
			.orElseGet(List::of);
	}

	@Transactional
	public void changeProductQuantity(String loginId, Long productId, int quantity) {
		if (!productLookupRepository.isPurchasable(productId, quantity)) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"판매 중인 상품이 아니거나 재고가 부족합니다."
			);
		}
		findOwnedProductItem(loginId, productId).changeQuantity(quantity);
	}

	@Transactional
	public void changeProductSelection(String loginId, Long productId, boolean selected) {
		findOwnedProductItem(loginId, productId).changeSelected(selected);
	}

	@Transactional
	public void deleteProductItem(String loginId, Long productId) {
		cartProductItemRepository.delete(findOwnedProductItem(loginId, productId));
	}

	@Transactional
	public void changeQuoteSelection(String loginId, Long quoteId, boolean selected) {
		findOwnedQuoteItem(loginId, quoteId).changeSelected(selected);
	}

	@Transactional
	public void deleteQuoteItem(String loginId, Long quoteId) {
		cartQuoteItemRepository.delete(findOwnedQuoteItem(loginId, quoteId));
	}

	private List<CartProductLineResponse> buildProductLines(Long cartId) {
		List<CartProductItemEntity> items =
			cartProductItemRepository.findByCartCartIdOrderByCartProductItemIdDesc(cartId);
		if (items.isEmpty()) {
			return List.of();
		}

		Map<Long, ProductEntity> productsById = productRepository
			.findAllById(items.stream().map(CartProductItemEntity::getProductId).toList())
			.stream()
			.collect(Collectors.toMap(ProductEntity::getProductId, product -> product));

		return items.stream()
			.map(item -> {
				ProductEntity product = productsById.get(item.getProductId());
				if (product == null) {
					return null;
				}
				boolean purchasable = "ON_SALE".equals(product.getSalesStatus())
					&& product.getStockQuantity() >= item.getQuantity();
				return new CartProductLineResponse(
					product.getProductId(),
					product.getCategory().getCategoryName(),
					product.getBrand(),
					product.getProductName(),
					product.getImageUrl(),
					product.getPrice(),
					item.getQuantity(),
					product.getPrice() * item.getQuantity(),
					item.isSelected(),
					purchasable
				);
			})
			.filter(Objects::nonNull)
			.toList();
	}

	private List<CartQuoteLineResponse> buildQuoteLines(Long cartId) {
		List<CartQuoteItemEntity> items =
			cartQuoteItemRepository.findByCartCartIdOrderByCartQuoteItemIdDesc(cartId);
		if (items.isEmpty()) {
			return List.of();
		}

		Map<Long, QuoteEntity> quotesById = quoteRepository
			.findByQuoteIdIn(items.stream().map(CartQuoteItemEntity::getQuoteId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(QuoteEntity::getQuoteId, quote -> quote));
		Map<Long, ProductEntity> productsById = productRepository.findAllById(
			quotesById.values().stream()
				.flatMap(quote -> quote.getItems().stream())
				.map(QuoteItemEntity::getProductId)
				.distinct()
				.toList()
		).stream().collect(Collectors.toMap(ProductEntity::getProductId, product -> product));

		return items.stream()
			.map(item -> {
				QuoteEntity quote = quotesById.get(item.getQuoteId());
				return quote == null ? null : toQuoteLine(quote, item, productsById);
			})
			.filter(Objects::nonNull)
			.toList();
	}

	private CartQuoteLineResponse toQuoteLine(
		QuoteEntity quote,
		CartQuoteItemEntity cartItem,
		Map<Long, ProductEntity> productsById
	) {
		long unitTotal = quote.getItems().stream()
			.mapToLong(item -> {
				ProductEntity product = productsById.get(item.getProductId());
				return product == null ? 0L : product.getPrice() * item.getQuantity();
			})
			.sum();

		int issueCount = countCompatibilityIssues(quote.getItems(), productsById);

		List<CartQuoteItemPreview> previews = quote.getItems().stream()
			.filter(item -> productsById.get(item.getProductId()) != null)
			.sorted(Comparator.comparingInt(
				item -> categoryOrder(productsById.get(item.getProductId()).getCategory().getCategoryName())
			))
			.map(item -> {
				ProductEntity product = productsById.get(item.getProductId());
				String categoryName = product.getCategory().getCategoryName();
				String name = item.getQuantity() > 1
					? "%s x%d".formatted(product.getProductName(), item.getQuantity())
					: product.getProductName();
				return new CartQuoteItemPreview(categoryLabel(categoryName), name);
			})
			.toList();

		return new CartQuoteLineResponse(
			quote.getQuoteId(),
			quote.getQuoteName(),
			quote.getItems().size(),
			issueCount == 0,
			issueCount,
			previews,
			unitTotal * cartItem.getQuantity(),
			cartItem.isSelected()
		);
	}

	private String categoryLabel(String categoryName) {
		return CategoryDisplay.CATEGORY_TABS.stream()
			.filter(tab -> tab.name().equals(categoryName))
			.findFirst()
			.map(CategoryDisplay.CategoryTab::label)
			.orElse(categoryName);
	}

	private int categoryOrder(String categoryName) {
		for (int index = 0; index < CategoryDisplay.CATEGORY_TABS.size(); index++) {
			if (CategoryDisplay.CATEGORY_TABS.get(index).name().equals(categoryName)) {
				return index;
			}
		}
		return CategoryDisplay.CATEGORY_TABS.size();
	}

	/**
	 * quote-builder.js의 evaluateCompatibility()와 동일한 5개 규칙을 서버에서도 계산해
	 * 장바구니에 담긴 견적 카드에 "호환성 검사 통과/문제 N건"을 표시한다. 저장 시점 이후
	 * 상품 정보가 바뀌었을 가능성까지 감안해, 표시할 때마다 다시 계산한다.
	 */
	private int countCompatibilityIssues(List<QuoteItemEntity> items, Map<Long, ProductEntity> productsById) {
		Map<String, List<ProductEntity>> productsByCategory = new LinkedHashMap<>();
		for (QuoteItemEntity item : items) {
			ProductEntity product = productsById.get(item.getProductId());
			if (product == null) {
				continue;
			}
			productsByCategory
				.computeIfAbsent(product.getCategory().getCategoryName(), key -> new ArrayList<>())
				.add(product);
		}

		ProductEntity cpu = firstOf(productsByCategory, "CPU");
		ProductEntity board = firstOf(productsByCategory, "MAINBOARD");
		List<ProductEntity> ramList = productsByCategory.getOrDefault("RAM", List.of());
		ProductEntity gpu = firstOf(productsByCategory, "GPU");
		ProductEntity psu = firstOf(productsByCategory, "POWER_SUPPLY");
		ProductEntity pcCase = firstOf(productsByCategory, "CASE");

		int issues = 0;

		if (cpu != null && board != null
			&& cpu.getSocketType() != null && board.getSocketType() != null
			&& !cpu.getSocketType().equals(board.getSocketType())) {
			issues++;
		}

		if (board != null && board.getMemoryType() != null) {
			for (ProductEntity ram : ramList) {
				if (ram.getMemoryType() != null && !ram.getMemoryType().equals(board.getMemoryType())) {
					issues++;
				}
			}
		}

		if (board != null && pcCase != null) {
			Integer boardRank = formFactorRank(board.getFormFactor());
			Integer caseRank = formFactorRank(pcCase.getFormFactor());
			if (boardRank != null && caseRank != null && caseRank < boardRank) {
				issues++;
			}
		}

		if (gpu != null && pcCase != null && gpu.getGpuLengthMm() != null && pcCase.getMaxGpuLengthMm() != null
			&& gpu.getGpuLengthMm() > pcCase.getMaxGpuLengthMm()) {
			issues++;
		}

		if (psu != null && psu.getPowerCapacityWatt() != null
			&& (cpu != null && cpu.getPowerConsumption() != null
				|| gpu != null && gpu.getRecommendedPower() != null)) {
			int cpuPower = cpu != null && cpu.getPowerConsumption() != null ? cpu.getPowerConsumption() : 0;
			int gpuPower = gpu != null && gpu.getRecommendedPower() != null ? gpu.getRecommendedPower() : 0;
			int required = (int) Math.ceil((cpuPower + gpuPower) * 1.2);
			if (required > psu.getPowerCapacityWatt()) {
				issues++;
			}
		}

		return issues;
	}

	private ProductEntity firstOf(Map<String, List<ProductEntity>> productsByCategory, String category) {
		List<ProductEntity> products = productsByCategory.get(category);
		return products == null || products.isEmpty() ? null : products.get(0);
	}

	private Integer formFactorRank(String formFactor) {
		if (formFactor == null || formFactor.isBlank()) {
			return null;
		}
		String upper = formFactor.toUpperCase(Locale.ROOT);
		if (EATX_PATTERN.matcher(upper).find()) {
			return 5;
		}
		if (MICRO_ATX_PATTERN.matcher(upper).find()) {
			return 3;
		}
		if (DTX_PATTERN.matcher(upper).find()) {
			return 2;
		}
		if (ITX_PATTERN.matcher(upper).find()) {
			return 1;
		}
		if (ATX_PATTERN.matcher(upper).find()) {
			return 4;
		}
		return null;
	}

	private CartProductItemEntity findOwnedProductItem(String loginId, Long productId) {
		Long memberId = activeMemberId(loginId);
		CartEntity cart = cartRepository.findByMemberId(memberId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니가 비어 있습니다."));
		return cartProductItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니에서 상품을 찾을 수 없습니다."));
	}

	private CartQuoteItemEntity findOwnedQuoteItem(String loginId, Long quoteId) {
		Long memberId = activeMemberId(loginId);
		CartEntity cart = cartRepository.findByMemberId(memberId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니가 비어 있습니다."));
		return cartQuoteItemRepository.findByCartCartIdAndQuoteId(cart.getCartId(), quoteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니에서 견적을 찾을 수 없습니다."));
	}

	private CartEntity findOrCreateCart(Long memberId) {
		return cartRepository.findByMemberId(memberId)
			.orElseGet(() -> cartRepository.save(CartEntity.create(memberId)));
	}

	private Long activeMemberId(String loginId) {
		return memberLookupRepository.findActiveMemberIdByLoginId(loginId)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"로그인한 회원 정보를 찾을 수 없습니다."
			));
	}
}
