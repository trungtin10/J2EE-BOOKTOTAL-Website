package com.bookstore.controller;

import com.bookstore.model.User;
import com.bookstore.service.EmailService;
import com.bookstore.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request, Model model) {
        // Chuyển hướng về trang trước đó với tham số lỗi
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/") + "?loginError=true";
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
}
