package com.boot.compick.member.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProfileForm {
    @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 50)
    private String name;
    @NotBlank(message = "휴대전화 번호를 입력해 주세요.")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대전화 번호를 입력해 주세요.")
    private String phone;
}
