package com.boot.compick.cart.controller;

import java.security.Principal;
import com.boot.compick.cart.service.CartQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CartQuoteController {
    private final CartQuoteService cartQuoteService;

    @PostMapping("/cart/quotes")
    public String add(Principal principal, @RequestParam Long quoteId,
            RedirectAttributes redirectAttributes) {
        cartQuoteService.add(principal.getName(), quoteId);
        redirectAttributes.addFlashAttribute("message", "AI 견적을 장바구니에 담았습니다.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/quotes/selection")
    public String select(Principal principal,
            @RequestParam(required = false) List<Long> cartItemIds,
            @RequestParam(defaultValue = "false") boolean order,
            RedirectAttributes redirectAttributes) {
        cartQuoteService.select(principal.getName(), cartItemIds);
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "주문할 견적서를 선택해 주세요.");
            return "redirect:/cart";
        }
        return order ? "redirect:/orders/new" : "redirect:/cart";
    }
}
