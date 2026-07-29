package com.boot.compick.member.service;

import com.boot.compick.member.dto.*;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void join(JoinForm form) {
        if (memberRepository.existsByLoginId(form.getLoginId())) throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        if (memberRepository.existsByEmail(form.getEmail())) throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        if (!form.getPassword().equals(form.getPasswordConfirm())) throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        memberRepository.save(new Member(form.getLoginId(), passwordEncoder.encode(form.getPassword()),
                form.getName(), form.getEmail(), form.getPhone()));
    }

    public String findMaskedLoginId(String name, String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String loginId = memberRepository.findByNameAndEmailAndStatus(name, email,
                        MemberStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원정보가 없습니다."))
                .getLoginId();
        int visible = Math.min(4, Math.max(1, loginId.length() / 2));
        return loginId.substring(0, visible) + "*".repeat(Math.max(1, loginId.length() - visible));
    }

    public void validatePasswordResetMember(String loginId, String rawEmail) {
        memberRepository.findByLoginIdAndEmailAndStatus(loginId, rawEmail.trim().toLowerCase(Locale.ROOT),
                        MemberStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원정보가 없습니다."));
    }

    @Transactional
    public void resetPassword(String loginId, String rawEmail, String newPassword, String confirm) {
        if (!newPassword.equals(confirm)) throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$"))
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다.");
        Member member = memberRepository.findByLoginIdAndEmailAndStatus(loginId,
                        rawEmail.trim().toLowerCase(Locale.ROOT), MemberStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원정보가 없습니다."));
        member.changePassword(passwordEncoder.encode(newPassword));
    }

    public Member findActiveByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("활성 회원을 찾을 수 없습니다."));
    }

    public boolean isLoginIdAvailable(String loginId) {
        return !memberRepository.existsByLoginId(loginId);
    }

    public ProfileForm getProfile(String loginId) {
        Member member = findActiveByLoginId(loginId);
        ProfileForm form = new ProfileForm();
        form.setName(member.getName()); form.setPhone(member.getPhone());
        return form;
    }

    @Transactional
    public void updateProfile(String loginId, ProfileForm form) {
        Member member = findActiveByLoginId(loginId);
        member.updateProfile(form.getName(), form.getPhone());
    }

    @Transactional
    public void changePassword(String loginId, PasswordForm form) {
        Member member = findActiveByLoginId(loginId);
        if (!passwordEncoder.matches(form.getCurrentPassword(), member.getPasswordHash())) throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        if (!form.getNewPassword().equals(form.getNewPasswordConfirm())) throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        member.changePassword(passwordEncoder.encode(form.getNewPassword()));
    }

    @Transactional
    public void withdraw(String loginId, String password) {
        Member member = findActiveByLoginId(loginId);
        if (!passwordEncoder.matches(password, member.getPasswordHash())) throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        member.withdraw();
    }
}
