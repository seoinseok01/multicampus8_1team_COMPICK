package com.boot.compick.order.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.boot.compick.cart.entity.CartEntity;
import com.boot.compick.cart.repository.CartProductItemRepository;
import com.boot.compick.cart.repository.CartRepository;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.order.dto.*;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutService {
	private final MemberService memberService;
	private final CartRepository cartRepository;
	private final CartProductItemRepository itemRepository;
	private final ProductRepository productRepository;

	public CheckoutView getCheckout(String loginId) {
		return getCheckout(memberService.findActiveByLoginId(loginId).getId());
	}

	public CheckoutView getCheckout(Long memberId) {
		CartEntity cart = cartRepository.findByMemberId(memberId).orElse(null);
		if (cart == null) return new CheckoutView(List.of(), 0, 0, 0);
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
		long amount = items.stream().mapToLong(CheckoutItem::lineAmount).sum();
		return new CheckoutView(items, amount, 0, amount);
	}
}
