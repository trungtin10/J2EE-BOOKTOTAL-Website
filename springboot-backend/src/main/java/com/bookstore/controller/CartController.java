package com.bookstore.controller;

import com.bookstore.model.CartItem;
import com.bookstore.model.Product;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.NotificationService;
import com.bookstore.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private ProductService productService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        List<CartItem> cart = getCartFromSession(session);
        model.addAttribute("cart", cart);
        model.addAttribute("total", calculateTotal(cart));
        return "cart";
    }

    @GetMapping("/summary")
    @ResponseBody
    public ResponseEntity<?> getCartSummary(HttpSession session) {
        List<CartItem> cart = getCartFromSession(session);
        Map<String, Object> summary = new HashMap<>();
        summary.put("items", cart);
        summary.put("totalQty", cart.stream().mapToInt(CartItem::getQuantity).sum());
        summary.put("totalPrice", calculateTotal(cart));
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(@RequestParam("productId") Long productId, 
                                     @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                                     HttpSession session,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<Product> productOpt = productService.getActiveProductById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            int available = product.getQuantity() != null ? product.getQuantity() : 0;
            int reqQty = quantity;
            if (reqQty <= 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số lượng không hợp lệ"));
            }
            // giới hạn an toàn để tránh spam số cực lớn
            if (reqQty > 999) reqQty = 999;
            if (available <= 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Sản phẩm đã hết hàng"));
            }
            List<CartItem> cart = getCartFromSession(session);

            boolean found = false;
            for (CartItem item : cart) {
                if (item.getId().equals(productId)) {
                    int current = item.getQuantity() != null ? item.getQuantity() : 0;
                    int newQty = current + reqQty;
                    if (newQty > available) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "success", false,
                                "message", "Sản phẩm không đủ hàng",
                                "available", available,
                                "currentInCart", current
                        ));
                    }
                    item.setQuantity(newQty);
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (reqQty > available) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Sản phẩm không đủ hàng",
                            "available", available
                    ));
                }
                cart.add(new CartItem(product.getId(), product.getName(), product.getPrice(), reqQty, product.getImageUrl()));
            }
            session.setAttribute("cart", cart);

            // Tạo thông báo hệ thống
            if (userDetails != null) {
                notificationService.createNotification(
                    userDetails.getUser().getId(),
                    "Giỏ hàng",
                    "Sản phẩm '" + product.getName() + "' đã được thêm vào giỏ hàng.",
                    "success"
                );
            }

            int totalQty = cart.stream().mapToInt(CartItem::getQuantity).sum();
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Đã thêm vào giỏ hàng", "totalQty", totalQty));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Sản phẩm không tồn tại"));
    }

    /** Đồng bộ giỏ hàng từ LocalStorage → Session để không mất khi F5/đổi tab. */
    @PostMapping("/sync")
    @ResponseBody
    public ResponseEntity<?> syncCartFromClient(@RequestBody Map<String, Object> payload, HttpSession session) {
        Object itemsObj = payload != null ? payload.get("items") : null;
        if (!(itemsObj instanceof List<?> itemsRaw)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid payload"));
        }

        // Build map productId -> qty (merge duplicates)
        Map<Long, Integer> merged = new HashMap<>();
        for (Object o : itemsRaw) {
            if (!(o instanceof Map<?, ?> m)) continue;
            Object pidObj = m.get("productId");
            Object qtyObj = m.get("quantity");
            if (!(pidObj instanceof Number) || !(qtyObj instanceof Number)) continue;
            long pid = ((Number) pidObj).longValue();
            int qty = ((Number) qtyObj).intValue();
            if (pid <= 0 || qty <= 0) continue;
            merged.merge(pid, qty, Integer::sum);
        }

        List<CartItem> newCart = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : merged.entrySet()) {
            Long productId = e.getKey();
            int qty = Math.min(Math.max(e.getValue(), 1), 99);
            Optional<Product> productOpt = productService.getActiveProductById(productId);
            if (productOpt.isEmpty()) continue;
            Product p = productOpt.get();
            int available = p.getQuantity() != null ? p.getQuantity() : 0;
            if (available <= 0) continue;
            int finalQty = Math.min(qty, available);
            if (finalQty <= 0) continue;
            newCart.add(new CartItem(p.getId(), p.getName(), p.getPrice(), finalQty, p.getImageUrl()));
        }

        // Keep stable order (by id desc)
        newCart = newCart.stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .collect(Collectors.toList());

        session.setAttribute("cart", newCart);
        int totalQty = newCart.stream().mapToInt(CartItem::getQuantity).sum();
        return ResponseEntity.ok(Map.of("success", true, "totalQty", totalQty));
    }

    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<?> removeFromCart(@RequestParam("productId") Long productId, HttpSession session) {
        List<CartItem> cart = getCartFromSession(session);
        cart.removeIf(item -> item.getId().equals(productId));
        session.setAttribute("cart", cart);
        
        int totalQty = cart.stream().mapToInt(CartItem::getQuantity).sum();
        return ResponseEntity.ok().body(Map.of("success", true, "totalQty", totalQty));
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<?> updateCart(@RequestParam("productId") Long productId, 
                                     @RequestParam("quantity") int quantity, 
                                     HttpSession session) {
        List<CartItem> cart = getCartFromSession(session);
        for (CartItem item : cart) {
            if (item.getId().equals(productId)) {
                item.setQuantity(quantity);
                break;
            }
        }
        session.setAttribute("cart", cart);
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    @SuppressWarnings("unchecked")
    private List<CartItem> getCartFromSession(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    private double calculateTotal(List<CartItem> cart) {
        return cart.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
    }
}
