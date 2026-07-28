package com.boot.compick.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
			.logout(logout -> logout.logoutSuccessUrl("/"));

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
