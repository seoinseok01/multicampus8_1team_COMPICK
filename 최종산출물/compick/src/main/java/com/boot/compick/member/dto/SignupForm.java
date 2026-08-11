package com.boot.compick.member.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupForm {

	@NotBlank(message = "아이디를 입력해 주세요.")
	@Pattern(
		regexp = "^[A-Za-z0-9]{4,20}$",
		message = "아이디는 영문과 숫자 4~20자로 입력해 주세요."
	)
	private String loginId;

	@NotBlank(message = "비밀번호를 입력해 주세요.")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
		message = "비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다."
	)
	private String password;

	@NotBlank(message = "비밀번호 확인을 입력해 주세요.")
	private String passwordConfirm;

	@NotBlank(message = "이메일을 입력해 주세요.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100)
	private String email;

	@NotBlank(message = "이름을 입력해 주세요.")
	@Size(max = 50)
	private String name;

	@NotBlank(message = "닉네임을 입력해 주세요.")
	@Size(max = 20)
	private String nickname;

	@NotBlank(message = "휴대전화 번호를 입력해 주세요.")
	@Pattern(
		regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
		message = "올바른 휴대전화 번호를 입력해 주세요."
	)
	private String phone;

	@AssertTrue(message = "이용약관과 개인정보 처리방침에 동의해 주세요.")
	private boolean termsAgreed;

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPasswordConfirm() {
		return passwordConfirm;
	}

	public void setPasswordConfirm(String passwordConfirm) {
		this.passwordConfirm = passwordConfirm;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public boolean isTermsAgreed() {
		return termsAgreed;
	}

	public void setTermsAgreed(boolean termsAgreed) {
		this.termsAgreed = termsAgreed;
	}
}
