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
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;


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
                // VNPAY gọi IPN server-to-server (không cookie đăng nhập); Return do trình duyệt redirect — kiểm tra bằng chữ ký.
                .requestMatchers("/payment/vnpay/ipn", "/payment/vnpay/return").permitAll()
                .requestMatchers("/payment/onepay/return").permitAll()
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
                        // Sai mật khẩu: quay lại trang đang mở (returnUrl) kèm ?error=true để popup đăng nhập hiện cảnh báo.
                        .failureHandler((request, response, exception) -> {
                            String returnUrl = request.getParameter("returnUrl");
                            String loc;
                            if (returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//")) {
                                int q = returnUrl.indexOf('?');
                                String pathOnly = q >= 0 ? returnUrl.substring(0, q) : returnUrl;
                                if (!"/login".equals(pathOnly)) {
                                    String sep = q >= 0 ? "&" : "?";
                                    loc = returnUrl + sep + "error=true";
                                } else {
                                    loc = q >= 0 ? "/login?error=true&" + returnUrl.substring(q + 1) : "/login?error=true";
                                }
                            } else {
                                loc = "/login?error=true";
                            }
                            response.sendRedirect(loc);
                        })
                        .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(oAuth2LoginSuccessHandler))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // Query để client xóa localStorage giỏ hàng (tránh đăng xuất rồi đăng nhập lại vẫn thấy giỏ cũ)
                        .logoutSuccessUrl("/?btLogout=1")
                        .deleteCookies("jwt")
                        .permitAll())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(e -> e.accessDeniedPage("/403"));

        return http.build();
    }
}
