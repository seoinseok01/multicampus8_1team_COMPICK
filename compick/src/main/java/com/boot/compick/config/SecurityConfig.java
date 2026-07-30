package com.boot.compick.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		LoginUrlAuthenticationEntryPoint loginEntryPoint =
			new LoginUrlAuthenticationEntryPoint("/members/login");

		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/",
					"/products/**",
					"/quotes/**",
					"/recommendations/**",
					"/ai-quotes/**",
					"/members/signup",
					"/error",
					"/error/**"
				).permitAll()
				.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
				.requestMatchers(
					"/cart/**",
					"/mypage/**",
					"/orders/**",
					"/api/cart/**",
					"/api/orders/**",
					"/api/addresses/**"
				).authenticated()
				.anyRequest().permitAll()
			)
			.formLogin(form -> form
				.loginPage("/members/login")
				.loginProcessingUrl("/members/login")
				.usernameParameter("loginId")
				.passwordParameter("password")
				.defaultSuccessUrl("/", true)
				.failureUrl("/members/login?error")
				.permitAll()
			)
			.logout(logout -> logout.logoutSuccessUrl("/"))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) -> {
					if (request.getRequestURI().startsWith("/api/")) {
						response.sendError(401);
						return;
					}
					loginEntryPoint.commence(request, response, authException);
				})
			);

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
