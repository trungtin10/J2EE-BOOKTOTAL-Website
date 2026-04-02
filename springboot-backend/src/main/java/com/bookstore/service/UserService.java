package com.bookstore.service;

import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    /** Thời hạn link / mã đặt lại mật khẩu (phút). */
    public static final int PASSWORD_RESET_TOKEN_VALID_MINUTES = 15;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> searchUsersPage(String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return userRepository.searchUsersByFullNameOrEmail(kw, pageable);
    }

    @Transactional
    public void setUserEnabled(Long userId, boolean enabled) {
        User u = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        u.setEnabled(enabled);
        userRepository.save(u);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> getUserByResetToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByResetTokenAndResetTokenExpiryAfter(token.trim(), LocalDateTime.now());
    }

    public void updateResetToken(String email, String token, LocalDateTime expiry) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Could not find any user with the email " + email));
        user.setResetToken(token);
        user.setResetTokenExpiry(expiry);
        userRepository.save(user);
    }

    /**
     * @return {@code false} nếu email không tồn tại (để báo lỗi theo AC).
     */
    @Transactional
    public boolean requestPasswordReset(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return false;
        }
        String email = rawEmail.trim();
        Optional<User> opt = userRepository.findByEmailIgnoreCase(email);
        if (opt.isEmpty()) {
            return false;
        }
        User user = opt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_VALID_MINUTES));
        userRepository.save(user);
        emailService.sendResetPasswordEmail(user.getEmail(), token);
        return true;
    }

    @Transactional
    public void resetPasswordWithToken(String token, String plainNewPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("invalid_token");
        }
        User user = userRepository.findByResetTokenAndResetTokenExpiryAfter(token.trim(), LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("invalid_or_expired_token"));
        if (plainNewPassword == null || plainNewPassword.length() < 6) {
            throw new IllegalArgumentException("weak_password");
        }
        user.setPassword(passwordEncoder.encode(plainNewPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public void updatePassword(User user, String newPassword) {
        user.setPassword(newPassword);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
}
