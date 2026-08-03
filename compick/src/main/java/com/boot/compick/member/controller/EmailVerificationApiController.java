package com.boot.compick.member.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.member.dto.EmailVerificationConfirmRequest;
import com.boot.compick.member.dto.EmailVerificationRequest;
import com.boot.compick.member.entity.VerificationPurpose;
import com.boot.compick.member.service.EmailVerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/email-verifications")
public class EmailVerificationApiController {

	private final EmailVerificationService emailVerificationService;

	public EmailVerificationApiController(EmailVerificationService emailVerificationService) {
		this.emailVerificationService = emailVerificationService;
	}

	@PostMapping("/send")
	public Map<String, String> send(@Valid @RequestBody EmailVerificationRequest request) {
		requireSignup(request.purpose());
		call(() -> emailVerificationService.send(request.email(), request.purpose()));
		return Map.of("message", "인증번호를 발송했습니다.");
	}

	@PostMapping("/confirm")
	public Map<String, String> confirm(@Valid @RequestBody EmailVerificationConfirmRequest request) {
		requireSignup(request.purpose());
		call(() -> emailVerificationService.confirm(
			request.email(),
			request.purpose(),
			request.code()
		));
		return Map.of("message", "이메일 인증이 완료되었습니다.");
	}

	private void requireSignup(VerificationPurpose purpose) {
		if (purpose != VerificationPurpose.SIGN_UP) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 인증 목적입니다.");
		}
	}

	private void call(Runnable action) {
		try {
			action.run();
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
		}
	}
}
