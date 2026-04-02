package com.bookstore.security;

import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oAuth2User)) {
            response.sendRedirect("/");
            return;
        }

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendRedirect("/login?error=true");
            return;
        }

        User user = upsertGoogleUser(email.trim(), name);

        // set session user (for existing templates using session.user)
        request.getSession().setAttribute("user", user);

        // build a Spring Security principal so JwtTokenProvider can generate claims consistently
        CustomUserDetails cud = new CustomUserDetails(user);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        cud, null, cud.getAuthorities());

        String jwt = tokenProvider.generateToken(auth);
        Cookie jwtCookie = new Cookie("jwt", jwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60);
        response.addCookie(jwtCookie);

        // redirect: admin → /admin, else → /
        String role = user.getRole() != null ? user.getRole() : "user";
        boolean isAdmin = role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("ROLE_ADMIN");
        response.sendRedirect(isAdmin ? "/admin" : "/");
    }

    private User upsertGoogleUser(String email, String fullName) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User u = existing.get();
            // nếu trước đó bị khóa thì vẫn không cho đăng nhập
            if (u.getEnabled() != null && !u.getEnabled()) {
                throw new RuntimeException("Tài khoản đã bị khóa");
            }
            if (fullName != null && !fullName.isBlank()) {
                u.setFullName(fullName.trim());
            }
            if (u.getRole() == null || u.getRole().isBlank()) {
                u.setRole("user");
            }
            return userRepository.save(u);
        }

        User u = new User();
        u.setEmail(email);
        u.setFullName(fullName != null && !fullName.isBlank() ? fullName.trim() : email);
        // username: tránh trùng — lấy phần trước @ + suffix
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        if (base.isBlank()) base = "user";
        String username = base;
        int i = 0;
        while (userRepository.existsByUsername(username)) {
            i++;
            username = base + "_" + i;
        }
        u.setUsername(username);
        // password không dùng cho Google login, nhưng DB require not null → set random (BCrypt sẽ hash khi register; ở đây chỉ lưu placeholder).
        u.setPassword(UUID.randomUUID().toString());
        u.setRole("user");
        u.setEnabled(true);
        return userRepository.save(u);
    }
}

