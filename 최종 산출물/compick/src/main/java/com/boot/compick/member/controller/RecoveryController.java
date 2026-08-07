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
    private static final String SENT_MESSAGE = "인증번호를 발송하였습니다. 메일을 받지 못하였다면 입력정보를 다시 확인하세요.";
    private final MemberService memberService;
    private final EmailVerificationService verificationService;

    @GetMapping("/members/find-id")
    public String findId() { return "member/find-id"; }

    @PostMapping("/members/find-id/send")
    public String sendFindId(@RequestParam String phone, @RequestParam String email,
                             HttpSession session, RedirectAttributes redirect) {
        clearFindId(session);
        try {
            memberService.findLoginId(phone, email);
            verificationService.send(email, VerificationPurpose.FIND_ID);
            session.setAttribute("findIdPhone", phone);
            session.setAttribute("findIdEmail", verificationService.normalize(email));
        } catch (RuntimeException ignored) { }
        redirect.addFlashAttribute("message", SENT_MESSAGE);
        return "redirect:/members/find-id/verify";
    }

    @GetMapping("/members/find-id/verify")
    public String findIdVerify(HttpSession session) {
        return session.getAttribute("findIdEmail") == null ? "redirect:/members/find-id" : "member/find-id-verify";
    }

    @PostMapping("/members/find-id/verify")
    public String verifyFindId(@RequestParam String code, HttpSession session,
                               Model model, RedirectAttributes redirect) {
        String phone = (String) session.getAttribute("findIdPhone");
        String email = (String) session.getAttribute("findIdEmail");
        if (phone == null || email == null) return "redirect:/members/find-id";
        try {
            verificationService.confirm(email, VerificationPurpose.FIND_ID, code);
            verificationService.consumeVerified(email, VerificationPurpose.FIND_ID);
            redirect.addFlashAttribute("loginId", memberService.findLoginId(phone, email));
            clearFindId(session);
            return "redirect:/members/find-id/result";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return "member/find-id-verify";
        }
    }

    @GetMapping("/members/find-id/result")
    public String findIdResult(Model model) {
        return model.containsAttribute("loginId") ? "member/find-id-result" : "redirect:/members/find-id";
    }

    @GetMapping("/members/password-reset")
    public String passwordReset() { return "member/password-reset"; }

    @PostMapping("/members/password-reset/send")
    public String sendPasswordReset(@RequestParam String loginId, @RequestParam String email,
                                    HttpSession session, RedirectAttributes redirect) {
        clearPasswordReset(session);
        try {
            memberService.validatePasswordResetMember(loginId, email);
            verificationService.send(email, VerificationPurpose.PASSWORD_RESET);
            session.setAttribute("resetLoginId", loginId);
            session.setAttribute("resetEmail", verificationService.normalize(email));
        } catch (RuntimeException ignored) { }
        redirect.addFlashAttribute("message", SENT_MESSAGE);
        return "redirect:/members/password-reset/verify";
    }

    @GetMapping("/members/password-reset/verify")
    public String passwordResetVerify(HttpSession session) {
        return session.getAttribute("resetEmail") == null ? "redirect:/members/password-reset" : "member/password-reset-verify";
    }

    @PostMapping("/members/password-reset/verify")
    public String verifyPasswordReset(@RequestParam String code, HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/members/password-reset";
        try {
            verificationService.confirm(email, VerificationPurpose.PASSWORD_RESET, code);
            session.setAttribute("resetVerified", true);
            return "redirect:/members/password-reset/change";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return "member/password-reset-verify";
        }
    }

    @GetMapping("/members/password-reset/change")
    public String passwordResetChange(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("resetVerified"))
                ? "member/password-reset-change" : "redirect:/members/password-reset";
    }

    @PostMapping("/members/password-reset/change")
    public String changePassword(@RequestParam String newPassword, @RequestParam String newPasswordConfirm,
                                 HttpSession session, Model model, RedirectAttributes redirect) {
        String loginId = (String) session.getAttribute("resetLoginId");
        String email = (String) session.getAttribute("resetEmail");
        if (loginId == null || email == null || !Boolean.TRUE.equals(session.getAttribute("resetVerified"))) {
            return "redirect:/members/password-reset";
        }
        try {
            verificationService.requireVerified(email, VerificationPurpose.PASSWORD_RESET);
            memberService.resetPassword(loginId, email, newPassword, newPasswordConfirm);
            verificationService.consumeVerified(email, VerificationPurpose.PASSWORD_RESET);
            clearPasswordReset(session);
            redirect.addFlashAttribute("message", "비밀번호를 변경했습니다.");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return "member/password-reset-change";
        }
    }

    private void clearFindId(HttpSession session) {
        session.removeAttribute("findIdPhone"); session.removeAttribute("findIdEmail");
    }
    private void clearPasswordReset(HttpSession session) {
        session.removeAttribute("resetLoginId"); session.removeAttribute("resetEmail"); session.removeAttribute("resetVerified");
    }
}

