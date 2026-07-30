package com.boot.compick.member.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.compick.member.dto.DuplicateCheckResponse;
import com.boot.compick.member.service.MemberService;

@RestController
@RequestMapping("/api/members")
public class MemberApiController {

	private final MemberService memberService;

	public MemberApiController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping("/check-login-id")
	public DuplicateCheckResponse checkLoginId(@RequestParam String loginId) {
		return new DuplicateCheckResponse(
			memberService.isLoginIdAvailable(loginId)
		);
	}

	@GetMapping("/check-email")
	public DuplicateCheckResponse checkEmail(@RequestParam String email) {
		return new DuplicateCheckResponse(
			memberService.isEmailAvailable(email)
		);
	}
}
