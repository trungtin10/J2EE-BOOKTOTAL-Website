package com.bookstore.controller;

import com.bookstore.model.User;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid User user,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               @RequestParam(name = "returnUrl", required = false) String returnUrl) {
        if (bindingResult.hasErrors()) { return "auth/register"; }
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại.");
            return "auth/register";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("user");
        userService.saveUser(user);
        // Nếu đăng ký từ popup (có returnUrl), quay về đúng trang và mở popup đăng nhập.
        if (returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//") && !returnUrl.startsWith("/register")) {
            String sep = returnUrl.contains("?") ? "&" : "?";
            return "redirect:" + returnUrl + sep + "registerSuccess=1";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}
