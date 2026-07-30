package com.boot.compick.member.controller;

import com.boot.compick.member.entity.VerificationPurpose;
import com.boot.compick.member.service.EmailVerificationService;
import com.boot.compick.member.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RecoveryController {
    private final MemberService memberService;
    private final EmailVerificationService verificationService;

    @GetMapping("/members/find-id")
    public String findId() {
        return "member/find-id";
    }

    @PostMapping("/members/find-id/send")
    public String sendFindId(@RequestParam String name, @RequestParam String email,
                             HttpSession session, RedirectAttributes redirect) {
        try {
            memberService.findMaskedLoginId(name, email);
            verificationService.send(email, VerificationPurpose.FIND_ID);
            session.setAttribute("findIdName", name);
            session.setAttribute("findIdEmail", verificationService.normalize(email));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            session.removeAttribute("findIdName");
            session.removeAttribute("findIdEmail");
        }
        redirect.addFlashAttribute("message", "입력한 정보와 일치하는 계정이 있다면 인증 메일을 전송했습니다.");
        return "redirect:/members/find-id";
    }

    @PostMapping("/members/find-id/confirm")
    public String confirmFindId(@RequestParam String code, HttpSession session, Model model) {
        String name = (String) session.getAttribute("findIdName");
        String email = (String) session.getAttribute("findIdEmail");
        if (name == null || email == null) {
            model.addAttribute("error", "먼저 인증번호를 요청해 주세요.");
            return "member/find-id";
        }
        try {
            verificationService.confirmAndConsume(email, VerificationPurpose.FIND_ID, code);
            model.addAttribute("maskedLoginId", memberService.findMaskedLoginId(name, email));
            session.removeAttribute("findIdName");
            session.removeAttribute("findIdEmail");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "member/find-id";
    }

    @GetMapping("/members/password-reset")
    public String passwordReset() {
        return "member/password-reset";
    }

    @PostMapping("/members/password-reset/send")
    public String sendPasswordReset(@RequestParam String loginId, @RequestParam String email,
                                    HttpSession session, RedirectAttributes redirect) {
        try {
            memberService.validatePasswordResetMember(loginId, email);
            verificationService.send(email, VerificationPurpose.PASSWORD_RESET);
            session.setAttribute("resetLoginId", loginId);
            session.setAttribute("resetEmail", verificationService.normalize(email));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            session.removeAttribute("resetLoginId");
            session.removeAttribute("resetEmail");
        }
        redirect.addFlashAttribute("message", "입력한 정보와 일치하는 계정이 있다면 인증 메일을 전송했습니다.");
        return "redirect:/members/password-reset";
    }

    @PostMapping("/members/password-reset/confirm")
    public String confirmPasswordReset(@RequestParam String code, @RequestParam String newPassword,
                                       @RequestParam String newPasswordConfirm, HttpSession session,
                                       Model model, RedirectAttributes redirect) {
        String loginId = (String) session.getAttribute("resetLoginId");
        String email = (String) session.getAttribute("resetEmail");
        if (loginId == null || email == null) {
            model.addAttribute("error", "먼저 인증번호를 요청해 주세요.");
            return "member/password-reset";
        }
        try {
            if (!newPassword.equals(newPasswordConfirm)) {
                throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
            }
            if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$")) {
                throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다.");
            }
            verificationService.confirmAndConsume(email, VerificationPurpose.PASSWORD_RESET, code);
            memberService.resetPassword(loginId, email, newPassword, newPasswordConfirm);
            session.removeAttribute("resetLoginId");
            session.removeAttribute("resetEmail");
            redirect.addFlashAttribute("message", "비밀번호를 변경했습니다. 새 비밀번호로 로그인해 주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "member/password-reset";
        }
    }
}
