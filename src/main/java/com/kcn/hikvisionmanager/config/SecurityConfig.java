package com.kcn.hikvisionmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private final String key;

    @Value("${app.security.https-only:false}")
    private boolean httpsOnly;

    public SecurityConfig(@Value("${remember.me.key:}") String userKey) {
        if(userKey != null && !userKey.isBlank()) {
            this.key = userKey;
            log.info("✅ SecurityConfig initialized with UserKey");
        } else {
            this.key = UUID.randomUUID().toString() + UUID.randomUUID().toString();
            log.info("✅ SecurityConfig initialized with Generated Key");
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .defaultSuccessUrl("/", true)
                )

                .rememberMe(remember -> remember
                        .key(key)
                        .tokenValiditySeconds(30 * 24 * 60 * 60)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("hikvision-remember-me")
                        .useSecureCookie(httpsOnly)
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            log.info("🚪 User logged out successfully");
                            response.sendRedirect("/login?logout");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "hikvision-remember-me")
                        .clearAuthentication(true)
                        .permitAll()
                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/streams/**")
                )

                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                );


        if (httpsOnly) {
            log.info("🔒 HTTPS-only mode ENABLED - all requests redirected to HTTPS");
            http.redirectToHttps(withDefaults());
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}