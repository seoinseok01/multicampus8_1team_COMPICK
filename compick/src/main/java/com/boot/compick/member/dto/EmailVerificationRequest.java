package com.boot.compick.member.dto;

import com.boot.compick.member.entity.VerificationPurpose;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailVerificationRequest(
	@NotBlank(message = "이메일을 입력해 주세요.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	String email,

	@NotNull(message = "인증 목적을 지정해 주세요.")
	VerificationPurpose purpose
) {
}
