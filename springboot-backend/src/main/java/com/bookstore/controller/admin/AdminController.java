package com.bookstore.controller.admin;

import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.ProductRepository;
import com.bookstore.repository.ReviewRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping({ "", "/" })
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", userRepository.count());
        stats.put("products", productRepository.count());
        stats.put("orders", orderRepository.count());
        stats.put("categories", categoryRepository.count());
        stats.put("reviews", reviewRepository.count());
        Double revenue = orderRepository.getTotalRevenue();
        stats.put("revenue", revenue != null ? revenue : 0.0);
        
        model.addAttribute("stats", stats);
        
        model.addAttribute("pendingOrders", orderRepository.findAllByOrderByOrderDateDesc().stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .limit(5)
                .toList());

        return "admin/index";
    }

    @GetMapping("/stats")
    public String stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "day") String group,
            Model model) {
        model.addAttribute("title", "Thống kê chi tiết");
        model.addAttribute("activePage", "stats");

        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = end != null ? end : today;
        LocalDate rangeStart = start != null ? start : today.withDayOfMonth(1);
        if (rangeStart.isAfter(rangeEnd)) {
            LocalDate tmp = rangeStart;
            rangeStart = rangeEnd;
            rangeEnd = tmp;
        }
        long maxDays = 366L * 2;
        if (rangeStart.until(rangeEnd).getDays() > maxDays) {
            rangeEnd = rangeStart.plusDays(maxDays);
        }

        boolean byMonth = "month".equalsIgnoreCase(group);
        model.addAttribute("groupBy", byMonth ? "month" : "day");
        model.addAttribute("rangeStart", rangeStart);
        model.addAttribute("rangeEnd", rangeEnd);

        Double revenueInRange = orderRepository.getTotalRevenueBetween(rangeStart, rangeEnd);
        model.addAttribute("totalRevenue", revenueInRange != null ? revenueInRange : 0.0);
        Long ordersInRange = orderRepository.countOrdersBetween(rangeStart, rangeEnd);
        model.addAttribute("totalOrders", ordersInRange != null ? ordersInRange : 0L);

        Map<String, Long> orderStats = new HashMap<>();
        orderStats.put("PENDING", orderRepository.countByStatus("PENDING"));
        orderStats.put("CONFIRMED", orderRepository.countByStatus("CONFIRMED"));
        orderStats.put("PROCESSING", orderRepository.countByStatus("PROCESSING"));
        orderStats.put("DELIVERING", orderRepository.countByStatus("DELIVERING"));
        orderStats.put("COMPLETED", orderRepository.countByStatus("COMPLETED"));
        orderStats.put("CANCELLED", orderRepository.countByStatus("CANCELLED"));
        model.addAttribute("orderStats", orderStats);

        model.addAttribute("recentOrders", orderRepository.findAllByOrderByOrderDateDesc().stream()
                .limit(10)
                .toList());

        List<Object[]> series = byMonth
                ? orderRepository.getRevenueByMonthInRange(rangeStart, rangeEnd)
                : orderRepository.getRevenueByDayInRange(rangeStart, rangeEnd);

        DateTimeFormatter dayLabelFmt = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter monthLabelFmt = DateTimeFormatter.ofPattern("MM/yyyy");

        List<String> chartLabels = series.stream().map(row -> {
            if (byMonth) {
                String ym = row[0] != null ? row[0].toString() : "";
                try {
                    return YearMonth.parse(ym).format(monthLabelFmt);
                } catch (Exception e) {
                    return ym;
                }
            }
            if (row[0] instanceof java.sql.Date) {
                return ((java.sql.Date) row[0]).toLocalDate().format(dayLabelFmt);
            }
            if (row[0] instanceof LocalDate) {
                return ((LocalDate) row[0]).format(dayLabelFmt);
            }
            return row[0] != null ? row[0].toString() : "";
        }).collect(Collectors.toList());

        List<Double> chartData = series.stream().map(row -> {
            if (row[1] instanceof java.math.BigDecimal) {
                return ((java.math.BigDecimal) row[1]).doubleValue();
            }
            if (row[1] instanceof Number) {
                return ((Number) row[1]).doubleValue();
            }
            return 0.0;
        }).collect(Collectors.toList());

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        model.addAttribute("chartEmpty", chartData.isEmpty());
        model.addAttribute("chartJsType", byMonth ? "bar" : "line");

        return "admin/stats";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        com.bookstore.model.User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("userInfo", user);
        return "admin/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam("fullName") String fullName,
                                @RequestParam("email") String email,
                                RedirectAttributes redirectAttributes) {
        com.bookstore.model.User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        user.setFullName(fullName);
        user.setEmail(email);
        userService.saveUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công");
        return "redirect:/admin/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "admin/change_password";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam("current_password") String currentPassword,
                                 @RequestParam("new_password") String newPassword,
                                 @RequestParam("confirm_password") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        com.bookstore.model.User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addAttribute("error", "Mật khẩu hiện tại không đúng");
            return "redirect:/admin/change-password";
        }
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/admin/change-password";
        }
        
        String passwordError = validatePassword(newPassword);
        if (passwordError != null) {
            redirectAttributes.addAttribute("error", passwordError);
            return "redirect:/admin/change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);
        
        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công");
        return "redirect:/admin/change-password";
    }

    private String validatePassword(String password) {
        if (password.length() < 6) return "Mật khẩu phải có ít nhất 6 ký tự.";
        if (!password.matches(".*[A-Z].*")) return "Mật khẩu phải chứa ít nhất một chữ cái viết hoa.";
        if (!password.matches(".*[a-z].*")) return "Mật khẩu phải chứa ít nhất một chữ cái viết thường.";
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) return "Mật khẩu phải chứa ít nhất một ký tự đặc biệt.";
        return null;
    }
}
