package com.boot.compick.home;

import com.boot.compick.member.entity.Member;
import com.boot.compick.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final MemberService memberService;

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Member member = memberService.findActiveByLoginId(authentication.getName());
            String phone = member.getPhone();
            boolean phoneMissing = phone == null || phone.isBlank() || "미등록".equals(phone);

            model.addAttribute("phoneMissing", phoneMissing);
        }

        return "index";
    }
}
