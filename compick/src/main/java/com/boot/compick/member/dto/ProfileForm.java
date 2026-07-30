package com.boot.compick.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileForm {

	@NotBlank(message = "이메일을 입력해 주세요.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100)
	private String email;

	@NotBlank(message = "닉네임을 입력해 주세요.")
	@Size(max = 20)
	private String nickname;

	@NotBlank(message = "휴대전화 번호를 입력해 주세요.")
	@Pattern(
		regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
		message = "올바른 휴대전화 번호를 입력해 주세요."
	)
	private String phone;

	@NotBlank(message = "현재 비밀번호를 입력해 주세요.")
	private String currentPassword;

	@Pattern(
		regexp = "^$|^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
		message = "새 비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다."
	)
	private String newPassword = "";

	private String newPasswordConfirm = "";

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public String getCurrentPassword() {
		return currentPassword;
	}

	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getNewPasswordConfirm() {
		return newPasswordConfirm;
	}

	public void setNewPasswordConfirm(String newPasswordConfirm) {
		this.newPasswordConfirm = newPasswordConfirm;
	}
}
