package com.bookstore.controller;

import com.bookstore.model.Order;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.MomoPaymentService;
import com.bookstore.service.OrderService;
import com.bookstore.service.OnepayPaymentService;
import com.bookstore.service.VnpayPaymentService;
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
import org.springframework.http.MediaType;

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
    private MomoPaymentService momoPaymentService;

    @Autowired
    private OnepayPaymentService onepayPaymentService;

    @Autowired
    private VnpayPaymentService vnpayPaymentService;

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

        if (userDetails == null) {
            // Giữ nguyên link thanh toán để sau khi login quay lại đúng luồng.
            String qs = request.getQueryString();
            String returnUrl = request.getRequestURI() + (qs != null ? ("?" + qs) : "");
            String enc = java.net.URLEncoder.encode(returnUrl, java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/login?returnUrl=" + enc;
        }

        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);
        model.addAttribute("method", method);

        // For OnePAY (user yêu cầu thay MoMo bằng OnePAY):
        // - Nếu method=MOMO (link test cũ), mình vẫn map sang OnePAY.
        if ("ONEPAY".equalsIgnoreCase(method) || "MOMO".equalsIgnoreCase(method)) {
            try {
                String baseUrl = resolveBaseUrl(request);
                String orderInfo = "Thanh toan don hang #" + orderId;
                String url = onepayPaymentService.createPaymentRedirectUrl(orderId, amount, orderInfo, baseUrl);
                if (StringUtils.hasText(url)) {
                    return "redirect:" + url;
                }
                model.addAttribute("onepayError", "Không tạo được giao dịch OnePAY");
            } catch (Exception e) {
                model.addAttribute("onepayError", e.getMessage());
            }
        }

        // For VNPAY: create a real payment and redirect to VNPAY
        if ("VNPAY".equalsIgnoreCase(method)) {
            try {
                long amt = Math.max(0, Math.round(amount));
                String baseUrl = resolveBaseUrl(request);
                String url = vnpayPaymentService.createPaymentUrl(orderId, amt, baseUrl, request);
                return "redirect:" + url;
            } catch (Exception e) {
                model.addAttribute("vnpayError", e.getMessage());
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

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isPresent()) {
            Order o = orderOpt.get();
            if (ok) {
                String previousPaymentStatus = o.getPaymentStatus();
                o.setPaymentStatus("PAID");
                orderService.save(o);
                orderService.sendPaymentSuccessNotificationIfFirstTime(o, previousPaymentStatus);
                return "redirect:/order/success/" + orderId + "?btCartClear=1";
            } else {
                o.setPaymentStatus("FAILED");
                // không auto-cancel đơn để khách có thể thử lại / admin xử lý
                orderService.save(o);
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
                String previousPaymentStatus = o.getPaymentStatus();
                if (ok) o.setPaymentStatus("PAID");
                else o.setPaymentStatus("FAILED");
                orderService.save(o);
                if (ok) orderService.sendPaymentSuccessNotificationIfFirstTime(o, previousPaymentStatus);
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
     * OnePAY return URL (user browser redirect).
     *
     * Success condition (thường gặp): vpc_TxnResponseCode == "0"
     * Verify secure hash bằng vpc_SecureHash (best-effort).
     */
    @GetMapping("/onepay/return")
    public String onepayReturn(@RequestParam Map<String, String> params,
                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Chỉ phục vụ luồng browser return. (Nếu mất trình duyệt, trạng thái sẽ không tự cập nhật.)
        String orderIdStr = OnepayPaymentService.extractOrderIdFromReturn(params);
        Long orderId;
        try {
            orderId = orderIdStr != null ? Long.parseLong(orderIdStr) : null;
        } catch (Exception e) {
            orderId = null;
        }

        if (orderId == null) {
            return "redirect:/cart?paymentFailed=true";
        }

        boolean sigOk = onepayPaymentService.verifySecureHash(params);
        String responseCode = params.getOrDefault("vpc_TxnResponseCode", "");
        boolean ok = "0".equals(responseCode);
        // Best-effort: nếu có vpc_SecureHash thì mới ràng buộc chữ ký.
        if (StringUtils.hasText(params.get("vpc_SecureHash"))) {
            ok = ok && sigOk;
        }
        final boolean finalOk = ok;

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        orderOpt.ifPresent(o -> {
            String previousPaymentStatus = o.getPaymentStatus();

            o.setPaymentMethod("ONEPAY");
            o.setPaymentGatewayTransactionNo(params.get("vpc_TxnId"));
            String txnRef = params.get("vpc_TxnRef");
            if (!StringUtils.hasText(txnRef)) txnRef = params.get("vpc_MerchTxnRef");
            o.setPaymentTxnRef(txnRef);
            o.setPaymentBankCode(params.get("vpc_BankCode"));
            o.setPaymentPaidAt(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            o.setPaymentStatus(finalOk ? "PAID" : "FAILED");
            orderService.save(o);
            if (finalOk) orderService.sendPaymentSuccessNotificationIfFirstTime(o, previousPaymentStatus);
        });

        if (finalOk) return "redirect:/order/success/" + orderId + "?btCartClear=1";
        return "redirect:/cart?paymentFailed=true";
    }

    /**
     * VNPAY return URL (user browser redirect).
     * Example: /payment/vnpay/return?vnp_Amount=...&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_SecureHash=...
     */
    @GetMapping("/vnpay/return")
    public String vnpayReturn(@RequestParam Map<String, String> params,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, String> vnpParams = filterVnpParams(params);
        boolean sigOk = vnpayPaymentService.verifyReturn(vnpParams);
        Long orderId = VnpayPaymentService.extractOrderId(vnpParams);

        String responseCode = vnpParams.getOrDefault("vnp_ResponseCode", "");
        String txnStatus = vnpParams.getOrDefault("vnp_TransactionStatus", "");
        boolean ok = sigOk && "00".equals(responseCode) && "00".equals(txnStatus);

        if (orderId != null) {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            orderOpt.ifPresent(o -> {
                String previousPaymentStatus = o.getPaymentStatus();
                o.setPaymentMethod("VNPAY");
                o.setPaymentTxnRef(vnpParams.get("vnp_TxnRef"));
                o.setPaymentGatewayTransactionNo(vnpParams.get("vnp_TransactionNo"));
                o.setPaymentBankCode(vnpParams.get("vnp_BankCode"));
                o.setPaymentPaidAt(vnpParams.get("vnp_PayDate"));
                if (ok) {
                    o.setPaymentStatus("PAID");
                } else {
                    o.setPaymentStatus("FAILED");
                }
                orderService.save(o);
                if (ok) orderService.sendPaymentSuccessNotificationIfFirstTime(o, previousPaymentStatus);
            });

            if (userDetails != null && ok) {
                return "redirect:/order/success/" + orderId + "?btCartClear=1";
            }
        }

        // If user not logged in or order not found -> send them back to cart
        return "redirect:/cart?paymentFailed=true";
    }

    /**
     * VNPAY IPN (server-to-server callback). Must return JSON with RspCode/Message.
     */
    @GetMapping(value = "/vnpay/ipn", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> vnpParams = filterVnpParams(params);
        String rspCode = "99";
        String message = "Unknown error";
        try {
            boolean sigOk = vnpayPaymentService.verifyReturn(vnpParams);
            if (!sigOk) {
                rspCode = "97";
                message = "Invalid signature";
                return Map.of("RspCode", rspCode, "Message", message);
            }

            Long orderId = VnpayPaymentService.extractOrderId(vnpParams);
            if (orderId == null) {
                rspCode = "01";
                message = "Order not found";
                return Map.of("RspCode", rspCode, "Message", message);
            }

            String responseCode = vnpParams.getOrDefault("vnp_ResponseCode", "");
            String txnStatus = vnpParams.getOrDefault("vnp_TransactionStatus", "");
            boolean ok = "00".equals(responseCode) && "00".equals(txnStatus);

            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isEmpty()) {
                rspCode = "01";
                message = "Order not found";
                return Map.of("RspCode", rspCode, "Message", message);
            }

            Order o = orderOpt.get();
            long vnpAmountMinor = VnpayPaymentService.parseAmountMinorUnits(vnpParams);
            if (vnpAmountMinor < 0) {
                rspCode = "04";
                message = "Invalid amount";
                return Map.of("RspCode", rspCode, "Message", message);
            }
            double finalTotalVnd = o.getFinalTotal() != null ? o.getFinalTotal() : 0d;
            long expectedMinor = Math.round(finalTotalVnd * 100.0);
            if (vnpAmountMinor != expectedMinor) {
                rspCode = "04";
                message = "Invalid amount";
                return Map.of("RspCode", rspCode, "Message", message);
            }

            // Only update once if already PAID
            if ("PAID".equalsIgnoreCase(o.getPaymentStatus())) {
                rspCode = "02";
                message = "Order already confirmed";
                return Map.of("RspCode", rspCode, "Message", message);
            }

            String previousPaymentStatus = o.getPaymentStatus();
            o.setPaymentMethod("VNPAY");
            o.setPaymentTxnRef(vnpParams.get("vnp_TxnRef"));
            o.setPaymentGatewayTransactionNo(vnpParams.get("vnp_TransactionNo"));
            o.setPaymentBankCode(vnpParams.get("vnp_BankCode"));
            o.setPaymentPaidAt(vnpParams.get("vnp_PayDate"));
            o.setPaymentStatus(ok ? "PAID" : "FAILED");
            orderService.save(o);
            if (ok) orderService.sendPaymentSuccessNotificationIfFirstTime(o, previousPaymentStatus);

            rspCode = "00";
            message = "Confirm Success";
            return Map.of("RspCode", rspCode, "Message", message);
        } catch (Exception e) {
            return Map.of("RspCode", rspCode, "Message", message);
        }
    }

    private static Map<String, String> filterVnpParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) return Map.of();
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey() != null && e.getKey().startsWith("vnp_")) {
                out.put(e.getKey(), e.getValue());
            }
        }
        // Keep secure hash too (it starts with vnp_)
        if (params.containsKey("vnp_SecureHash")) out.put("vnp_SecureHash", params.get("vnp_SecureHash"));
        if (params.containsKey("vnp_SecureHashType")) out.put("vnp_SecureHashType", params.get("vnp_SecureHashType"));
        return out;
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

        if ("SUCCESS".equals(status)) {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            orderOpt.ifPresent(o -> {
                String previousPaymentStatus = o.getPaymentStatus();
                o.setPaymentStatus("PAID");
                orderService.save(o);
                orderService.sendPaymentSuccessNotificationIfFirstTime(o, previousPaymentStatus);
            });
            return "redirect:/order/success/" + orderId + "?btCartClear=1";

        } else {
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            orderOpt.ifPresent(o -> {
                o.setStatus("CANCELLED");
                o.setPaymentStatus("FAILED");
                orderService.save(o);
            });
            return "redirect:/cart?paymentFailed=true";
        }
    }
}
