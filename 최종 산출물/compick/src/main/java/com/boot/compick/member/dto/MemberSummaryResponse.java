package com.boot.compick.member.dto;

public record MemberSummaryResponse(
	Long memberId,
	String loginId,
	String name,
	String email,
	String nickname,
	String phone
) {
	public String maskedEmail() {
		int at = email.indexOf('@');
		if (at <= 0) {
			return email;
		}
		String local = email.substring(0, at);
		String visible = local.substring(0, Math.min(3, local.length()));
		return visible + "***" + email.substring(at);
	}
}
