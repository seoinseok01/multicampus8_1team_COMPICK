package com.boot.compick.member;

import com.boot.compick.member.dto.*;
import com.boot.compick.member.entity.Address;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.entity.EmailVerification;
import com.boot.compick.member.entity.VerificationPurpose;
import com.boot.compick.member.repository.EmailVerificationRepository;
import com.boot.compick.member.service.AddressService;
import com.boot.compick.member.service.EmailVerificationService;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.member.service.SocialAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberServiceIntegrationTests {
    @Autowired MemberService memberService;
    @Autowired AddressService addressService;
    @Autowired MockMvc mockMvc;
    @Autowired EmailVerificationRepository emailVerificationRepository;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SocialAccountService socialAccountService;

    @Test
    void joinProfilePasswordAndWithdrawalFlow() {
        JoinForm join = joinForm("compick01", "member1@compick.com");
        memberService.join(join);

        Member member = memberService.findActiveByLoginId("compick01");
        assertThat(member.getPasswordHash()).isNotEqualTo(join.getPassword());

        ProfileForm profile = memberService.getProfile("compick01");
        profile.setName("김컴픽"); profile.setPhone("010-2222-3333");
        memberService.updateProfile("compick01", profile);
        assertThat(memberService.findActiveByLoginId("compick01").getName()).isEqualTo("김컴픽");
        assertThat(memberService.findActiveByLoginId("compick01").getEmail()).isEqualTo("member1@compick.com");

        PasswordForm password = new PasswordForm();
        password.setCurrentPassword("Abc!1234"); password.setNewPassword("New!1234"); password.setNewPasswordConfirm("New!1234");
        memberService.changePassword("compick01", password);
        memberService.withdraw("compick01", "New!1234");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThatThrownBy(() -> memberService.findActiveByLoginId("compick01")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateLoginIdAndEmailAreRejected() {
        memberService.join(joinForm("duplicate", "same@compick.com"));
        assertThatThrownBy(() -> memberService.join(joinForm("duplicate", "other@compick.com")))
                .hasMessageContaining("아이디");
        assertThatThrownBy(() -> memberService.join(joinForm("other01", "same@compick.com")))
                .hasMessageContaining("이메일");
    }

    @Test
    void onlyOneDefaultAddressIsMaintained() {
        memberService.join(joinForm("address01", "address@compick.com"));
        AddressForm home = addressForm("집", true);
        addressService.save("address01", null, home);
        AddressForm office = addressForm("회사", true);
        addressService.save("address01", null, office);

        List<Address> addresses = addressService.findAll("address01");
        assertThat(addresses).hasSize(2);
        assertThat(addresses).filteredOn(Address::isDefault).extracting(Address::getAddressName).containsExactly("회사");
    }

    @Test
    void memberPagesRenderSuccessfully() throws Exception {
        mockMvc.perform(get("/member/login")).andExpect(status().isOk()).andExpect(view().name("member/login"));
        mockMvc.perform(get("/member/join")).andExpect(status().isOk()).andExpect(view().name("member/join"));
        mockMvc.perform(get("/member/find-id")).andExpect(status().isOk()).andExpect(view().name("member/find-id"));
        mockMvc.perform(get("/member/password-reset")).andExpect(status().isOk()).andExpect(view().name("member/password-reset"));
        mockMvc.perform(get("/privacy-policy")).andExpect(status().isOk()).andExpect(view().name("privacy-policy"));
        memberService.join(joinForm("screen01", "screen@compick.com"));
        mockMvc.perform(get("/member/mypage").with(user("screen01").roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("member/mypage"));
        mockMvc.perform(get("/member/addresses").with(user("screen01").roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("member/address-list"));
    }

    @Test
    void successfulJoinLogsTheNewMemberIn() throws Exception {
        mockMvc.perform(post("/member/join").with(csrf())
                        .param("loginId", "autologin01")
                        .param("password", "Abc!1234")
                        .param("passwordConfirm", "Abc!1234")
                        .param("email", "autologin@compick.com")
                        .param("name", "자동로그인")
                        .param("phone", "010-1234-5678")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/mypage"))
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT",
                        org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    void verificationCodeIsSingleUseAndPurposeSpecific() {
        verifiedEmail("verify@compick.com", VerificationPurpose.FIND_ID, "654321");
        emailVerificationService.consumeVerified("verify@compick.com", VerificationPurpose.FIND_ID);
        assertThatThrownBy(() -> emailVerificationService.consumeVerified("verify@compick.com", VerificationPurpose.FIND_ID))
                .hasMessageContaining("사용");
        assertThatThrownBy(() -> emailVerificationService.confirm("verify@compick.com", VerificationPurpose.PASSWORD_RESET, "654321"))
                .hasMessageContaining("요청");
    }

    @Test
    void googleLoginCreatesAndReusesTheSameMember() {
        Member first = socialAccountService.loginGoogle("google-sub-123", "GoogleUser@Example.com", "구글회원");
        Member second = socialAccountService.loginGoogle("google-sub-123", "googleuser@example.com", "구글회원");
        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(first.getLoginId()).startsWith("google_");
        assertThat(first.getEmail()).isEqualTo("googleuser@example.com");
    }

    @Test
    void googleLoginLinksAnExistingMemberWithTheSameEmail() {
        memberService.join(joinForm("localgoogle", "linked@example.com"));
        Member local = memberService.findActiveByLoginId("localgoogle");
        Member linked = socialAccountService.loginGoogle("google-sub-linked", "linked@example.com", "연결회원");
        assertThat(linked.getId()).isEqualTo(local.getId());
    }

    private JoinForm joinForm(String loginId, String email) {
        JoinForm form = new JoinForm(); form.setLoginId(loginId); form.setEmail(email);
        form.setPassword("Abc!1234"); form.setPasswordConfirm("Abc!1234");
        form.setName("테스트회원"); form.setPhone("010-1111-2222"); form.setTermsAccepted(true);
        return form;
    }

    private AddressForm addressForm(String name, boolean isDefault) {
        AddressForm form = new AddressForm(); form.setAddressName(name); form.setRecipientName("테스트회원");
        form.setRecipientPhone("010-1111-2222"); form.setZipCode("06236");
        form.setBasicAddress("서울특별시 강남구 테헤란로"); form.setDetailAddress("123"); form.setDefaultAddress(isDefault);
        return form;
    }

    private void verifiedEmail(String email, VerificationPurpose purpose, String code) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        emailVerificationRepository.save(new EmailVerification(email, purpose, passwordEncoder.encode(code), now.plusMinutes(5), now));
        emailVerificationService.confirm(email, purpose, code);
    }
}
