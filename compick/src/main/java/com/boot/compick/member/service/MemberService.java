package com.boot.compick.member.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.compick.member.dto.MemberSummaryResponse;
import com.boot.compick.member.dto.ProfileForm;
import com.boot.compick.member.dto.SignupForm;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.repository.MemberRepository;
import com.boot.compick.member.repository.SocialAccountRepository;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final PasswordEncoder passwordEncoder;

	public MemberService(
		MemberRepository memberRepository,
		SocialAccountRepository socialAccountRepository,
		PasswordEncoder passwordEncoder
	) {
		this.memberRepository = memberRepository;
		this.socialAccountRepository = socialAccountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void signup(SignupForm form) {
		String loginId = form.getLoginId().trim();
		String email = normalizeEmail(form.getEmail());

		if (memberRepository.existsByLoginId(loginId)) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}
		if (memberRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
		}
		if (!form.getPassword().equals(form.getPasswordConfirm())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}

		memberRepository.save(new Member(
			loginId,
			passwordEncoder.encode(form.getPassword()),
			form.getName().trim(),
			email,
			form.getNickname().trim(),
			form.getPhone().trim()
		));
	}

	public boolean isLoginIdAvailable(String loginId) {
		return loginId != null
			&& !loginId.isBlank()
			&& !memberRepository.existsByLoginId(loginId.trim());
	}

	public boolean isEmailAvailable(String email) {
		return email != null
			&& !email.isBlank()
			&& !memberRepository.existsByEmail(normalizeEmail(email));
	}

	public Member findActiveByLoginId(String loginId) {
		return memberRepository.findByLoginId(loginId)
			.filter(member -> member.getStatus() == MemberStatus.ACTIVE)
			.orElseThrow(() -> new IllegalArgumentException("활성 회원을 찾을 수 없습니다."));
	}

	public MemberSummaryResponse getSummary(String loginId) {
		Member member = findActiveByLoginId(loginId);
		return new MemberSummaryResponse(
			member.getId(),
			member.getLoginId(),
			member.getName(),
			member.getEmail(),
			member.getNickname(),
			member.getPhone()
		);
	}

	public boolean isPhoneMissing(String loginId) {
		return memberRepository.findByLoginId(loginId)
			.filter(member -> member.getStatus() == MemberStatus.ACTIVE)
			.map(member -> {
				String phone = member.getPhone();
				return phone == null
					|| phone.isBlank()
					|| "미등록".equals(phone);
			})
			.orElse(false);
	}

	public ProfileForm getProfile(String loginId) {
		Member member = findActiveByLoginId(loginId);
		ProfileForm form = new ProfileForm();
		form.setEmail(member.getEmail());
		form.setNickname(member.getNickname());
		form.setPhone(member.getPhone());
		return form;
	}

	@Transactional
	public void updateProfile(String loginId, ProfileForm form) {
		Member member = findActiveByLoginId(loginId);
		String email = normalizeEmail(form.getEmail());

		if (!passwordEncoder.matches(
			form.getCurrentPassword(),
			member.getPasswordHash()
		)) {
			throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
		}
		if (!member.getEmail().equals(email)
			&& memberRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
		}

		String newPassword = form.getNewPassword() == null
			? ""
			: form.getNewPassword();
		String confirmPassword = form.getNewPasswordConfirm() == null
			? ""
			: form.getNewPasswordConfirm();

		if (!newPassword.isBlank()) {
			if (!newPassword.equals(confirmPassword)) {
				throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
			}
			member.changePassword(passwordEncoder.encode(newPassword));
		}

		member.updateProfile(
			email,
			form.getNickname().trim(),
			form.getPhone().trim()
		);
	}

	@Transactional
	public boolean withdraw(String loginId, String currentPassword) {
		Member member = findActiveByLoginId(loginId);
		if (!passwordEncoder.matches(currentPassword, member.getPasswordHash())) {
			throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
		}
		boolean socialConnectionRemoved =
			socialAccountRepository.deleteByMemberId(member.getId()) > 0;
		member.withdraw();
		return socialConnectionRemoved;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
