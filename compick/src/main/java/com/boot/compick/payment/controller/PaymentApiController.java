package com.boot.compick.payment.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.cart.repository.CartMemberLookupRepository;
import com.boot.compick.payment.dto.KakaoReadyRequest;
import com.boot.compick.payment.dto.KakaoReadyResult;
import com.boot.compick.payment.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {

	private final PaymentService paymentService;
	private final CartMemberLookupRepository memberLookupRepository;

	public PaymentApiController(
		PaymentService paymentService,
		CartMemberLookupRepository memberLookupRepository
	) {
		this.paymentService = paymentService;
		this.memberLookupRepository = memberLookupRepository;
	}

	@PostMapping("/kakao/ready")
	public KakaoReadyResult ready(
		@Valid @RequestBody KakaoReadyRequest request,
		Principal principal,
		HttpServletRequest servletRequest
	) {
		Long memberId = activeMemberId(principal.getName());
		String originUrl = originUrl(servletRequest);
		String redirectUrl = paymentService.readyKakao(
			principal.getName(),
			request.orderNumber(),
			memberId,
			originUrl
		);
		return new KakaoReadyResult(redirectUrl);
	}

	private String originUrl(HttpServletRequest request) {
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
	}

	private Long activeMemberId(String loginId) {
		return memberLookupRepository.findActiveMemberIdByLoginId(loginId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "로그인한 회원 정보를 찾을 수 없습니다."));
	}
}
