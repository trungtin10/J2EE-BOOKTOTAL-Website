package com.bookstore.controller;

import com.bookstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class PasswordResetController {

    @Autowired
    private UserService userService;

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot_password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập email.");
            model.addAttribute("email", "");
            return "auth/forgot_password";
        }
        boolean sent = userService.requestPasswordReset(email);
        if (!sent) {
            model.addAttribute("error", "Email không tồn tại trong hệ thống. Vui lòng kiểm tra lại.");
            model.addAttribute("email", email);
            return "auth/forgot_password";
        }
        model.addAttribute("success",
                "Đã gửi email chứa mã xác nhận và liên kết đặt lại mật khẩu. Link có hiệu lực trong "
                        + UserService.PASSWORD_RESET_TOKEN_VALID_MINUTES + " phút.");
        return "auth/forgot_password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam(name = "token", required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("token", null);
            model.addAttribute("error", "Thiếu mã xác nhận.");
            return "auth/reset_password";
        }
        String t = token.trim();
        if (userService.getUserByResetToken(t).isEmpty()) {
            model.addAttribute("token", null);
            model.addAttribute("error", "Liên kết hoặc mã xác nhận không hợp lệ, hoặc đã hết hạn (" + UserService.PASSWORD_RESET_TOKEN_VALID_MINUTES + " phút).");
            return "auth/reset_password";
        }
        model.addAttribute("token", t);
        return "auth/reset_password";
    }

    @PostMapping("/reset-password/{token}")
    public String resetPasswordSubmit(
            @PathVariable("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (password == null || !password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        }
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            return "redirect:/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        }
        try {
            userService.resetPasswordWithToken(token, password);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Liên kết hoặc mã xác nhận không hợp lệ, hoặc đã hết hạn.");
            return "redirect:/forgot-password";
        }
        return "redirect:/login?resetSuccess=true";
    }
}
