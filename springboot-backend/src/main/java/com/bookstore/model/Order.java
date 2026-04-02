package com.bookstore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "orders")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_money", nullable = false)
    private Double totalMoney;

    @Column(name = "shipping_fee")
    private Double shippingFee = 0.0;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "final_total", nullable = false)
    private Double finalTotal;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_name")
    private String shippingName;

    @Column(name = "shipping_phone")
    private String shippingPhone;

    @Column(name = "order_note", columnDefinition = "TEXT")
    private String orderNote;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, CANCELLED

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // COD, VNPAY, MOMO

    @Column(name = "payment_status")
    private String paymentStatus = "UNPAID";

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "shipping_coupon_code")
    private String shippingCouponCode;

    @Column(name = "tracking_code")
    private String trackingCode;

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    /** Đã trừ tồn kho khi đơn được duyệt (CONFIRMED); dùng để hoàn tồn khi hủy / hoàn tác. */
    @Column(name = "stock_deducted", nullable = false)
    private Boolean stockDeducted = false;

    public Order() {}

    /** Nhãn tiếng Việt cho mã trạng thái (giao diện + email). */
    public static String labelVietnamese(String code) {
        if (code == null || code.isBlank()) return "Không xác định";
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "Chờ duyệt";
            case "CONFIRMED" -> "Đã duyệt";
            case "PROCESSING" -> "Đang chuẩn bị";
            case "SHIPPED", "SHIPPING", "DELIVERING" -> "Đang giao";
            case "COMPLETED" -> "Đã giao";
            case "CANCELLED" -> "Đã hủy";
            default -> code;
        };
    }

    public String getStatusVietnamese() {
        return labelVietnamese(this.status);
    }

    /** Đơn đã hủy — không hiển thị thanh 3 bước. */
    public boolean isCustomerOrderCancelled() {
        return status != null && "CANCELLED".equalsIgnoreCase(status.trim());
    }

    private static boolean isShippingPhaseCode(String code) {
        if (code == null || code.isBlank()) return false;
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "SHIPPED", "SHIPPING", "DELIVERING" -> true;
            default -> false;
        };
    }

    /**
     * Tiến trình 3 bước cho khách: 1 = Chờ xác nhận, 2 = Đang giao, 3 = Hoàn thành.
     * Đồng bộ với mã trạng thái admin cập nhật trên cùng bản ghi {@code orders}.
     */
    public int getCustomerProgressStep() {
        if (isCustomerOrderCancelled()) return 0;
        String s = status == null ? "PENDING" : status.trim().toUpperCase(Locale.ROOT);
        if ("COMPLETED".equals(s)) return 3;
        if (isShippingPhaseCode(s)) return 2;
        return 1;
    }

    /** Độ đầy thanh nối bước 1 → 3 (0–100). */
    public int getCustomerProgressPercent() {
        return switch (getCustomerProgressStep()) {
            case 1 -> 5;
            case 2 -> 50;
            case 3 -> 100;
            default -> 0;
        };
    }

    /** Nhãn bước hiện tại theo tiến trình 3 giai đoạn (AC khách hàng). */
    public String getCustomerProgressPhaseLabel() {
        if (isCustomerOrderCancelled()) return "Đã hủy";
        return switch (getCustomerProgressStep()) {
            case 1 -> "Chờ xác nhận";
            case 2 -> "Đang giao";
            case 3 -> "Hoàn thành";
            default -> "—";
        };
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Double getTotalMoney() { return totalMoney; }
    public void setTotalMoney(Double totalMoney) { this.totalMoney = totalMoney; }

    public Double getShippingFee() { return shippingFee; }
    public void setShippingFee(Double shippingFee) { this.shippingFee = shippingFee; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }

    public Double getFinalTotal() { return finalTotal; }
    public void setFinalTotal(Double finalTotal) { this.finalTotal = finalTotal; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getShippingName() { return shippingName; }
    public void setShippingName(String shippingName) { this.shippingName = shippingName; }

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public String getOrderNote() { return orderNote; }
    public void setOrderNote(String orderNote) { this.orderNote = orderNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public List<OrderDetail> getOrderDetails() { return orderDetails; }
    public void setOrderDetails(List<OrderDetail> orderDetails) { this.orderDetails = orderDetails; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getShippingCouponCode() { return shippingCouponCode; }
    public void setShippingCouponCode(String shippingCouponCode) { this.shippingCouponCode = shippingCouponCode; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public LocalDateTime getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDateTime expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }

    public Boolean getStockDeducted() { return stockDeducted; }
    public void setStockDeducted(Boolean stockDeducted) { this.stockDeducted = stockDeducted != null && stockDeducted; }
}
