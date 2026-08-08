package com.boot.compick.admin;

/**
 * 관리자 화면이라도 회원 개인정보(이메일·연락처·배송지)를 원본 그대로 보여주지 않고
 * 일부만 남기고 나머지는 가린다. Thymeleaf 템플릿에서 T(...) 정적 호출로 사용한다.
 */
public final class AdminPrivacyMasking {

	private AdminPrivacyMasking() {
	}

	public static String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return email;
		}
		int at = email.indexOf('@');
		if (at <= 0) {
			return email;
		}
		String local = email.substring(0, at);
		String domain = email.substring(at);
		String visible = local.substring(0, Math.min(2, local.length()));
		return visible + "*".repeat(Math.max(local.length() - visible.length(), 2)) + domain;
	}

	public static String maskPhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return phone;
		}
		String digits = phone.replaceAll("\\D", "");
		if (digits.length() < 7) {
			return phone;
		}
		return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
	}

	/** 시/도·구/군까지만 보여주고 나머지 상세 주소는 가린다. */
	public static String maskAddress(String basicAddress) {
		if (basicAddress == null || basicAddress.isBlank()) {
			return basicAddress;
		}
		String[] parts = basicAddress.trim().split("\\s+");
		int visibleParts = Math.min(2, parts.length);
		StringBuilder region = new StringBuilder();
		for (int i = 0; i < visibleParts; i++) {
			if (i > 0) {
				region.append(' ');
			}
			region.append(parts[i]);
		}
		return region + " 이하 비공개";
	}
}
