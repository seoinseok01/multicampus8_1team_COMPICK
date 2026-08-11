package com.boot.compick.member.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class JoinForm {
    @NotBlank(message = "아이디를 입력해 주세요.")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "아이디는 영문과 숫자 4~20자로 입력해 주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$", message = "비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다.")
    private String password;
    @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
    private String passwordConfirm;

    @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
    @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 50)
    private String name;
    @NotBlank(message = "휴대전화 번호를 입력해 주세요.")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대전화 번호를 입력해 주세요.")
    private String phone;
    @AssertTrue(message = "이용약관과 개인정보 처리방침에 동의해 주세요.")
    private boolean termsAccepted;
}
