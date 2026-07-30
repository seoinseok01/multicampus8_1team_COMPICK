package com.boot.compick.home.controller;

<<<<<<< HEAD
=======
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

<<<<<<< HEAD
=======
import com.boot.compick.member.service.MemberService;
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
import com.boot.compick.product.service.ProductService;

@Controller
public class HomeController {

	private final ProductService productService;
<<<<<<< HEAD

	public HomeController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("popularProducts", productService.findPopularProducts());
		return "home/index";
	}
=======
	private final MemberService memberService;

	public HomeController(
		ProductService productService,
		MemberService memberService
	) {
		this.productService = productService;
		this.memberService = memberService;
	}

	@GetMapping("/")
	public String home(Authentication authentication, Model model) {
		model.addAttribute("popularProducts", productService.findPopularProducts());
		if (isLoggedIn(authentication)) {
			model.addAttribute(
				"phoneMissing",
				memberService.isPhoneMissing(authentication.getName())
			);
		}
		return "home/index";
	}

	private boolean isLoggedIn(Authentication authentication) {
		return authentication != null
			&& authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken);
	}
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
}
