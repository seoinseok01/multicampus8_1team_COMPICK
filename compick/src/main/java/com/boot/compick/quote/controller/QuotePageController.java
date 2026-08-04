package com.boot.compick.quote.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.boot.compick.product.CategoryDisplay;
import com.boot.compick.quote.dto.QuoteItemView;
import com.boot.compick.quote.entity.PurposeTag;
import com.boot.compick.quote.service.QuoteService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class QuotePageController {

	private final QuoteService quoteService;
	private final ObjectMapper objectMapper;

	public QuotePageController(QuoteService quoteService, ObjectMapper objectMapper) {
		this.quoteService = quoteService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/quotes/new")
	public String newQuote(
		@RequestParam(required = false) Long basedOn,
		@RequestParam(required = false) Long productId,
		Principal principal,
		Model model
	) {
		model.addAttribute("categoryTabs", CategoryDisplay.CATEGORY_TABS);
		model.addAttribute("initialItemsJson", writeJson(initialItems(basedOn, productId, principal)));
		return "shopping/quote-new";
	}

	@GetMapping({"/preset", "/recommendations"})
	public String presetList(@RequestParam(required = false) String purpose, Model model) {
		PurposeTag purposeTag = parsePurposeTag(purpose);
		model.addAttribute("presets", quoteService.findPresets(purposeTag));
		model.addAttribute("activePurpose", purposeTag == null ? "" : purposeTag.name());
		return "shopping/preset-list";
	}

	@GetMapping({"/preset/{quoteId}", "/recommendations/{quoteId}"})
	public String presetDetail(@PathVariable Long quoteId, Model model) {
		model.addAttribute("preset", quoteService.findPresetDetail(quoteId));
		return "shopping/preset-detail";
	}

	private List<QuoteItemView> initialItems(Long basedOn, Long productId, Principal principal) {
		if (basedOn != null) {
			String loginId = principal == null ? null : principal.getName();
			return quoteService.findQuoteItemsForEditing(basedOn, loginId);
		}
		if (productId != null) {
			return List.of(quoteService.findProductAsQuoteItem(productId));
		}
		return List.of();
	}

	private PurposeTag parsePurposeTag(String purpose) {
		if (purpose == null || purpose.isBlank()) {
			return null;
		}
		try {
			return PurposeTag.valueOf(purpose);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception ignored) {
			return "[]";
		}
	}
}
