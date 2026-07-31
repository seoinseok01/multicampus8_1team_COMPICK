package com.boot.compick.member.controller;

import com.boot.compick.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    @GetMapping("/check-login-id")
    public ResponseEntity<Boolean> checkLoginId(@RequestParam String loginId) {
        return ResponseEntity.ok(memberService.isLoginIdAvailable(loginId));
    }

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(memberService.isEmailAvailable(email));
    }
}
