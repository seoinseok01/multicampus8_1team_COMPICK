package com.boot.compick.member.controller;

import com.boot.compick.member.dto.*;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.VerificationPurpose;
import com.boot.compick.member.service.AddressService;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.member.service.SocialAccountService;
import com.boot.compick.member.service.EmailVerificationService;
import com.boot.compick.member.security.CompickOidcUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final AddressService addressService;
    private final SocialAccountService socialAccountService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationManager authenticationManager;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("googleLoginEnabled", clientRegistrations.getIfAvailable() != null);
        return "member/login";
    }

    @GetMapping("/members/signup")
    public String joinForm(Model model) {
        model.addAttribute("joinForm", new JoinForm());
        return "member/join";
    }

    @PostMapping("/members/signup")
    public String join(@Valid @ModelAttribute JoinForm joinForm, BindingResult bindingResult,
                       HttpServletRequest request, HttpServletResponse response,
                       RedirectAttributes redirectAttributes) {
        if (!joinForm.getPassword().equals(joinForm.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            return "member/join";
        }
        try {
            emailVerificationService.requireVerified(joinForm.getEmail(), VerificationPurpose.SIGN_UP);
            memberService.join(joinForm);
            emailVerificationService.consumeVerified(joinForm.getEmail(), VerificationPurpose.SIGN_UP);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("join", e.getMessage());
            return "member/join";
        }

        signIn(joinForm.getLoginId(), joinForm.getPassword(), request, response);
        redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다.");
        return "redirect:/";
    }

    @GetMapping("/members/social-password")
    public String socialCredentials(Authentication authentication, Model model) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CompickOidcUser user)
                || !user.isCredentialSetupRequired()) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("socialCredentialForm")) {
            model.addAttribute("socialCredentialForm", new SocialCredentialForm());
        }
        return "member/social-credentials";
    }

    @PostMapping("/members/social-password")
    public String setSocialCredentials(Authentication authentication,
                                       @Valid @ModelAttribute SocialCredentialForm socialCredentialForm,
                                       BindingResult bindingResult,
                                       HttpServletRequest request, HttpServletResponse response,
                                       RedirectAttributes redirectAttributes) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CompickOidcUser user)
                || !user.isCredentialSetupRequired()) {
            return "redirect:/login";
        }
        if (!socialCredentialForm.getPassword().equals(socialCredentialForm.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            return "member/social-credentials";
        }
        try {
            socialAccountService.completeSocialSignup(
                    authentication.getName(),
                    socialCredentialForm.getPassword());
        } catch (IllegalArgumentException e) {
            bindingResult.reject("credentials", e.getMessage());
            return "member/social-credentials";
        }

        signIn(authentication.getName(), socialCredentialForm.getPassword(), request, response);
        redirectAttributes.addFlashAttribute("message", "구글 회원가입이 완료되었습니다.");
        return "redirect:/";
    }

    @GetMapping("/mypage")
    public String mypage(Authentication authentication, Model model) {
        Member member = memberService.findActiveByLoginId(authentication.getName());
        String phone = member.getPhone();

        model.addAttribute("member", member);
        model.addAttribute(
                "phoneMissing",
                phone == null || phone.isBlank() || "미등록".equals(phone));
        model.addAttribute("addresses", addressService.findAll(authentication.getName()));
        return "member/mypage";
    }

    @GetMapping("/mypage/profile")
    public String profile(Authentication authentication, Model model) {
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", memberService.getProfile(authentication.getName()));
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordForm());
        }
        return "member/profile";
    }

    @PostMapping("/mypage/profile")
    public String updateProfile(Authentication authentication, @Valid @ModelAttribute ProfileForm profileForm,
                                BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordForm", new PasswordForm());
            return "member/profile";
        }
        try {
            memberService.updateProfile(authentication.getName(), profileForm);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("profile", e.getMessage());
            model.addAttribute("passwordForm", new PasswordForm());
            return "member/profile";
        }
        redirectAttributes.addFlashAttribute("message", "회원정보를 수정했습니다.");
        return "redirect:/mypage/profile";
    }

    @PostMapping("/mypage/password")
    public String changePassword(Authentication authentication, @Valid @ModelAttribute PasswordForm passwordForm,
                                 BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profileForm", memberService.getProfile(authentication.getName()));
            return "member/profile";
        }
        try {
            memberService.changePassword(authentication.getName(), passwordForm);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("password", e.getMessage());
            model.addAttribute("profileForm", memberService.getProfile(authentication.getName()));
            return "member/profile";
        }
        redirectAttributes.addFlashAttribute("message", "비밀번호를 변경했습니다.");
        return "redirect:/mypage/profile";
    }

    @PostMapping("/mypage/withdraw")
    public String withdraw(Authentication authentication, @RequestParam String password,
                           HttpServletRequest request, HttpServletResponse response,
                           RedirectAttributes redirectAttributes) {
        boolean googleConnectionRemoved;
        try {
            googleConnectionRemoved = memberService.withdraw(authentication.getName(), password);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage";
        }
        request.getSession().invalidate();
        redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다.");
        redirectAttributes.addFlashAttribute("googleConnectionRemoved", googleConnectionRemoved);
        return "redirect:/login";
    }

    private void signIn(String loginId, String password,
                        HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(loginId, password));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
