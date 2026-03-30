package com.bookstore.controller;

import com.bookstore.model.User;
import com.bookstore.service.EmailService;
import com.bookstore.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String showLoginForm() {
        // Hiển thị trang đăng nhập (Spring Security failureUrl dùng ?error=true)
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid User user, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) { return "auth/register"; }
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại.");
            return "auth/register";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("user");
        userService.saveUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot_password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        if (!userService.existsByEmail(email)) {
            model.addAttribute("error", "Email không tồn tại trong hệ thống.");
            return "auth/forgot_password";
        }

        String token = generate6DigitCode();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
        userService.updateResetToken(email, token, expiry);
        emailService.sendResetPasswordEmail(email, token);

        model.addAttribute("success", "Đã gửi email khôi phục. Vui lòng kiểm tra hộp thư của bạn.");
        return "auth/forgot_password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam(name = "token", required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("error", "Liên kết không hợp lệ hoặc đã hết hạn.");
            model.addAttribute("token", null);
            return "auth/reset_password";
        }

        return userService.getUserByResetToken(token)
                .map(u -> {
                    model.addAttribute("token", token);
                    return "auth/reset_password";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Liên kết không hợp lệ hoặc đã hết hạn.");
                    model.addAttribute("token", null);
                    return "auth/reset_password";
                });
    }

    @PostMapping("/reset-password/{token}")
    public String processResetPassword(@PathVariable("token") String token,
                                       @RequestParam("password") String password,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("error", "Liên kết không hợp lệ hoặc đã hết hạn.");
            model.addAttribute("token", null);
            return "auth/reset_password";
        }

        User user = userService.getUserByResetToken(token).orElse(null);
        if (user == null) {
            model.addAttribute("error", "Liên kết không hợp lệ hoặc đã hết hạn.");
            model.addAttribute("token", null);
            return "auth/reset_password";
        }

        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            model.addAttribute("token", token);
            return "auth/reset_password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            model.addAttribute("token", token);
            return "auth/reset_password";
        }

        userService.updatePassword(user, passwordEncoder.encode(password));
        return "redirect:/login?resetSuccess=true";
    }

    private String generate6DigitCode() {
        SecureRandom rnd = new SecureRandom();
        int code = rnd.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
