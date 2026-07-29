package com.boot.compick.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddressForm {
    @NotBlank @Size(max = 50) private String addressName;
    @NotBlank @Size(max = 50) private String recipientName;
    @NotBlank @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대전화 번호를 입력해 주세요.")
    private String recipientPhone;
    @NotBlank @Pattern(regexp = "^\\d{5}$", message = "우편번호 5자리를 입력해 주세요.")
    private String zipCode;
    @NotBlank @Size(max = 255) private String basicAddress;
    @Size(max = 255) private String detailAddress;
    private boolean defaultAddress;
}
