package com.boot.compick.cart.service;

import com.boot.compick.cart.entity.CartEntity;
import com.boot.compick.cart.entity.CartQuoteItemEntity;
import com.boot.compick.cart.repository.CartQuoteItemRepository;
import com.boot.compick.cart.repository.CartRepository;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.service.AiQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartQuoteService {
    private final MemberService memberService;
    private final AiQuoteService quoteService;
    private final CartRepository cartRepository;
    private final CartQuoteItemRepository cartQuoteItemRepository;

    @Transactional
    public void add(String loginId, Long quoteId) {
        Member member = memberService.findActiveByLoginId(loginId);
        QuoteEntity quote = quoteService.findOwned(loginId, quoteId);
        CartEntity cart = cartRepository.findByMemberId(member.getId())
                .orElseGet(() -> cartRepository.save(CartEntity.create(member.getId())));
        if (!cartQuoteItemRepository.existsByCartCartIdAndQuoteId(cart.getCartId(), quoteId)) {
            cartQuoteItemRepository.save(CartQuoteItemEntity.create(cart, quote));
        }
    }

    @Transactional
    public void select(String loginId, List<Long> cartItemIds) {
        Long memberId = memberService.findActiveByLoginId(loginId).getId();
        CartEntity cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니를 찾을 수 없습니다."));
        var selectedIds = new HashSet<>(cartItemIds == null ? List.<Long>of() : cartItemIds);
        cartQuoteItemRepository.findAllByCartCartIdOrderById(cart.getCartId())
                .forEach(item -> item.select(selectedIds.contains(item.getId())));
    }

    @Transactional
    public void removeSelected(Long memberId) {
        cartRepository.findByMemberId(memberId).ifPresent(cart ->
                cartQuoteItemRepository.deleteAllByCartCartIdAndSelected(cart.getCartId(), "Y"));
    }

	@Transactional
	public void deleteItem(String loginId, Long cartItemId) {
		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		cartRepository.findByMemberId(memberId).ifPresent(cart ->
			cartQuoteItemRepository.deleteByCartCartIdAndId(cart.getCartId(), cartItemId));
	}

	@Transactional
	public void deleteItems(String loginId, List<Long> cartItemIds) {
		if (cartItemIds == null || cartItemIds.isEmpty()) return;
		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		cartRepository.findByMemberId(memberId).ifPresent(cart ->
			cartQuoteItemRepository.deleteAllByCartCartIdAndIdIn(cart.getCartId(), cartItemIds));
	}
}
