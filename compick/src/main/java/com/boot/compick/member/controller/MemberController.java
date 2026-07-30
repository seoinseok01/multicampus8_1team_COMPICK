package com.boot.compick.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/members")
public class MemberController {

	@GetMapping("/login")
	public String loginForm(@RequestParam(required = false) String error,
							org.springframework.ui.Model model) {
		if (error != null) {
			model.addAttribute("errorMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
		}
		return "members/login";
	}
}
