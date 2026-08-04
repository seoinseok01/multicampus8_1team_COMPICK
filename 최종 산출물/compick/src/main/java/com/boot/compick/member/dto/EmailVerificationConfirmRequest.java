package com.boot.compick.member.dto;

import com.boot.compick.member.entity.VerificationPurpose;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationConfirmRequest(
	@NotBlank(message = "이메일을 입력해 주세요.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	String email,

	@NotNull(message = "인증 목적을 지정해 주세요.")
	VerificationPurpose purpose,

	@NotBlank(message = "인증번호를 입력해 주세요.")
	@Pattern(regexp = "^\\d{6}$", message = "인증번호 6자리를 입력해 주세요.")
	String code
) {
}
