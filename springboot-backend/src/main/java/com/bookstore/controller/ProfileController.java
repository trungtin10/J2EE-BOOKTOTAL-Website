package com.bookstore.controller;

import com.bookstore.model.Order;
import com.bookstore.model.User;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.OrderService;
import com.bookstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        User fresh = userService.getUserById(userDetails.getUser().getId()).orElse(userDetails.getUser());
        userDetails.setUser(fresh);
        model.addAttribute("userInfo", fresh);
        return "profile";
    }

    /** Alias thân thiện: cùng nội dung với {@code GET /orders} (danh sách đơn của khách). */
    @GetMapping("/orders")
    public String profileOrderListAlias() {
        return "redirect:/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("id") Long id, Model model) {
        if (userDetails == null) return "redirect:/login";
        Order order = orderService.getOrderById(id).orElse(null);
        if (order == null || !order.getUser().getId().equals(userDetails.getUser().getId())) {
            return "redirect:/orders";
        }
        model.addAttribute("order", order);
        return "order_detail";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam("fullName") String fullName,
                                @RequestParam("email") String email,
                                @RequestParam(name = "phone", required = false) String phone,
                                @RequestParam(name = "address", required = false) String address,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        User user = userService.getUserById(userDetails.getUser().getId()).orElseThrow();
        String newEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        if (newEmail.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email không được để trống.");
            return "redirect:/profile";
        }
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(newEmail)) {
            if (userService.existsByEmail(newEmail)) {
                redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng bởi tài khoản khác.");
                return "redirect:/profile";
            }
        }

        user.setFullName(fullName != null ? fullName.trim() : "");
        user.setEmail(newEmail);
        user.setPhone(phone != null ? phone.trim() : null);
        user.setAddress(address != null ? address.trim() : null);

        userService.saveUser(user);
        userDetails.setUser(user);

        redirectAttributes.addFlashAttribute("success", "Đã lưu thông tin cá nhân.");
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/profile#change-password";
        }

        String passwordError = validatePassword(newPassword);
        if (passwordError != null) {
            redirectAttributes.addFlashAttribute("error", passwordError);
            return "redirect:/profile#change-password";
        }

        User user = userService.getUserById(userDetails.getUser().getId()).orElseThrow();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng!");
            return "redirect:/profile#change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);
        userDetails.setUser(user);

        redirectAttributes.addFlashAttribute("success", "Cập nhật mật khẩu thành công!");
        return "redirect:/profile";
    }

    private String validatePassword(String password) {
        if (password.length() < 6) return "Mật khẩu phải có ít nhất 6 ký tự.";
        if (!password.matches(".*[A-Z].*")) return "Mật khẩu phải chứa ít nhất một chữ cái viết hoa.";
        if (!password.matches(".*[a-z].*")) return "Mật khẩu phải chứa ít nhất một chữ cái viết thường.";
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) return "Mật khẩu phải chứa ít nhất một ký tự đặc biệt.";
        return null;
    }
}
