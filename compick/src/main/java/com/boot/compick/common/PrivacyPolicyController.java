package com.boot.compick.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PrivacyPolicyController {
    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "privacy-policy";
    }
}
