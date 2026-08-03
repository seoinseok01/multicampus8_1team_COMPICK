package com.boot.compick.common.config;

import com.boot.compick.member.service.MemberUserDetailsService;
import com.boot.compick.member.service.CompickOidcUserService;
import com.boot.compick.member.security.CompickOidcUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberUserDetailsService memberUserDetailsService;
    private final CompickOidcUserService compickOidcUserService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        LoginUrlAuthenticationEntryPoint loginEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/login");
        SecurityContextRepository securityContextRepository =
                new HttpSessionSecurityContextRepository();

        http
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/products/**", "/quotes/**", "/recommendations/**",
                                "/ai-quotes/**", "/privacy-policy", "/login", "/members/signup",
                                "/api/members/check-login-id", "/api/members/check-email",
                                "/api/email-verifications/**", "/members/find-id/**", "/members/password-reset/**",
                                "/oauth2/**", "/login/oauth2/**", "/css/**", "/js/**", "/images/**",
                                "/error", "/error/**").permitAll()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cart/**", "/mypage/**", "/orders/**", "/payments/**",
                                "/members/social-password",
                                "/api/cart/**", "/api/orders/**", "/api/addresses/**")
                        .authenticated()
                        .anyRequest().permitAll())
                .userDetailsService(memberUserDetailsService)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error"))
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember-me")
                        .userDetailsService(memberUserDetailsService))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me"))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.sendError(401);
                                return;
                            }
                            loginEntryPoint.commence(request, response, authException);
                        }));
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(compickOidcUserService))
                    .successHandler((request, response, authentication) -> {
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                        securityContextRepository.saveContext(context, request, response);

                        String target = authentication.getPrincipal() instanceof CompickOidcUser user
                                && user.isCredentialSetupRequired() ? "/members/social-password" : "/";
                        response.sendRedirect(request.getContextPath() + target);
                    })
                    .failureUrl("/login?oauthError"));
        }
        return http.build();
    }
}
