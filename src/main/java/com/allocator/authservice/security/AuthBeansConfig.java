package com.allocator.authservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Split out from SecurityConfig: AuthService depends on these two beans, and
// SecurityConfig depends (via OAuth2LoginSuccessHandler) on AuthService — so
// defining them on SecurityConfig itself creates a circular dependency
// (Spring must fully construct a @Configuration class before invoking its
// @Bean methods). Keeping them in an independent config class breaks the cycle.
@Configuration
public class AuthBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}