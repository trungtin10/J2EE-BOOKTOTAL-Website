package com.bookstore.controller.api;

import com.bookstore.model.Coupon;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class CouponRestController {

    @Autowired
    private CouponService couponService;

    @GetMapping("/coupons")
    public ResponseEntity<?> getActiveCoupons(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Coupon> coupons = couponService.getAllActiveCoupons();
        Long userId = userDetails != null && userDetails.getUser() != null ? userDetails.getUser().getId() : null;

        if (userId != null) {
            coupons = coupons.stream()
                    .filter(c -> couponService.getCouponByCodeForUser(c.getCode(), userId).isPresent())
                    .toList();
        }
        
        List<Coupon> productCoupons = coupons.stream()
                .filter(c -> c.getType() != null && "PRODUCT".equalsIgnoreCase(c.getType()))
                .toList();
        List<Coupon> shippingCoupons = coupons.stream()
                .filter(c -> c.getType() != null && "SHIPPING".equalsIgnoreCase(c.getType()))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("product", productCoupons);
        data.put("shipping", shippingCoupons);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/coupon/check")
    public ResponseEntity<?> checkCoupon(@RequestBody Map<String, Object> payload,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        String code = (String) payload.get("code");
        Double totalAmount = Double.valueOf(payload.get("totalAmount").toString());
        Double shippingFee = payload.get("shippingFee") != null ? Double.valueOf(payload.get("shippingFee").toString()) : 30000.0;
        int cartItemQuantity = 0;
        if (payload.get("cartItemQuantity") != null) {
            try {
                cartItemQuantity = Integer.parseInt(payload.get("cartItemQuantity").toString().trim());
            } catch (NumberFormatException ignored) {
                cartItemQuantity = 0;
            }
        }

        Long userId = userDetails != null && userDetails.getUser() != null ? userDetails.getUser().getId() : null;
        Optional<Coupon> couponOpt = (userId != null)
                ? couponService.getCouponByCodeForUser(code, userId)
                : couponService.getCouponByCode(code);
        
        Map<String, Object> response = new HashMap<>();
        if (couponOpt.isPresent()) {
            Coupon coupon = couponOpt.get();
            
            if (coupon.getMinOrderValue() != null && totalAmount < coupon.getMinOrderValue()) {
                response.put("success", false);
                response.put("message", "Đơn hàng tối thiểu phải từ " + String.format("%.0f", coupon.getMinOrderValue()) + "đ");
                return ResponseEntity.ok(response);
            }

            Integer minQty = coupon.getMinItemQuantity();
            if (minQty != null && minQty > 0 && cartItemQuantity < minQty) {
                response.put("success", false);
                response.put("message", "Giỏ hàng cần tối thiểu " + minQty + " sản phẩm để dùng mã này");
                return ResponseEntity.ok(response);
            }

            double discountValue = 0;
            String message = "";

            if ("PRODUCT".equalsIgnoreCase(coupon.getType())) {
                if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
                    discountValue = totalAmount * (coupon.getDiscountValue() / 100.0);
                    if (coupon.getMaxDiscountAmount() != null && discountValue > coupon.getMaxDiscountAmount()) {
                        discountValue = coupon.getMaxDiscountAmount();
                    }
                } else {
                    discountValue = coupon.getDiscountValue();
                }
                if (discountValue > totalAmount) discountValue = totalAmount;
                message = "Áp dụng mã giảm giá sản phẩm thành công!";
            } else if ("SHIPPING".equalsIgnoreCase(coupon.getType())) {
                discountValue = coupon.getDiscountValue();
                if (discountValue > shippingFee) discountValue = shippingFee;
                message = "Áp dụng mã Freeship thành công!";
            }

            response.put("success", true);
            response.put("message", message);
            response.put("discount", discountValue);
            response.put("type", coupon.getType());
            response.put("code", coupon.getCode());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Mã giảm giá không hợp lệ, đã hết hạn hoặc bạn đã dùng hết lượt cho mã này");
            return ResponseEntity.ok(response);
        }
    }
}
