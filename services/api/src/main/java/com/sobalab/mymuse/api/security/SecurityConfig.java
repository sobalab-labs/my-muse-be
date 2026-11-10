package com.sobalab.mymuse.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppOidcUserService oidcUserService;
    private final AppOAuth2UserService oauth2UserService;

    @Bean
    SecurityFilterChain web(HttpSecurity http) throws Exception {
        var successHandler = new SimpleUrlAuthenticationSuccessHandler("/");
        successHandler.setAlwaysUseDefaultTargetUrl(false);

        var failureHandler = new SimpleUrlAuthenticationFailureHandler("/login?error=oauth");

        return http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health", "/oauth2/**", "/login/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers(HttpMethod.GET, "/search/**", "/feed/**", "/artists/**", "/concerts/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(o -> o
                        .userInfoEndpoint(u -> u
                                .oidcUserService(oidcUserService)
                                .userService(oauth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler))
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/users/**")))
                .csrf(csrf -> csrf.spa())
                .logout(l -> l.logoutUrl("/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
                .build();
    }
}
