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
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/privacy-policy", "/login", "/members/signup",
                                "/api/members/check-login-id", "/api/members/check-email",
                                "/oauth2/**", "/login/oauth2/**", "/members/find-id/**",
                                "/members/password-reset/**", "/css/**").permitAll()
                        .anyRequest().authenticated())
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
                        .deleteCookies("JSESSIONID", "remember-me"));
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(compickOidcUserService))
                    .successHandler((request, response, authentication) -> {
                        String target = authentication.getPrincipal() instanceof CompickOidcUser user
                                && user.isCredentialSetupRequired() ? "/members/social-password" : "/";
                        response.sendRedirect(request.getContextPath() + target);
                    })
                    .failureUrl("/login?oauthError"));
        }
        return http.build();
    }
}
