package com.bookstore.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    /**
     * Mật khẩu lưu DB dạng BCrypt; khi đăng nhập, Spring Security dùng {@link PasswordEncoder#matches}
     * (BCrypt) so sánh mật khẩu thô với hash — không bao giờ so sánh plaintext với DB.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
        http.authenticationProvider(authenticationProvider);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.authorizeHttpRequests(authz -> authz
                .requestMatchers("/admin/**", "/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/checkout", "/order", "/orders", "/payment/**", "/order/**", "/profile/**", "/orders/**", "/notifications/**", "/api/user/**", "/api/checkout/**").authenticated()
                .requestMatchers("/cart/**", "/api/**", "/forgot-password", "/reset-password", "/reset-password/**", "/login", "/register", "/").permitAll()
                .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                            request.getSession().setAttribute("user", userDetails.getUser());
                            
                            String jwt = tokenProvider.generateToken(authentication);
                            jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("jwt", jwt);
                            jwtCookie.setHttpOnly(true);
                            jwtCookie.setPath("/");
                            jwtCookie.setMaxAge(24 * 60 * 60);
                            response.addCookie(jwtCookie);

                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                            if (isAdmin) {
                                response.sendRedirect("/admin");
                                return;
                            }

                            String returnUrl = request.getParameter("returnUrl");
                            String target = (returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//"))
                                    ? returnUrl
                                    : null;

                            if (target == null) {
                                SavedRequest saved = new HttpSessionRequestCache().getRequest(request, response);
                                if (saved != null) {
                                    String ru = saved.getRedirectUrl();
                                    if (ru != null) {
                                        // saved redirectUrl is absolute; keep only same-origin path
                                        int idx = ru.indexOf("://");
                                        String pathOnly = ru;
                                        if (idx >= 0) {
                                            int slash = ru.indexOf('/', idx + 3);
                                            pathOnly = slash >= 0 ? ru.substring(slash) : "/";
                                        }
                                        if (pathOnly.startsWith("/") && !pathOnly.startsWith("//") && !pathOnly.startsWith("/login")) {
                                            target = pathOnly;
                                        }
                                    }
                                }
                            }

                            if (target == null) target = "/";
                            response.sendRedirect(target);
                        })
                        // Nếu login từ popup modal, quay lại đúng trang và mở lại modal để hiển thị lỗi.
                        .failureHandler((request, response, exception) -> {
                            String returnUrl = request.getParameter("returnUrl");
                            String target = (returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//"))
                                    ? returnUrl
                                    : "/login";

                            // tránh bơm loginError vô hạn khi đang ở trang /login
                            if (target.startsWith("/login")) {
                                response.sendRedirect("/login?error=true");
                                return;
                            }

                            String sep = target.contains("?") ? "&" : "?";
                            response.sendRedirect(target + sep + "loginError=1");
                        })
                        .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(oAuth2LoginSuccessHandler))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .deleteCookies("jwt")
                        .permitAll())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(e -> e.accessDeniedPage("/403"));

        return http.build();
    }
}
