package com.boot.compick.home.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Model model) {
		/*
		 * 화면 구현 단계에서 사용하는 임시 데이터입니다.
		 * 상품 기능이 구현되면 ProductService의 인기 상품 조회 결과로 교체합니다.
		 */
		model.addAttribute("popularProducts", List.of(
			new PopularProductView(1L, "인기 제조사", "Ryzen 7 7800X3D", 219000),
			new PopularProductView(2L, "인기 제조사", "RTX 4070 SUPER", 289000),
			new PopularProductView(3L, "인기 제조사", "DDR5 32GB", 359000),
			new PopularProductView(4L, "인기 제조사", "NVMe SSD 1TB", 429000)
		));
		return "home/index";
	}

	private record PopularProductView(
		Long productId,
		String brand,
		String name,
		int price
	) {
	}
}
