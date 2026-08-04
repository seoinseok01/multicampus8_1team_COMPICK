package com.boot.compick.order.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.boot.compick.cart.entity.CartEntity;
import com.boot.compick.cart.dto.CartQuoteView;
import com.boot.compick.cart.dto.CartProductView;
import com.boot.compick.cart.dto.CartView;
import com.boot.compick.cart.repository.CartProductItemRepository;
import com.boot.compick.cart.repository.CartQuoteItemRepository;
import com.boot.compick.cart.repository.CartRepository;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.order.dto.*;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.repository.QuoteItemRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutService {
	private final MemberService memberService;
	private final CartRepository cartRepository;
	private final CartProductItemRepository itemRepository;
	private final CartQuoteItemRepository cartQuoteItemRepository;
	private final ProductRepository productRepository;
	private final QuoteItemRepository quoteItemRepository;

	public CheckoutView getCheckout(String loginId) {
		return getCheckout(memberService.findActiveByLoginId(loginId).getId());
	}

	public CheckoutView getCheckout(Long memberId) {
		CartEntity cart = cartRepository.findByMemberId(memberId).orElse(null);
		if (cart == null) return new CheckoutView(List.of(), List.of(), 0, 0, 0);
		var cartItems = itemRepository.findAllByCartCartIdAndSelectedOrderByCartProductItemId(cart.getCartId(), "Y");
		Map<Long, ProductEntity> products = new HashMap<>();
		productRepository.findAllById(cartItems.stream().map(i -> i.getProductId()).toList())
			.forEach(p -> products.put(p.getProductId(), p));
		List<CheckoutItem> items = cartItems.stream().map(item -> {
			ProductEntity p = products.get(item.getProductId());
			if (p == null) return null;
			return new CheckoutItem(p.getProductId(), p.getProductName(), p.getBrand(),
				p.getImageUrl(), p.getPrice(), item.getQuantity(), p.getPrice() * item.getQuantity());
		}).filter(Objects::nonNull).toList();

		var cartQuotes = cartQuoteItemRepository
			.findAllByCartCartIdAndSelectedOrderById(cart.getCartId(), "Y");
		var quoteItems = quoteItemRepository.findAllByQuoteQuoteIdInOrderByQuoteItemId(
			cartQuotes.stream().map(item -> item.getQuote().getId()).toList());
		productRepository.findAllById(quoteItems.stream().map(item -> item.getProductId()).toList())
			.forEach(product -> products.put(product.getProductId(), product));
		Map<Long, List<com.boot.compick.quote.entity.QuoteItemEntity>> itemsByQuote =
			quoteItems.stream().collect(java.util.stream.Collectors.groupingBy(
				item -> item.getQuote().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));
		List<CheckoutQuote> quotes = cartQuotes.stream().map(cartQuote -> {
			var components = itemsByQuote.getOrDefault(cartQuote.getQuote().getId(), List.of()).stream()
				.map(item -> {
					ProductEntity product = products.get(item.getProductId());
					return product == null ? null : new CheckoutItem(product.getProductId(), product.getProductName(),
						product.getBrand(), product.getImageUrl(), product.getPrice(), item.getQuantity(),
						product.getPrice() * item.getQuantity());
				}).filter(Objects::nonNull)
				.toList();
			long unitAmount = components.stream().mapToLong(CheckoutItem::lineAmount).sum();
			return new CheckoutQuote(cartQuote.getQuote().getId(), cartQuote.getQuote().getName(),
				cartQuote.getQuantity(), components, unitAmount, unitAmount * cartQuote.getQuantity());
		}).toList();

		long amount = items.stream().mapToLong(CheckoutItem::lineAmount).sum()
			+ quotes.stream().mapToLong(CheckoutQuote::lineAmount).sum();
		return new CheckoutView(items, quotes, amount, 0, amount);
	}

	public CartView getCart(String loginId) {
		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		CartEntity cart = cartRepository.findByMemberId(memberId).orElse(null);
		if (cart == null) return new CartView(List.of(), List.of(), 0);
		Map<Long, ProductEntity> products = new HashMap<>();
		var cartProducts = itemRepository.findAllByCartCartIdOrderByCartProductItemId(cart.getCartId());
		productRepository.findAllById(cartProducts.stream().map(item -> item.getProductId()).toList())
			.forEach(product -> products.put(product.getProductId(), product));
		List<CartProductView> productViews = cartProducts.stream().map(item -> {
			ProductEntity product = products.get(item.getProductId());
			return product == null ? null : new CartProductView(item.getCartProductItemId(), product.getProductId(),
				product.getProductName(), product.getBrand(), product.getImageUrl(), product.getPrice(),
				item.getQuantity(), item.isSelected(), product.getPrice() * item.getQuantity());
		}).filter(Objects::nonNull).toList();
		var cartQuotes = cartQuoteItemRepository.findAllByCartCartIdOrderById(cart.getCartId());
		var quoteItems = quoteItemRepository.findAllByQuoteQuoteIdInOrderByQuoteItemId(
			cartQuotes.stream().map(item -> item.getQuote().getId()).toList());
		productRepository.findAllById(quoteItems.stream().map(item -> item.getProductId()).toList())
			.forEach(product -> products.put(product.getProductId(), product));
		Map<Long, List<com.boot.compick.quote.entity.QuoteItemEntity>> itemsByQuote =
			quoteItems.stream().collect(java.util.stream.Collectors.groupingBy(
				item -> item.getQuote().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));
		List<CartQuoteView> quotes = cartQuotes.stream().map(cartQuote -> {
			var components = itemsByQuote.getOrDefault(cartQuote.getQuote().getId(), List.of()).stream()
				.map(item -> {
					ProductEntity product = products.get(item.getProductId());
					return product == null ? null : new CheckoutItem(product.getProductId(), product.getProductName(),
						product.getBrand(), product.getImageUrl(), product.getPrice(), item.getQuantity(),
						product.getPrice() * item.getQuantity());
				}).filter(Objects::nonNull)
				.toList();
			long amount = components.stream().mapToLong(CheckoutItem::lineAmount).sum() * cartQuote.getQuantity();
			return new CartQuoteView(cartQuote.getId(), cartQuote.getQuote().getId(), cartQuote.getQuote().getName(),
				quoteTypeLabel(cartQuote.getQuote().getQuoteType()), cartQuote.getQuantity(),
				cartQuote.isSelected(), components, amount);
		}).toList();
		long selectedAmount = productViews.stream().filter(CartProductView::selected).mapToLong(CartProductView::lineAmount).sum()
			+ quotes.stream().filter(CartQuoteView::selected).mapToLong(CartQuoteView::lineAmount).sum();
		return new CartView(productViews, quotes, selectedAmount);
	}

	private String quoteTypeLabel(com.boot.compick.quote.entity.QuoteType quoteType) {
		return switch (quoteType) {
			case USER -> "견적서";
			case PRESET -> "추천 견적";
			case AI -> "AI 견적";
		};
	}
}
