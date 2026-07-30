package com.boot.compick.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
	@Size(max = 50)
	String addressName,

	@NotBlank(message = "받는 사람을 입력해 주세요.")
	@Size(max = 50)
	String recipientName,

	@NotBlank(message = "휴대전화 번호를 입력해 주세요.")
	@Pattern(
		regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
		message = "올바른 휴대전화 번호를 입력해 주세요."
	)
	String phone,

	@NotBlank(message = "우편번호를 입력해 주세요.")
	@Pattern(regexp = "^\\d{5}$", message = "우편번호 5자리를 입력해 주세요.")
	String zipCode,

	@NotBlank(message = "기본 주소를 입력해 주세요.")
	@Size(max = 255)
	String address1,

	@Size(max = 255)
	String address2,

	boolean isDefault
) {
	public String normalizedAddressName() {
		return addressName == null || addressName.isBlank()
			? "배송지"
			: addressName.trim();
	}
}
