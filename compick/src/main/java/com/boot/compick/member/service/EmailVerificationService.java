package com.boot.compick.member.service;

import com.boot.compick.member.entity.EmailVerification;
import com.boot.compick.member.entity.VerificationPurpose;
import com.boot.compick.member.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {
    private static final int MAX_ATTEMPTS = 5;
    private final EmailVerificationRepository repository;
    private final VerificationMailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void send(String rawEmail, VerificationPurpose purpose) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now();
        repository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .filter(v -> v.getCreatedAt().isAfter(now.minusMinutes(1)))
                .ifPresent(v -> { throw new IllegalArgumentException("인증번호는 1분 후 다시 요청할 수 있습니다."); });
        String code = "%06d".formatted(random.nextInt(1_000_000));
        repository.save(new EmailVerification(email, purpose, passwordEncoder.encode(code), now.plusMinutes(5), now));
        mailService.send(email, code, purpose);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void confirm(String rawEmail, VerificationPurpose purpose, String code) {
        EmailVerification verification = current(rawEmail, purpose);
        LocalDateTime now = LocalDateTime.now();
        validateUsable(verification, now);
        verification.increaseAttempt();
        if (!passwordEncoder.matches(code, verification.getCodeHash()))
            throw new IllegalArgumentException("인증번호가 올바르지 않습니다.");
        verification.verify(now);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void confirmAndConsume(String email, VerificationPurpose purpose, String code) {
        confirm(email, purpose, code);
        consumeVerified(email, purpose);
    }

    @Transactional
    public void consumeVerified(String rawEmail, VerificationPurpose purpose) {
        EmailVerification verification = current(rawEmail, purpose);
        LocalDateTime now = LocalDateTime.now();
        validateUsable(verification, now);
        if (verification.getVerifiedAt() == null) throw new IllegalArgumentException("이메일 인증을 완료해 주세요.");
        verification.use(now);
    }

    private EmailVerification current(String email, VerificationPurpose purpose) {
        return repository.findTopByEmailAndPurposeOrderByCreatedAtDesc(normalize(email), purpose)
                .orElseThrow(() -> new IllegalArgumentException("먼저 인증번호를 요청해 주세요."));
    }

    private void validateUsable(EmailVerification verification, LocalDateTime now) {
        if (verification.getUsedAt() != null) throw new IllegalArgumentException("이미 사용된 인증입니다.");
        if (verification.getExpiresAt().isBefore(now)) throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        if (verification.getAttemptCount() >= MAX_ATTEMPTS) throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다.");
    }

    public String normalize(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일을 입력해 주세요.");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        return normalized;
    }
}
