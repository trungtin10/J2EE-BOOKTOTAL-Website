package com.bookstore.controller.admin;

import com.bookstore.model.User;
import com.bookstore.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String listUsers(@RequestParam(name = "page", defaultValue = "0") int page,
                            @RequestParam(name = "size", defaultValue = "10") int size,
                            @RequestParam(name = "keyword", required = false) String keyword,
                            Model model) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 5), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));

        Page<User> usersPage = userService.searchUsers(keyword, pageable);

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("page", safePage);
        model.addAttribute("size", safeSize);
        model.addAttribute("activePage", "users");
        return "admin/user/user_list";
    }

    @GetMapping("/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("activePage", "users");
        return "admin/user/add";
    }

    @PostMapping("/add")
    public String addUser(@Valid @ModelAttribute("user") User user, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "users");
            return "admin/user/add";
        }
        if (userService.existsByUsername(user.getUsername())) {
            bindingResult.rejectValue("username", "error.user", "Tên đăng nhập đã tồn tại");
            model.addAttribute("activePage", "users");
            return "admin/user/add";
        }
        if (userService.existsByEmail(user.getEmail())) {
            bindingResult.rejectValue("email", "error.user", "Email đã được sử dụng");
            model.addAttribute("activePage", "users");
            return "admin/user/add";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.saveUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm người dùng thành công");
        return "redirect:/admin/user";
    }

    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable(name = "id") Long id, Model model) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user id: " + id));
        // Xóa mật khẩu để tránh hiển thị hash
        user.setPassword("");
        model.addAttribute("user", user);
        return "admin/user/edit";
    }

    @PostMapping("/toggle/{id}")
    public String toggleUser(@PathVariable(name = "id") Long id,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             @RequestParam(name = "size", defaultValue = "10") int size,
                             @RequestParam(name = "keyword", required = false) String keyword,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id).orElseThrow();
            String role = user.getRole() == null ? "" : user.getRole().toLowerCase();
            if ("admin".equals(role)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể khóa tài khoản Admin.");
            } else {
                boolean next = !(user.getEnabled() == null || Boolean.TRUE.equals(user.getEnabled()));
                user.setEnabled(next);
                userService.saveUser(user);
                redirectAttributes.addFlashAttribute("successMessage", (next ? "Mở khóa" : "Khóa") + " tài khoản thành công");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái người dùng");
        }
        String k = keyword == null ? "" : keyword.trim();
        return "redirect:/admin/user?page=" + Math.max(page, 0) + "&size=" + Math.min(Math.max(size, 5), 50) + "&keyword=" + k;
    }

    @PostMapping("/edit/{id}")
    public String editUser(@PathVariable(name = "id") Long id, @ModelAttribute("user") User user, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        // Không dùng @Valid ở đây để tránh lỗi password trống
        
        User existingUser = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user id: " + id));

        // Kiểm tra email trùng lặp (nếu email thay đổi)
        if (!existingUser.getEmail().equals(user.getEmail()) && userService.existsByEmail(user.getEmail())) {
            bindingResult.rejectValue("email", "error.user", "Email đã được sử dụng bởi người dùng khác");
            model.addAttribute("activePage", "users");
            return "admin/user/edit";
        }

        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setRole(user.getRole());

        // Chỉ cập nhật mật khẩu nếu người dùng nhập mới
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (user.getPassword().length() < 6) {
                bindingResult.rejectValue("password", "error.user", "Mật khẩu phải có ít nhất 6 ký tự");
                model.addAttribute("activePage", "users");
                return "admin/user/edit";
            }
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        userService.saveUser(existingUser);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật người dùng thành công");
        return "redirect:/admin/user";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable(name = "id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa người dùng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa người dùng này (có thể do ràng buộc dữ liệu)");
        }
        return "redirect:/admin/user";
    }
}
