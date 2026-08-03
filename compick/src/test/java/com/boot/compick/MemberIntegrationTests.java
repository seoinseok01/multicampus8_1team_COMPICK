package com.boot.compick;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.boot.compick.member.entity.EmailVerification;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.entity.VerificationPurpose;
import com.boot.compick.member.repository.AddressRepository;
import com.boot.compick.member.repository.EmailVerificationRepository;
import com.boot.compick.member.repository.MemberRepository;
import com.boot.compick.member.repository.SocialAccountRepository;
import com.boot.compick.member.service.SocialAccountService;

@SpringBootTest
@AutoConfigureMockMvc
class MemberIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AddressRepository addressRepository;

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Autowired
	private SocialAccountService socialAccountService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmailVerificationRepository emailVerificationRepository;

	@AfterEach
	void cleanUp() {
		addressRepository.deleteAll();
		socialAccountRepository.deleteAll();
		memberRepository.deleteAll();
		emailVerificationRepository.deleteAll();
	}

	private void markEmailVerified(String email, VerificationPurpose purpose) {
		LocalDateTime now = LocalDateTime.now();
		EmailVerification verification = new EmailVerification(
			email,
			purpose,
			passwordEncoder.encode("000000"),
			now.plusMinutes(5),
			now
		);
		verification.verify(now);
		emailVerificationRepository.save(verification);
	}

	@Test
	void signupAndLoginPagesArePublic() throws Exception {
		mockMvc.perform(get("/members/signup"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("회원가입")));

		mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("COMPICK 계정으로 로그인하세요.")));
	}

	@Test
	void signupStoresBcryptPassword() throws Exception {
		markEmailVerified("member@compick.test", VerificationPurpose.SIGN_UP);

		mockMvc.perform(post("/members/signup")
				.with(csrf())
				.param("loginId", "compick01")
				.param("password", "Password!1")
				.param("passwordConfirm", "Password!1")
				.param("email", "member@compick.test")
				.param("name", "컴픽회원")
				.param("nickname", "컴픽이")
				.param("phone", "010-1234-5678")
				.param("termsAgreed", "true"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));

		Member member = memberRepository.findByLoginId("compick01").orElseThrow();
		assertTrue(passwordEncoder.matches("Password!1", member.getPasswordHash()));
	}

	@Test
	void duplicateCheckReturnsAvailability() throws Exception {
		saveMember("takenId", "taken@compick.test");

		mockMvc.perform(get("/api/members/check-login-id")
				.param("loginId", "takenId"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.available").value(false));

		mockMvc.perform(get("/api/members/check-email")
				.param("email", "new@compick.test"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void springSecurityAuthenticatesDatabaseMember() throws Exception {
		saveMember("member01", "member01@compick.test");

		mockMvc.perform(post("/login")
				.with(csrf())
				.param("username", "member01")
				.param("password", "Password!1"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/"))
			.andExpect(authenticated().withUsername("member01"));
	}

	@Test
	void authenticatedMemberCanOpenMyPageAndCreateAddress() throws Exception {
		saveMember("member01", "member01@compick.test");

		mockMvc.perform(get("/mypage").with(user("member01").roles("USER")))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("마이페이지")));

		mockMvc.perform(post("/api/addresses")
				.with(user("member01").roles("USER"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"addressName": "우리 집",
						"recipientName": "컴픽회원",
						"phone": "010-1234-5678",
						"zipCode": "06236",
						"address1": "서울특별시 강남구 테헤란로",
						"address2": "101호",
						"isDefault": true
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.addressName").value("우리 집"))
			.andExpect(jsonPath("$.isDefault").value(true));
	}

	@Test
	void googleLoginCreatesAndReusesSocialMember() {
		SocialAccountService.GoogleLoginResult first =
			socialAccountService.loginGoogle(
				"google-subject-123",
				"GoogleUser@compick.test",
				"구글회원"
			);
		socialAccountService.setSocialPassword(
			first.member().getLoginId(),
			"Password!1"
		);
		SocialAccountService.GoogleLoginResult second =
			socialAccountService.loginGoogle(
				"google-subject-123",
				"googleuser@compick.test",
				"구글회원"
			);

		assertEquals(first.member().getId(), second.member().getId());
		assertTrue(first.credentialSetupRequired());
		assertFalse(second.credentialSetupRequired());
		assertTrue(passwordEncoder.matches(
			"Password!1",
			second.member().getPasswordHash()
		));
		assertEquals(1, socialAccountRepository.count());
	}

	@Test
	void socialMemberEntersPasswordToWithdraw() throws Exception {
		SocialAccountService.GoogleLoginResult result =
			socialAccountService.loginGoogle(
				"withdraw-google-subject",
				"withdraw@compick.test",
				"탈퇴회원"
			);
		String loginId = result.member().getLoginId();
		socialAccountService.setSocialPassword(loginId, "Password!1");

		mockMvc.perform(post("/mypage/withdraw")
				.with(user(loginId).roles("USER"))
				.with(csrf())
				.param("password", "Password!1"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));

		Member member = memberRepository.findByLoginId(loginId).orElseThrow();
		assertEquals(MemberStatus.WITHDRAWN, member.getStatus());
		assertEquals(0, socialAccountRepository.count());
	}

	@Test
	void mypageAlertsMemberWhosePhoneIsMissing() throws Exception {
		Member member = memberRepository.save(new Member(
			"phoneMissing",
			passwordEncoder.encode("Password!1"),
			"전화번호미등록",
			"phone-missing@compick.test",
			"미등록회원",
			"미등록"
		));

		mockMvc.perform(get("/")
				.with(user(member.getLoginId()).roles("USER")))
			.andExpect(status().isOk())
			.andExpect(content().string(not(containsString(
				"전화번호를 입력해주세요!"
			))));

		mockMvc.perform(get("/mypage")
				.with(user(member.getLoginId()).roles("USER")))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(
				"전화번호를 입력해주세요!"
			)));
	}

	private Member saveMember(String loginId, String email) {
		return memberRepository.save(new Member(
			loginId,
			passwordEncoder.encode("Password!1"),
			"컴픽회원",
			email,
			"컴픽이",
			"010-1234-5678"
		));
	}
}
