package com.boot.compick.member.service;

import com.boot.compick.member.entity.VerificationPurpose;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationMailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${compick.mail.mode:log}")
    private String mode;
    @Value("${spring.mail.username:}")
    private String from;

    public void send(String email, String code, VerificationPurpose purpose) {
        if (!"smtp".equalsIgnoreCase(mode)) {
            log.info("[COMPICK 이메일 인증] email={}, purpose={}, code={}", email, purpose, code);
            return;
        }
        if (from == null || from.isBlank()) throw new IllegalStateException("SMTP_USERNAME 설정이 필요합니다.");
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) throw new IllegalStateException("SMTP 메일 설정이 필요합니다.");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject(subject(purpose));
            helper.setText(html(code, purpose), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[COMPICK 인증 메일 발송 실패] email={}, purpose={}, type={}, cause={}",
                    email, purpose, e.getClass().getSimpleName(), rootMessage(e), e);
            throw new IllegalStateException("인증 메일을 발송하지 못했습니다. 잠시 후 다시 시도해 주세요.", e);
        }
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage();
    }

    private String subject(VerificationPurpose purpose) {
        return purpose == VerificationPurpose.PASSWORD_RESET
                ? "[COMPICK] 비밀번호 재설정 인증번호입니다"
                : "[COMPICK] 이메일 인증번호를 확인해 주세요";
    }

    private String html(String code, VerificationPurpose purpose) {
        String description = switch (purpose) {
            case FIND_ID -> "COMPICK 아이디 찾기를 위한 인증번호입니다.";
            case PASSWORD_RESET -> "COMPICK 비밀번호 재설정을 위한 인증번호입니다.";
        };
        return """
                <!doctype html><html lang="ko"><body style="margin:0;background:#f4f7fc;font-family:Arial,sans-serif;color:#172033">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"><tr><td align="center" style="padding:40px 16px">
                <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="max-width:560px;background:#fff;border:1px solid #d5deed;border-radius:16px">
                <tr><td style="padding:36px 40px"><div style="font-size:24px;font-weight:800;color:#2563eb">COMPICK</div>
                <h1 style="font-size:26px;margin:36px 0 12px">이메일 인증</h1><p style="color:#6b7892;line-height:1.7">%s</p>
                <div style="margin:30px 0;padding:24px;text-align:center;background:#eaf1ff;border-radius:12px">
                <div style="font-size:13px;color:#6b7892;margin-bottom:10px">인증번호</div><strong style="font-size:34px;letter-spacing:8px;color:#2563eb">%s</strong></div>
                <p style="font-size:14px;line-height:1.7;color:#6b7892">인증번호는 발급 후 5분 동안 사용할 수 있습니다.<br>본인이 요청하지 않았다면 이 메일을 무시해 주세요.</p>
                </td></tr></table><p style="font-size:12px;color:#9aa5b8">© 2026 COMPICK</p></td></tr></table></body></html>
                """.formatted(description, code);
    }
}
