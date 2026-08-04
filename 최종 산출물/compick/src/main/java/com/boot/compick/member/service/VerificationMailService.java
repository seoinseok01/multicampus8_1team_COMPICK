package com.boot.compick.member.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.boot.compick.member.entity.VerificationPurpose;

import jakarta.mail.internet.MimeMessage;

@Service
public class VerificationMailService {

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final String from;

	public VerificationMailService(
		ObjectProvider<JavaMailSender> mailSenderProvider,
		@Value("${spring.mail.username:}") String from
	) {
		this.mailSenderProvider = mailSenderProvider;
		this.from = from;
	}

	public void send(String email, String code, VerificationPurpose purpose) {
		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (mailSender == null || from.isBlank()) {
			throw new IllegalArgumentException("메일 발송 설정이 되어 있지 않습니다.");
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
			helper.setFrom(from);
			helper.setTo(email);
			helper.setSubject("[COMPICK] 이메일 인증번호입니다.");
			helper.setText(buildHtml(code, purpose), true);
			mailSender.send(message);
		} catch (Exception exception) {
			throw new IllegalArgumentException(
				"인증 메일을 발송하지 못했습니다. 잠시 후 다시 시도해 주세요.",
				exception
			);
		}
	}

	private String buildHtml(String code, VerificationPurpose purpose) {
		String label = switch (purpose) {
			case SIGN_UP -> "회원가입";
			case FIND_ID -> "아이디 찾기";
			case PASSWORD_RESET -> "비밀번호 재설정";
		};
		return "<div style='font-family:Arial,sans-serif;max-width:560px;padding:32px'>"
			+ "<h1 style='color:#2563eb'>COMPICK</h1>"
			+ "<p>" + label + " 인증번호입니다.</p>"
			+ "<div style='font-size:32px;font-weight:700;letter-spacing:8px;"
			+ "padding:24px;background:#eef4ff;text-align:center'>" + code + "</div>"
			+ "<p>5분 안에 입력해 주세요.</p>"
			+ "</div>";
	}
}
