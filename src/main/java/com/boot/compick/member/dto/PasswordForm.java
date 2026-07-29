package com.boot.compick.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PasswordForm {
    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    private String currentPassword;
    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$", message = "새 비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다.")
    private String newPassword;
    @NotBlank(message = "새 비밀번호 확인을 입력해 주세요.")
    private String newPasswordConfirm;
}
