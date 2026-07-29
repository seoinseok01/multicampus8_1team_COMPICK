package com.boot.compick.member.controller;

import com.boot.compick.member.dto.*;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.service.AddressService;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.member.service.SocialAccountService;
import com.boot.compick.member.security.CompickOidcUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final AddressService addressService;
    private final SocialAccountService socialAccountService;
    private final AuthenticationManager authenticationManager;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("googleLoginEnabled", clientRegistrations.getIfAvailable() != null);
        return "member/login";
    }

    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("joinForm", new JoinForm());
        return "member/join";
    }

    @PostMapping("/join")
    public String join(@Valid @ModelAttribute JoinForm joinForm, BindingResult bindingResult,
                       HttpServletRequest request, HttpServletResponse response,
                       RedirectAttributes redirectAttributes) {
        if (!joinForm.getPassword().equals(joinForm.getPasswordConfirm()))
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        if (bindingResult.hasErrors()) return "member/join";
        try { memberService.join(joinForm); }
        catch (IllegalArgumentException e) { bindingResult.reject("join", e.getMessage()); return "member/join"; }
        signIn(joinForm.getLoginId(), joinForm.getPassword(), request, response);
        redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다.");
        return "redirect:/member/mypage";
    }

    @GetMapping("/check-login-id")
    @ResponseBody
    public ResponseEntity<Boolean> checkLoginId(@RequestParam String loginId) {
        return ResponseEntity.ok(memberService.isLoginIdAvailable(loginId));
    }

    @GetMapping("/social-credentials")
    public String socialCredentials(Authentication authentication, Model model) {
        if (!(authentication.getPrincipal() instanceof CompickOidcUser user)
                || !user.isCredentialSetupRequired()) return "redirect:/";
        if (!model.containsAttribute("socialCredentialForm"))
            model.addAttribute("socialCredentialForm", new SocialCredentialForm());
        return "member/social-credentials";
    }

    @PostMapping("/social-credentials")
    public String setSocialCredentials(Authentication authentication,
                                       @Valid @ModelAttribute SocialCredentialForm socialCredentialForm,
                                       BindingResult bindingResult,
                                       HttpServletRequest request, HttpServletResponse response,
                                       RedirectAttributes redirectAttributes) {
        if (!(authentication.getPrincipal() instanceof CompickOidcUser user)
                || !user.isCredentialSetupRequired()) return "redirect:/";
        if (!socialCredentialForm.getPassword().equals(socialCredentialForm.getPasswordConfirm()))
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        if (bindingResult.hasErrors()) return "member/social-credentials";
        try {
            socialAccountService.setLoginCredentials(authentication.getName(),
                    socialCredentialForm.getLoginId(), socialCredentialForm.getPassword());
        } catch (IllegalArgumentException e) {
            bindingResult.reject("credentials", e.getMessage());
            return "member/social-credentials";
        }

        signIn(socialCredentialForm.getLoginId(), socialCredentialForm.getPassword(), request, response);
        redirectAttributes.addFlashAttribute("message", "구글 회원가입이 완료되었습니다.");
        return "redirect:/";
    }

    @GetMapping("/mypage")
    public String mypage(Authentication authentication, Model model) {
        Member member = memberService.findActiveByLoginId(authentication.getName());
        model.addAttribute("member", member);
        model.addAttribute("addresses", addressService.findAll(authentication.getName()));
        return "member/mypage";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        if (!model.containsAttribute("profileForm")) model.addAttribute("profileForm", memberService.getProfile(authentication.getName()));
        if (!model.containsAttribute("passwordForm")) model.addAttribute("passwordForm", new PasswordForm());
        return "member/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication, @Valid @ModelAttribute ProfileForm profileForm,
                                BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) { model.addAttribute("passwordForm", new PasswordForm()); return "member/profile"; }
        try { memberService.updateProfile(authentication.getName(), profileForm); }
        catch (IllegalArgumentException e) { bindingResult.reject("profile", e.getMessage()); model.addAttribute("passwordForm", new PasswordForm()); return "member/profile"; }
        redirectAttributes.addFlashAttribute("message", "회원정보를 수정했습니다.");
        return "redirect:/member/profile";
    }

    @PostMapping("/password")
    public String changePassword(Authentication authentication, @Valid @ModelAttribute PasswordForm passwordForm,
                                 BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) { model.addAttribute("profileForm", memberService.getProfile(authentication.getName())); return "member/profile"; }
        try { memberService.changePassword(authentication.getName(), passwordForm); }
        catch (IllegalArgumentException e) { bindingResult.reject("password", e.getMessage()); model.addAttribute("profileForm", memberService.getProfile(authentication.getName())); return "member/profile"; }
        redirectAttributes.addFlashAttribute("message", "비밀번호를 변경했습니다.");
        return "redirect:/member/profile";
    }

    @PostMapping("/withdraw")
    public String withdraw(Authentication authentication, @RequestParam String password,
                           HttpServletRequest request, HttpServletResponse response,
                           RedirectAttributes redirectAttributes) {
        try { memberService.withdraw(authentication.getName(), password); }
        catch (IllegalArgumentException e) { redirectAttributes.addFlashAttribute("error", e.getMessage()); return "redirect:/member/mypage"; }
        request.getSession().invalidate();
        redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다.");
        return "redirect:/member/login";
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
