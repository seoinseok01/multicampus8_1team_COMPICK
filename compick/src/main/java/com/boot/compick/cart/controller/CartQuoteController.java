package com.boot.compick.cart.controller;

import java.security.Principal;
import com.boot.compick.cart.service.CartQuoteService;
import com.boot.compick.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CartQuoteController {
    private final CartQuoteService cartQuoteService;
	private final CartService cartService;

    @PostMapping("/cart/quotes")
    public String add(Principal principal, @RequestParam Long quoteId,
            RedirectAttributes redirectAttributes) {
        cartQuoteService.add(principal.getName(), quoteId);
        redirectAttributes.addFlashAttribute("message", "AI 견적을 장바구니에 담았습니다.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/quotes/selection")
    public String select(Principal principal,
		@RequestParam(required = false) List<Long> productItemIds,
		@RequestParam(required = false) List<Long> quoteItemIds,
            @RequestParam(defaultValue = "false") boolean order,
            RedirectAttributes redirectAttributes) {
		cartService.select(principal.getName(), productItemIds);
		cartQuoteService.select(principal.getName(), quoteItemIds);
		boolean noneSelected = (productItemIds == null || productItemIds.isEmpty())
			&& (quoteItemIds == null || quoteItemIds.isEmpty());
		if (noneSelected) {
            redirectAttributes.addFlashAttribute("error", "주문할 상품 또는 견적서를 선택해 주세요.");
            return "redirect:/cart";
        }
        return order ? "redirect:/orders/new" : "redirect:/cart";
    }

	@PostMapping("/cart/products/{cartItemId}/delete")
	public String deleteProduct(Principal principal, @PathVariable Long cartItemId,
		RedirectAttributes redirectAttributes) {
		cartService.deleteItem(principal.getName(), cartItemId);
		redirectAttributes.addFlashAttribute("message", "상품을 장바구니에서 삭제했습니다.");
		return "redirect:/cart";
	}

	@PostMapping("/cart/quotes/{cartItemId}/delete")
	public String deleteQuote(Principal principal, @PathVariable Long cartItemId,
		RedirectAttributes redirectAttributes) {
		cartQuoteService.deleteItem(principal.getName(), cartItemId);
		redirectAttributes.addFlashAttribute("message", "견적서를 장바구니에서 삭제했습니다.");
		return "redirect:/cart";
	}

	@PostMapping("/cart/items/delete")
	public String deleteSelected(Principal principal,
		@RequestParam(required = false) List<Long> productItemIds,
		@RequestParam(required = false) List<Long> quoteItemIds,
		RedirectAttributes redirectAttributes) {
		boolean noneSelected = (productItemIds == null || productItemIds.isEmpty())
			&& (quoteItemIds == null || quoteItemIds.isEmpty());
		if (noneSelected) {
			redirectAttributes.addFlashAttribute("error", "삭제할 상품 또는 견적서를 선택해 주세요.");
			return "redirect:/cart";
		}
		cartService.deleteItems(principal.getName(), productItemIds);
		cartQuoteService.deleteItems(principal.getName(), quoteItemIds);
		redirectAttributes.addFlashAttribute("message", "선택한 항목을 장바구니에서 삭제했습니다.");
		return "redirect:/cart";
	}
}
