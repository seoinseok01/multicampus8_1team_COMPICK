package com.boot.compick.config;

<<<<<<< HEAD
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
=======
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
	private final ObjectProvider<ClientRegistrationRepository>
		clientRegistrations;

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
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad

		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/",
					"/products/**",
					"/quotes/**",
					"/recommendations/**",
					"/ai-quotes/**",
					"/members/signup",
<<<<<<< HEAD
=======
					"/api/members/check-login-id",
					"/api/members/check-email",
					"/login",
					"/oauth2/**",
					"/login/oauth2/**",
					"/css/**",
					"/js/**",
					"/images/**",
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
					"/error",
					"/error/**"
				).permitAll()
				.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
				.requestMatchers(
					"/cart/**",
					"/mypage/**",
<<<<<<< HEAD
=======
					"/members/social-password",
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
					"/orders/**",
					"/api/cart/**",
					"/api/orders/**",
					"/api/addresses/**"
				).authenticated()
				.anyRequest().permitAll()
			)
<<<<<<< HEAD
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
=======
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
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) -> {
					if (request.getRequestURI().startsWith("/api/")) {
						response.sendError(401);
						return;
					}
					loginEntryPoint.commence(request, response, authException);
				})
			);

<<<<<<< HEAD
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
=======
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

>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
}
