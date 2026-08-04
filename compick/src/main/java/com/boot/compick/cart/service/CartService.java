package com.boot.compick.cart.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.cart.dto.AddCartProductRequest;
import com.boot.compick.cart.dto.AddCartProductResponse;
import com.boot.compick.cart.entity.CartEntity;
import com.boot.compick.cart.entity.CartProductItemEntity;
import com.boot.compick.cart.repository.CartMemberLookupRepository;
import com.boot.compick.cart.repository.CartProductItemRepository;
import com.boot.compick.cart.repository.CartProductLookupRepository;
import com.boot.compick.cart.repository.CartRepository;

@Service
@Transactional(readOnly = true)
public class CartService {

	private final CartRepository cartRepository;
	private final CartProductItemRepository cartProductItemRepository;
	private final CartMemberLookupRepository memberLookupRepository;
	private final CartProductLookupRepository productLookupRepository;

	public CartService(
		CartRepository cartRepository,
		CartProductItemRepository cartProductItemRepository,
		CartMemberLookupRepository memberLookupRepository,
		CartProductLookupRepository productLookupRepository
	) {
		this.cartRepository = cartRepository;
		this.cartProductItemRepository = cartProductItemRepository;
		this.memberLookupRepository = memberLookupRepository;
		this.productLookupRepository = productLookupRepository;
	}

	@Transactional
	public AddCartProductResponse addProduct(
		String loginId,
		AddCartProductRequest request
	) {
		Long memberId = memberLookupRepository
			.findActiveMemberIdByLoginId(loginId)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"로그인한 회원 정보를 찾을 수 없습니다."
			));

		if (!productLookupRepository.isPurchasable(
			request.productId(),
			request.quantity()
		)) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"판매 중인 상품이 아니거나 재고가 부족합니다."
			);
		}

		CartEntity cart = cartRepository.findByMemberId(memberId)
			.orElseGet(() -> cartRepository.save(CartEntity.create(memberId)));

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

	@Transactional
	public void select(String loginId, List<Long> cartItemIds) {
		Long memberId = memberLookupRepository.findActiveMemberIdByLoginId(loginId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
		CartEntity cart = cartRepository.findByMemberId(memberId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."));
		var selectedIds = new HashSet<>(cartItemIds == null ? List.<Long>of() : cartItemIds);
		cartProductItemRepository.findAllByCartCartIdOrderByCartProductItemId(cart.getCartId())
			.forEach(item -> item.select(selectedIds.contains(item.getCartProductItemId())));
	}

	@Transactional
	public void removeSelected(Long memberId) {
		cartRepository.findByMemberId(memberId).ifPresent(cart ->
			cartProductItemRepository.deleteAllByCartCartIdAndSelected(cart.getCartId(), "Y"));
	}

	@Transactional
	public void deleteItem(String loginId, Long cartItemId) {
		Long memberId = memberLookupRepository.findActiveMemberIdByLoginId(loginId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
		cartRepository.findByMemberId(memberId).ifPresent(cart ->
			cartProductItemRepository.deleteByCartCartIdAndCartProductItemId(cart.getCartId(), cartItemId));
	}

	@Transactional
	public void deleteItems(String loginId, List<Long> cartItemIds) {
		if (cartItemIds == null || cartItemIds.isEmpty()) return;
		Long memberId = memberLookupRepository.findActiveMemberIdByLoginId(loginId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
		cartRepository.findByMemberId(memberId).ifPresent(cart ->
			cartProductItemRepository.deleteAllByCartCartIdAndCartProductItemIdIn(cart.getCartId(), cartItemIds));
	}
}
