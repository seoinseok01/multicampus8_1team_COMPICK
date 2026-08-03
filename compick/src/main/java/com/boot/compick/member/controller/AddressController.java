package com.boot.compick.member.controller;

import com.boot.compick.member.dto.AddressForm;
import com.boot.compick.member.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @GetMapping("/mypage/addresses")
    public String list(Authentication authentication, Model model) {
        model.addAttribute("addresses", addressService.findAll(authentication.getName()));
        return "member/address-list";
    }

    @GetMapping("/mypage/addresses/new")
    public String createForm(Model model) {
        model.addAttribute("addressForm", new AddressForm());
        model.addAttribute("addressId", null);
        return "member/address-form";
    }

    @GetMapping("/mypage/addresses/{addressId}/edit")
    public String editForm(Authentication authentication, @PathVariable Long addressId, Model model) {
        model.addAttribute("addressForm", addressService.getForm(authentication.getName(), addressId));
        model.addAttribute("addressId", addressId);
        return "member/address-form";
    }

    @PostMapping({"/mypage/addresses", "/mypage/addresses/{addressId}"})
    public String save(Authentication authentication, @PathVariable(required = false) Long addressId,
                       @Valid @ModelAttribute AddressForm addressForm, BindingResult bindingResult, Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("addressId", addressId);
            return "member/address-form";
        }
        addressService.save(authentication.getName(), addressId, addressForm);
        redirectAttributes.addFlashAttribute("message", "배송지를 저장했습니다.");
        return "redirect:/mypage/addresses";
    }

    @PostMapping("/mypage/addresses/{addressId}/delete")
    public String delete(Authentication authentication, @PathVariable Long addressId, RedirectAttributes redirectAttributes) {
        addressService.delete(authentication.getName(), addressId);
        redirectAttributes.addFlashAttribute("message", "배송지를 삭제했습니다.");
        return "redirect:/mypage/addresses";
    }

}
