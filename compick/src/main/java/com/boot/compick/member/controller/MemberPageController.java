package com.boot.compick.member.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.boot.compick.member.dto.ProfileForm;
import com.boot.compick.member.dto.SignupForm;
import com.boot.compick.member.dto.SocialCredentialForm;
import com.boot.compick.member.security.CompickOidcUser;
import com.boot.compick.member.service.AddressService;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.member.service.SocialAccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
public class MemberPageController {

	private final MemberService memberService;
	private final AddressService addressService;
	private final SocialAccountService socialAccountService;
	private final AuthenticationManager authenticationManager;
	private final ObjectProvider<ClientRegistrationRepository>
		clientRegistrations;
	private final SecurityContextRepository securityContextRepository =
		new HttpSessionSecurityContextRepository();

	public MemberPageController(
		MemberService memberService,
		AddressService addressService,
		SocialAccountService socialAccountService,
		AuthenticationManager authenticationManager,
		ObjectProvider<ClientRegistrationRepository> clientRegistrations
	) {
		this.memberService = memberService;
		this.addressService = addressService;
		this.socialAccountService = socialAccountService;
		this.authenticationManager = authenticationManager;
		this.clientRegistrations = clientRegistrations;
	}

	@GetMapping("/members/signup")
	public String signupForm(Authentication authentication, Model model) {
		if (isLoggedIn(authentication)) {
			return "redirect:/";
		}
		if (!model.containsAttribute("signupForm")) {
			model.addAttribute("signupForm", new SignupForm());
		}
		return "member/signup";
	}

	@PostMapping("/members/signup")
	public String signup(
		@Valid @ModelAttribute SignupForm signupForm,
		BindingResult bindingResult,
		RedirectAttributes redirectAttributes
	) {
		if (!signupForm.getPassword().equals(signupForm.getPasswordConfirm())) {
			bindingResult.rejectValue(
				"passwordConfirm",
				"mismatch",
				"비밀번호가 일치하지 않습니다."
			);
		}
		if (bindingResult.hasErrors()) {
			return "member/signup";
		}

		try {
			memberService.signup(signupForm);
		} catch (IllegalArgumentException exception) {
			bindingResult.reject("signup", exception.getMessage());
			return "member/signup";
		}

		redirectAttributes.addFlashAttribute(
			"message",
			"회원가입이 완료되었습니다. 로그인해 주세요."
		);
		return "redirect:/login";
	}

	@GetMapping("/login")
	public String login(Authentication authentication, Model model) {
		model.addAttribute(
			"googleLoginEnabled",
			clientRegistrations.getIfAvailable() != null
		);
		return isLoggedIn(authentication) ? "redirect:/" : "member/login";
	}

	@GetMapping("/members/social-password")
	public String socialCredentials(
		Authentication authentication,
		Model model
	) {
		if (!(authentication.getPrincipal() instanceof CompickOidcUser user)
			|| !user.isCredentialSetupRequired()) {
			return "redirect:/";
		}
		if (!model.containsAttribute("socialCredentialForm")) {
			model.addAttribute(
				"socialCredentialForm",
				new SocialCredentialForm()
			);
		}
		return "member/social-credentials";
	}

	@PostMapping("/members/social-password")
	public String setSocialCredentials(
		Authentication authentication,
		@Valid @ModelAttribute SocialCredentialForm socialCredentialForm,
		BindingResult bindingResult,
		HttpServletRequest request,
		HttpServletResponse response,
		RedirectAttributes redirectAttributes
	) {
		if (!(authentication.getPrincipal() instanceof CompickOidcUser user)
			|| !user.isCredentialSetupRequired()) {
			return "redirect:/";
		}
		if (!socialCredentialForm.getPassword().equals(
			socialCredentialForm.getPasswordConfirm()
		)) {
			bindingResult.rejectValue(
				"passwordConfirm",
				"mismatch",
				"비밀번호가 일치하지 않습니다."
			);
		}
		if (bindingResult.hasErrors()) {
			return "member/social-credentials";
		}

		try {
			socialAccountService.setSocialPassword(
				authentication.getName(),
				socialCredentialForm.getPassword()
			);
		} catch (IllegalArgumentException exception) {
			bindingResult.reject(
				"credentials",
				exception.getMessage()
			);
			return "member/social-credentials";
		}

		signIn(
			authentication.getName(),
			socialCredentialForm.getPassword(),
			request,
			response
		);
		redirectAttributes.addFlashAttribute(
			"message",
			"Google 회원가입이 완료되었습니다."
		);
		return "redirect:/";
	}

	@GetMapping("/mypage")
	public String mypage(Authentication authentication, Model model) {
		String loginId = authentication.getName();
		model.addAttribute("member", memberService.getSummary(loginId));
		model.addAttribute("addresses", addressService.findAll(loginId));
		return "mypage/index";
	}

	@GetMapping("/mypage/profile")
	public String profile(Authentication authentication, Model model) {
		String loginId = authentication.getName();
		if (!model.containsAttribute("profileForm")) {
			model.addAttribute("profileForm", memberService.getProfile(loginId));
		}
		model.addAttribute("member", memberService.getSummary(loginId));
		return "mypage/profile";
	}

	@PostMapping("/mypage/profile")
	public String updateProfile(
		Authentication authentication,
		@Valid @ModelAttribute ProfileForm profileForm,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(
				"member",
				memberService.getSummary(authentication.getName())
			);
			return "mypage/profile";
		}

		try {
			memberService.updateProfile(authentication.getName(), profileForm);
		} catch (IllegalArgumentException exception) {
			bindingResult.reject("profile", exception.getMessage());
			model.addAttribute(
				"member",
				memberService.getSummary(authentication.getName())
			);
			return "mypage/profile";
		}

		redirectAttributes.addFlashAttribute(
			"message",
			"회원정보를 수정했습니다."
		);
		return "redirect:/mypage";
	}

	@PostMapping("/mypage/withdraw")
	public String withdraw(
		Authentication authentication,
		@RequestParam String password,
		HttpServletRequest request,
		RedirectAttributes redirectAttributes
	) {
		boolean googleConnectionRemoved;
		try {
			googleConnectionRemoved = memberService.withdraw(
				authentication.getName(),
				password
			);
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute(
				"error",
				exception.getMessage()
			);
			return "redirect:/mypage";
		}

		request.getSession().invalidate();
		SecurityContextHolder.clearContext();
		redirectAttributes.addFlashAttribute(
			"message",
			"회원 탈퇴가 완료되었습니다."
		);
		redirectAttributes.addFlashAttribute(
			"googleConnectionRemoved",
			googleConnectionRemoved
		);
		return "redirect:/login";
	}

	@GetMapping("/mypage/addresses")
	public String addresses(Authentication authentication, Model model) {
		model.addAttribute(
			"addresses",
			addressService.findAll(authentication.getName())
		);
		return "mypage/address-list";
	}

	private boolean isLoggedIn(Authentication authentication) {
		return authentication != null
			&& authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken);
	}

	private void signIn(
		String loginId,
		String password,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		Authentication authentication = authenticationManager.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated(
				loginId,
				password
			)
		);
		SecurityContext context =
			SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(
			context,
			request,
			response
		);
	}
}
