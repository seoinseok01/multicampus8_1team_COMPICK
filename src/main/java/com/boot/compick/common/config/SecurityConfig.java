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
                        .requestMatchers("/", "/privacy-policy", "/member/login", "/member/join", "/member/check-login-id", "/oauth2/**", "/login/oauth2/**",
                                "/member/find-id/**", "/member/password-reset/**", "/css/**").permitAll()
                        .anyRequest().authenticated())
                .userDetailsService(memberUserDetailsService)
                .formLogin(form -> form
                        .loginPage("/member/login")
                        .loginProcessingUrl("/member/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/member/login?error"))
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember-me")
                        .userDetailsService(memberUserDetailsService))
                .logout(logout -> logout
                        .logoutUrl("/member/logout")
                        .logoutSuccessUrl("/member/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me"));
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/member/login")
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(compickOidcUserService))
                    .successHandler((request, response, authentication) -> {
                        String target = authentication.getPrincipal() instanceof CompickOidcUser user
                                && user.isCredentialSetupRequired() ? "/member/social-credentials" : "/";
                        response.sendRedirect(request.getContextPath() + target);
                    })
                    .failureUrl("/member/login?oauthError"));
        }
        return http.build();
    }
}
