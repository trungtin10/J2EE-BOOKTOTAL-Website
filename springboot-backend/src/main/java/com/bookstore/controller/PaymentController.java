package com.bookstore.controller;

import com.bookstore.model.Order;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.NotificationService;
import com.bookstore.service.MomoPaymentService;
import com.bookstore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MomoPaymentService momoPaymentService;

    /**
     * Show mock payment gateway page (MOMO/VNPAY).
     */
    @GetMapping("/gateway")
    public String paymentGateway(
            @RequestParam("orderId") Long orderId,
            @RequestParam("amount") double amount,
            @RequestParam("method") String method,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) return "redirect:/login";

        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);
        model.addAttribute("method", method);

        // For MoMo: create a real payment and redirect to payUrl
        if ("MOMO".equalsIgnoreCase(method)) {
            try {
                long amt = Math.max(0, Math.round(amount));
                String baseUrl = resolveBaseUrl(request);
                MomoPaymentService.MomoCreateResponse momo = momoPaymentService.createPayment(orderId, amt, "pay with MoMo", baseUrl);
                if (StringUtils.hasText(momo.payUrl())) {
                    return "redirect:" + momo.payUrl();
                }
                model.addAttribute("momoError", momo.message() != null ? momo.message() : "Không tạo được giao dịch MoMo");
            } catch (Exception e) {
                model.addAttribute("momoError", e.getMessage());
            }
        }
        return "payment_gateway";
    }

    private static String resolveBaseUrl(HttpServletRequest req) {
        // Prefer forwarded headers (ngrok/reverse proxy), fallback to request values.
        String proto = firstNonBlank(req.getHeader("X-Forwarded-Proto"), req.getScheme());
        String host = firstNonBlank(req.getHeader("X-Forwarded-Host"), req.getHeader("Host"));
        if (!StringUtils.hasText(host)) {
            host = req.getServerName() + (req.getServerPort() > 0 ? ":" + req.getServerPort() : "");
        }
        // X-Forwarded-Host can include multiple values: "a,b" -> take first
        int comma = host.indexOf(',');
        if (comma > 0) host = host.substring(0, comma).trim();
        return proto + "://" + host;
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) return a.trim();
        if (StringUtils.hasText(b)) return b.trim();
        return "";
    }

    /**
     * MoMo return URL (user browser redirect).
     */
    @GetMapping("/momo/return")
    public String momoReturn(@RequestParam Map<String, String> params,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";

        String orderIdStr = params.get("orderId");
        String resultCode = params.get("resultCode");
        long orderId = -1;
        try { orderId = Long.parseLong(orderIdStr); } catch (Exception ignored) {}

        boolean ok = "0".equals(resultCode);
        // Optional: verify signature if params contain accessKey etc.
        // If signature verification fails, we mark as failed to be safe.
        if (params.containsKey("signature")) {
            boolean sigOk = momoPaymentService.verifyReturnOrIpnSignature(params);
            if (!sigOk) ok = false;
        }

        Long userId = userDetails.getUser().getId();
        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isPresent()) {
            Order o = orderOpt.get();
            if (ok) {
                o.setPaymentStatus("PAID");
                orderService.save(o);
                notificationService.createNotification(
                        userId,
                        "Thanh toán MoMo thành công!",
                        "Đơn hàng #" + orderId + " đã được thanh toán.",
                        "success"
                );
                return "redirect:/order/success/" + orderId;
            } else {
                o.setPaymentStatus("FAILED");
                // không auto-cancel đơn để khách có thể thử lại / admin xử lý
                orderService.save(o);
                notificationService.createNotification(
                        userId,
                        "Thanh toán MoMo thất bại!",
                        "Đơn hàng #" + orderId + " thanh toán không thành công. Bạn có thể thử lại.",
                        "danger"
                );
            }
        }
        return "redirect:/cart?paymentFailed=true";
    }

    /**
     * MoMo IPN (server-to-server callback).
     * We accept both JSON and form posts; Spring will map form to params as well.
     */
    @PostMapping("/momo/ipn")
    @ResponseBody
    public Map<String, Object> momoIpn(@RequestParam Map<String, String> params) {
        Map<String, Object> res = new HashMap<>();
        try {
            String orderIdStr = params.get("orderId");
            String resultCode = params.get("resultCode");
            long orderId = Long.parseLong(orderIdStr);

            boolean ok = "0".equals(resultCode);
            if (params.containsKey("signature")) {
                boolean sigOk = momoPaymentService.verifyReturnOrIpnSignature(params);
                if (!sigOk) {
                    res.put("resultCode", 1);
                    res.put("message", "invalid signature");
                    return res;
                }
            }

            orderService.getOrderById(orderId).ifPresent(o -> {
                if (ok) o.setPaymentStatus("PAID");
                else o.setPaymentStatus("FAILED");
                orderService.save(o);
            });

            res.put("resultCode", 0);
            res.put("message", "success");
            return res;
        } catch (Exception e) {
            res.put("resultCode", 1);
            res.put("message", "error: " + e.getMessage());
            return res;
        }
    }

    /**
     * Handle payment gateway confirmation (SUCCESS or FAILED).
     */
    @PostMapping("/confirm")
    public String confirmPayment(
            @RequestParam("orderId") Long orderId,
            @RequestParam("status") String status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) return "redirect:/login";

        Long userId = userDetails.getUser().getId();

        if ("SUCCESS".equals(status)) {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            orderOpt.ifPresent(o -> {
                o.setPaymentStatus("PAID");
                orderService.save(o);
            });

            notificationService.createNotification(
                    userId,
                    "Thanh toán thành công!",
                    "Đơn hàng #" + orderId + " đã được thanh toán. Chúng tôi sẽ sớm xử lý đơn hàng của bạn.",
                    "success"
            );
            return "redirect:/order/success/" + orderId;

        } else {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            orderOpt.ifPresent(o -> {
                o.setStatus("CANCELLED");
                o.setPaymentStatus("FAILED");
                orderService.save(o);
            });

            notificationService.createNotification(
                    userId,
                    "Thanh toán thất bại!",
                    "Đơn hàng #" + orderId + " đã bị hủy do thanh toán không thành công. Vui lòng đặt lại.",
                    "danger"
            );
            return "redirect:/cart?paymentFailed=true";
        }
    }
}
