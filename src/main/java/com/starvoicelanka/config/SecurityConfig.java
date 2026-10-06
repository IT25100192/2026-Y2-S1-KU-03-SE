package com.starvoicelanka.config;

import com.starvoicelanka.user.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName("next");

        http
                .csrf(AbstractHttpConfigurer::disable) // Disabled to support both REST endpoints and browser form submits
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authenticationProvider(authenticationProvider())
                .requestCache(cache -> cache.requestCache(requestCache))
                .authorizeHttpRequests(auth -> auth
                        // Static assets and media
                        .requestMatchers("/assets/**", "/media/**", "/favicon.ico").permitAll()
                        .requestMatchers("/health", "/api/health").permitAll()
                        // Public pages
                        .requestMatchers("/", "/contestants/**", "/results/**", "/bundles", "/partners", "/sponsor-click/**").permitAll()
                        .requestMatchers("/login", "/register", "/verify/**", "/forgot", "/reset", "/error").permitAll()
                        // Public REST endpoints
                        .requestMatchers("/api/users/login", "/api/users/register", "/api/users/verify-mobile",
                                "/api/users/resend-code", "/api/users/forgot-password", "/api/users/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contestants/**", "/api/rounds/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/payments/bundles").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sponsors/packages", "/api/sponsors/banners/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sponsors/agreements/*/click").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/votes/tally/**").permitAll()
                        // H2 Console
                        .requestMatchers("/h2-console/**").permitAll()
                        // Admin desk
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SPONSOR_MANAGER")
                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", false)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("svl_session", "JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
