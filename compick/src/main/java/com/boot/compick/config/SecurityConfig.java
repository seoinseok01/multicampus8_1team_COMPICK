package com.boot.compick.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import com.boot.compick.member.security.CompickOidcUser;
import com.boot.compick.member.service.CompickOidcUserService;
import com.boot.compick.member.service.MemberUserDetailsService;

@Configuration
public class SecurityConfig {

	private final MemberUserDetailsService memberUserDetailsService;
	private final CompickOidcUserService compickOidcUserService;
	private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

	public SecurityConfig(
		MemberUserDetailsService memberUserDetailsService,
		CompickOidcUserService compickOidcUserService,
		ObjectProvider<ClientRegistrationRepository> clientRegistrations
	) {
		this.memberUserDetailsService = memberUserDetailsService;
		this.compickOidcUserService = compickOidcUserService;
		this.clientRegistrations = clientRegistrations;
	}

	@Bean
	AuthenticationManager authenticationManager(
		AuthenticationConfiguration configuration
	) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		LoginUrlAuthenticationEntryPoint loginEntryPoint =
			new LoginUrlAuthenticationEntryPoint("/login");

		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/",
					"/products/**",
					"/quotes/**",
					"/recommendations/**",
					"/ai-quotes/**",
					"/members/signup",
					"/api/members/check-login-id",
					"/api/members/check-email",
					"/login",
					"/oauth2/**",
					"/login/oauth2/**",
					"/css/**",
					"/js/**",
					"/images/**",
					"/error",
					"/error/**"
				).permitAll()
				.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
				.requestMatchers(
					"/cart/**",
					"/mypage/**",
					"/members/social-password",
					"/orders/**",
					"/api/cart/**",
					"/api/orders/**",
					"/api/addresses/**"
				).authenticated()
				.anyRequest().permitAll()
			)
			.userDetailsService(memberUserDetailsService)
			.formLogin(form -> form
				.loginPage("/login")
				.loginProcessingUrl("/login")
				.usernameParameter("username")
				.passwordParameter("password")
				.defaultSuccessUrl("/", true)
				.failureUrl("/login?error")
				.permitAll()
			)
			.rememberMe(remember -> remember
				.rememberMeParameter("remember-me")
				.userDetailsService(memberUserDetailsService)
			)
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID", "remember-me")
			)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) -> {
					if (request.getRequestURI().startsWith("/api/")) {
						response.sendError(401);
						return;
					}
					loginEntryPoint.commence(request, response, authException);
				})
			);

		if (clientRegistrations.getIfAvailable() != null) {
			http.oauth2Login(oauth -> oauth
				.loginPage("/login")
				.userInfoEndpoint(userInfo ->
					userInfo.oidcUserService(compickOidcUserService)
				)
				.successHandler((request, response, authentication) -> {
					String target =
						authentication.getPrincipal() instanceof CompickOidcUser user
						&& user.isCredentialSetupRequired()
							? "/members/social-password"
							: "/";
					response.sendRedirect(request.getContextPath() + target);
				})
				.failureUrl("/login?oauthError")
			);
		}

		return http.build();
	}
}
